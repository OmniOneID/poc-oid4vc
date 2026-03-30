import Foundation
import CoreBluetooth

public class BleTransportManager: NSObject, TransportManager {
    private let TAG = "MDR/BleTransport"
    private var centralManager: CBCentralManager!
    private var discoveredPeripheral: CBPeripheral?
    private var serviceUUID: CBUUID
    private var stateCharacteristic: CBCharacteristic?
    private var c2sCharacteristic: CBCharacteristic?
    private var s2cCharacteristic: CBCharacteristic?
    
    private var listeners = [TransferEvent.Listener]()
    private var sessionEncryption: SessionEncryption?
    private var engagementSource: EngagementSource?
    
    private var incomingData = Data()

    
    public init(config: TransportConfig) {
        self.serviceUUID = CBUUID(string: BleConstants.serviceUUID)
        super.init()
        self.centralManager = CBCentralManager(delegate: self, queue: nil)
        print("\(TAG): Initialized")
    }
    
    public func addListener(_ listener: TransferEvent.Listener) {
        listeners.append(listener)
    }
    
    public func removeListener(_ listener: TransferEvent.Listener) {
        listeners.removeAll { $0 === listener }
    }
    
    public func startDeviceEngagement(source: EngagementSource) {
        print("\(TAG): Starting device engagement")
        self.engagementSource = source
        self.notifyListeners(.deviceEngagementCompleted)
        
        if let dynamicUUID = source.bleServiceUUID {
            self.serviceUUID = dynamicUUID
            print("\(TAG): Using dynamic Service UUID from QR: \(dynamicUUID.uuidString)")
        } else {
            print("\(TAG): No BLE UUID in QR, using default: \(serviceUUID.uuidString)")
        }
        
        if centralManager.state == .poweredOn {
            print("\(TAG): Scanning for services: \(serviceUUID)")
            centralManager.scanForPeripherals(withServices: [serviceUUID], options: nil)
        } else {
            print("\(TAG): Bluetooth is not powered on (current state: \(centralManager.state.rawValue))")
        }
    }
    
    public func sendRequest(deviceRequestBytes: Data) {
        guard let peripheral = discoveredPeripheral, let char = c2sCharacteristic else {
            print("\(TAG): Cannot send request - peripheral or characteristic missing")
            return
        }
        
        if let stateChar = stateCharacteristic {
            print("\(TAG): Writing START(0x01) to State characteristic (withoutResponse)")
            peripheral.writeValue(Data([BleConstants.stateStart]), for: stateChar, type: .withoutResponse)
            
            // Give EUDI Wallet a brief moment to process the State command and update its internal state to CONNECTED
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                self.sendDataChunks(deviceRequestBytes)
            }
        } else {
            sendDataChunks(deviceRequestBytes)
        }
    }
    
    private func sendDataChunks(_ deviceRequestBytes: Data) {
        guard let peripheral = discoveredPeripheral, let char = c2sCharacteristic else { return }
        print("\(TAG): Sending request, total bytes: \(deviceRequestBytes.count)")
        let maxChunkSize = 512 - 3 - 1
        var offset = 0
        while offset < deviceRequestBytes.count {
            let end = min(offset + maxChunkSize, deviceRequestBytes.count)
            let chunkData = deviceRequestBytes.subdata(in: offset..<end)
            
            var packet = Data()
            packet.append((end < deviceRequestBytes.count) ? 0x01 : 0x00)
            packet.append(chunkData)
            
            peripheral.writeValue(packet, for: char, type: .withoutResponse)
            offset += maxChunkSize
            print("\(TAG): Sent chunk, remaining bytes: \(deviceRequestBytes.count - offset)")
        }
        notifyListeners(.requestSent)
    }
    
    public func getSessionEncryption() -> SessionEncryption? {
        return sessionEncryption
    }
    
    public func stopSession() {
        print("\(TAG): Stopping session")
        if let peripheral = discoveredPeripheral {
            centralManager.cancelPeripheralConnection(peripheral)
        }
        centralManager.stopScan()
    }
    
    private func notifyListeners(_ event: TransferEvent) {
        listeners.forEach { $0.onEvent(event) }
    }
}

extension BleTransportManager: CBCentralManagerDelegate {
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        print("\(TAG): Central manager state updated: \(central.state.rawValue)")
        if central.state == .poweredOn, let _ = engagementSource {
            print("\(TAG): Bluetooth powered on, starting scan")
            central.scanForPeripherals(withServices: [serviceUUID], options: nil)
        }
    }
    
    public func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        print("\(TAG): Discovered peripheral: \(peripheral.name ?? "Unknown") (\(peripheral.identifier))")
        self.discoveredPeripheral = peripheral
        central.stopScan()
        central.connect(peripheral, options: nil)
        notifyListeners(.connecting)
    }
    
    public func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        print("\(TAG): Connected to peripheral: \(peripheral.name ?? "Unknown")")
        peripheral.delegate = self
        // Discover all services to avoid missing the dynamic one due to cache
        peripheral.discoverServices(nil)
    }
    
    public func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        print("\(TAG): Failed to connect: \(error?.localizedDescription ?? "Unknown error")")
        notifyListeners(.error(message: "Connection failed"))
    }
}

extension BleTransportManager: CBPeripheralDelegate {
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        if let error = error {
            print("\(TAG): Error discovering services: \(error.localizedDescription)")
            return
        }
        guard let services = peripheral.services else {
            print("\(TAG): No services found (services array is nil)")
            return
        }
        
        print("\(TAG): Discovered \(services.count) services")
        var foundMatch = false
        for service in services {
            print("\(TAG): Found Service: \(service.uuid.uuidString)")
            // Compare case-insensitively to avoid mismatches
            if service.uuid.uuidString.lowercased() == serviceUUID.uuidString.lowercased() {
                print("\(TAG): Matching mDoc service found, discovering characteristics")
                foundMatch = true
                peripheral.discoverCharacteristics([
                    CBUUID(string: BleConstants.characteristicStateUUID),
                    CBUUID(string: BleConstants.characteristicClient2ServerUUID),
                    CBUUID(string: BleConstants.characteristicServer2ClientUUID)
                ], for: service)
            }
        }
        
        if !foundMatch {
            print("\(TAG): Target Service UUID \(serviceUUID.uuidString) not found in discovered list")
        }
    }
    
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        if let error = error {
            print("\(TAG): Error discovering characteristics: \(error.localizedDescription)")
            return
        }
        guard let characteristics = service.characteristics else { return }
        print("\(TAG): Discovered \(characteristics.count) characteristics for service \(service.uuid)")
        for char in characteristics {
            let uuid = char.uuid.uuidString.lowercased()
            if uuid == BleConstants.characteristicStateUUID.lowercased() {
                print("\(TAG): State characteristic found")
                stateCharacteristic = char
            } else if uuid == BleConstants.characteristicClient2ServerUUID.lowercased() {
                print("\(TAG): Client2Server characteristic found")
                c2sCharacteristic = char
            } else if uuid == BleConstants.characteristicServer2ClientUUID.lowercased() {
                print("\(TAG): Server2Client characteristic found")
                s2cCharacteristic = char
                peripheral.setNotifyValue(true, for: char)
                print("\(TAG): Notifications enabled for Server2Client")
            }
        }
        
        // Wait for didUpdateNotificationStateFor to notify .connected
    }
    
    public func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        if let error = error {
            print("\(TAG): Error updating characteristic value: \(error.localizedDescription)")
            return
        }
        guard let data = characteristic.value else { return }
        
        if characteristic == s2cCharacteristic {
            print("\(TAG): Received data from Server2Client, chunk size: \(data.count)")
            let hasMore = data[0] == 0x01
            incomingData.append(data.subdata(in: 1..<data.count))
            
            if !hasMore {
                print("\(TAG): Full message received, total bytes: \(incomingData.count)")
                let completeData = incomingData
                incomingData = Data()
                notifyListeners(.responseReceived(data: completeData))
            }
        }
    }
    
    public func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?) {
        if let error = error {
            print("\(TAG): Error changing notification state: \(error.localizedDescription)")
            return
        }
        if characteristic == s2cCharacteristic {
            print("\(TAG): Notifications successfully enabled for Server2Client")
            if stateCharacteristic != nil && c2sCharacteristic != nil {
                print("\(TAG): All characteristics ready and subscribed, notifying connection success")
                notifyListeners(.connected)
            }
        }
    }
    
    public func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?) {
        if let error = error {
            print("\(TAG): Error writing to characteristic \(characteristic.uuid): \(error.localizedDescription)")
            return
        }
        print("\(TAG): didWriteValueFor \(characteristic.uuid)")
    }
}
