/*
 * Copyright 2025 - 2026 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Foundation
import CoreBluetooth
import CryptoKit

/// BLE implementation of the ISO 18013-5 proximity server.
public class MdocBleServer: NSObject, MdocProximityServer, CBPeripheralManagerDelegate {
    private let serviceUuid: CBUUID
    private let messageHandler: MdocBleMessageHandler
    private var peripheralManager: CBPeripheralManager!
    private var listener: MdocProximityListener?
    private var connectedCentral: CBCentral?
    
    private var stateChar: CBMutableCharacteristic?
    private var server2ClientChar: CBMutableCharacteristic?
    
    private var currentState: UInt8 = BleConstants.STATE_START
    
    public init(bleUuidBytes: [UInt8], privateKey: P256.KeyAgreement.PrivateKey, deviceEngagementBytes: [UInt8]) {
        self.serviceUuid = BleConstants.SERVICE_UUID
        self.messageHandler = MdocBleMessageHandler(eDevicePrivateKey: privateKey, deviceEngagementBytes: deviceEngagementBytes)
        super.init()
        self.peripheralManager = CBPeripheralManager(delegate: self, queue: nil)
        print("DEBUG: [MdocBleServer] Initialized.")
    }
    
    public func setListener(_ listener: MdocProximityListener) {
        self.listener = listener
    }
    
    public func start() {
        if peripheralManager.state == .poweredOn {
            setupGattServer()
        }
    }
    
    public func stop() {
        peripheralManager.stopAdvertising()
        peripheralManager.removeAllServices()
    }
    
    public func getSessionManager() -> MdocSessionManager {
        return messageHandler.getSessionManager()
    }
    
    private func setupGattServer() {
        let service = CBMutableService(type: serviceUuid, primary: true)
        
        let stateCharacteristic = CBMutableCharacteristic(
            type: BleConstants.CHARACTERISTIC_STATE_UUID,
            properties: [.read, .write, .notify],
            value: nil,
            permissions: [.readable, .writeable]
        )
        self.stateChar = stateCharacteristic
        
        let c2sCharacteristic = CBMutableCharacteristic(
            type: BleConstants.CHARACTERISTIC_CLIENT_2_SERVER_UUID,
            properties: [.writeWithoutResponse],
            value: nil,
            permissions: [.writeable]
        )
        
        let s2cCharacteristic = CBMutableCharacteristic(
            type: BleConstants.CHARACTERISTIC_SERVER_2_CLIENT_UUID,
            properties: [.notify],
            value: nil,
            permissions: []
        )
        self.server2ClientChar = s2cCharacteristic
        
        let identCharacteristic = CBMutableCharacteristic(
            type: BleConstants.CHARACTERISTIC_IDENT_UUID,
            properties: [.read],
            value: Data(repeating: 0, count: 16),
            permissions: [.readable]
        )
        
        service.characteristics = [stateCharacteristic, c2sCharacteristic, s2cCharacteristic, identCharacteristic]
        peripheralManager.add(service)
    }
    
    private func startAdvertising() {
        print("DEBUG: [MdocBleServer] Starting Advertising")
        peripheralManager.startAdvertising([
            CBAdvertisementDataServiceUUIDsKey: [serviceUuid]
        ])
    }
    
    // MARK: - CBPeripheralManagerDelegate
    
    public func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        if peripheral.state == .poweredOn {
            setupGattServer()
        }
    }
    
    public func peripheralManager(_ peripheral: CBPeripheralManager, didAdd service: CBService, error: Error?) {
        if error == nil { startAdvertising() }
    }
    
    public func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveRead request: CBATTRequest) {
        if request.characteristic.uuid == BleConstants.CHARACTERISTIC_STATE_UUID {
            request.value = Data([currentState])
            peripheralManager.respond(to: request, withResult: .success)
            print("DEBUG: [MdocBleServer] Responded to STATE read: \(currentState)")
        } else if request.characteristic.uuid == BleConstants.CHARACTERISTIC_IDENT_UUID {
            request.value = Data(repeating: 0, count: 16)
            peripheralManager.respond(to: request, withResult: .success)
        } else {
            peripheralManager.respond(to: request, withResult: .requestNotSupported)
        }
    }
    
    public func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveWrite requests: [CBATTRequest]) {
        for request in requests {
            if request.characteristic.uuid == BleConstants.CHARACTERISTIC_CLIENT_2_SERVER_UUID {
                if let value = request.value {
                    let chunk = [UInt8](value)
                    if let fullRequest = messageHandler.receiveChunk(chunk) {
                        listener?.onRequestReceived(request: fullRequest)
                    }
                }
                peripheralManager.respond(to: request, withResult: .success)
            } else if request.characteristic.uuid == BleConstants.CHARACTERISTIC_STATE_UUID {
                if let value = request.value, !value.isEmpty {
                    self.currentState = value[0]
                    print("DEBUG: [MdocBleServer] State updated to: \(currentState)")
                }
                peripheralManager.respond(to: request, withResult: .success)
            }
        }
    }
    
    public func peripheralManager(_ peripheral: CBPeripheralManager, central: CBCentral, didSubscribeTo characteristic: CBCharacteristic) {
        print("DEBUG: [MdocBleServer] Central subscribed to \(characteristic.uuid.uuidString)")
        if characteristic.uuid == BleConstants.CHARACTERISTIC_STATE_UUID || characteristic.uuid == BleConstants.CHARACTERISTIC_SERVER_2_CLIENT_UUID {
            self.connectedCentral = central
            
            // [수정] 모든 초기 알림 로직 제거. 리더기의 Read/Write를 기다립니다.
            
            listener?.onDeviceConnected()
            peripheralManager.stopAdvertising()
        }
    }
    
    public func peripheralManager(_ peripheral: CBPeripheralManager, central: CBCentral, didUnsubscribeFrom characteristic: CBCharacteristic) {
        print("DEBUG: [MdocBleServer] Central unsubscribed from \(characteristic.uuid.uuidString)")
        listener?.onDeviceDisconnected()
        self.connectedCentral = nil
        startAdvertising()
    }
    
    public func sendResponse(response: [UInt8]) {
        guard let central = connectedCentral, let s2cChar = server2ClientChar else { return }
        
//        let maxChunkSize = central.maximumUpdateValueLength - 3 - 1
        let maxChunkSize = 512 - 3 - 1
        var offset = 0
        
        DispatchQueue.global().async { [weak self] in
            guard let self = self else { return }
            while offset < response.count {
                let end = min(offset + maxChunkSize, response.count)
                let chunkData = Array(response[offset..<end])
                var chunk = [UInt8]()
                chunk.append(end < response.count ? 0x01 : 0x00)
                chunk.append(contentsOf: chunkData)
                
                let didSend = self.peripheralManager.updateValue(Data(chunk), for: s2cChar, onSubscribedCentrals: [central])
                if didSend {
                    offset += maxChunkSize
                } else {
                    Thread.sleep(forTimeInterval: 0.02)
                }
            }
            
            self.currentState = BleConstants.STATE_END
            if let stateChar = self.stateChar {
                self.peripheralManager.updateValue(Data([BleConstants.STATE_END]), for: stateChar, onSubscribedCentrals: [central])
                print("DEBUG: [MdocBleServer] State notified as END (0x02)")
            }
        }
    }
}
