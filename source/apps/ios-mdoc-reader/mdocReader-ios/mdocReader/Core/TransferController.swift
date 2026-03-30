/*
 * Copyright 2026 OmniOne.
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
import SwiftCBOR

public class TransferController {
    private let TAG = "MDR/Transfer"
    public typealias TransferCallback = (TransferStatus) -> Void
    
    private var transportManager: TransportManager?
    private var currentCallback: TransferCallback?
    private var sessionEncryption: SessionEncryption?
    private var engagementSource: EngagementSource?
    
    public init() {}
    
    public func initializeTransferManager(config: TransportConfig) {
        print("\(TAG): Initializing Transfer Manager")
        self.transportManager = BleTransportManager(config: config)
    }
    
    public func startEngagement(source: EngagementSource) {
        print("\(TAG): Starting engagement")
        self.engagementSource = source
        do {
            self.sessionEncryption = try SessionEncryption(devicePublicKeyRaw: source.devicePublicKeyRaw, encodedEngagement: source.deviceEngagementBytes)
            print("\(TAG): Session Encryption initialized successfully")
        } catch {
            print("\(TAG): CRITICAL - Failed to initialize Session Encryption: \(error)")
        }
        transportManager?.startDeviceEngagement(source: source)
    }
    
    public func sendRequest(requestedDocs: [RequestedDocument], retainData: Bool, callback: @escaping TransferCallback) {
        self.currentCallback = callback
        
        guard let manager = transportManager else {
            print("\(TAG): Error - TransportManager not initialized")
            callback(.error("TransportManager not initialized"))
            return
        }
        
        guard let encryption = sessionEncryption else {
            print("\(TAG): Error - SessionEncryption not initialized")
            callback(.error("SessionEncryption not initialized"))
            return
        }
        
        print("\(TAG): Building Device Request")
        let docRequests = requestedDocs.map { doc in
            var items = [String: Bool]()
            doc.claims.forEach { items[$0] = retainData }
            return DeviceRequestBuilder.DocRequest(doc.docType, [doc.namespace: items])
        }
        
        do {
            let requestBytes = try DeviceRequestBuilder.build(docRequests: docRequests)
            print("\(TAG): Device Request built, bytes: \(requestBytes.count)")
            
            // Encrypt Request
            let encryptedRequest = try encryption.encryptRequest(requestBytes)
            print("\(TAG): Request encrypted, bytes: \(encryptedRequest.count)")
            
            // Build SessionEstablishment
            // Expected: { "eReaderKey": Tag24(bstr(.cbor COSE_Key)), "data": bstr(encryptedRequest) }
            let readerKeyCBOR = try encryption.getEReaderKeyCBOR()
            let readerKeyTagged = CBOR.tagged(CBOR.Tag(rawValue: 24), .byteString(readerKeyCBOR.encode()))
            
            var establishmentMap: [CBOR: CBOR] = [:]
            // Important: Use .utf8String for keys and direct CBOR objects for values
            establishmentMap[.utf8String("data")] = .byteString(encryptedRequest.map { $0 })
            establishmentMap[.utf8String("eReaderKey")] = readerKeyTagged
            
            let establishmentBytes = Data(CBOR.map(establishmentMap).encode())
            print("\(TAG): SessionEstablishment built, hex: \(establishmentBytes.map { String(format: "%02x", $0) }.joined())")
            print("\(TAG): SessionEstablishment total bytes: \(establishmentBytes.count)")
            
            let listener = InternalListener(controller: self, establishmentBytes: establishmentBytes)
            manager.addListener(listener)
            
        } catch {
            print("\(TAG): Error building request: \(error.localizedDescription)")
            callback(.error(error.localizedDescription))
        }
    }
    
    private class InternalListener: TransferEvent.Listener {
        weak var controller: TransferController?
        let establishmentBytes: Data
        
        init(controller: TransferController, establishmentBytes: Data) {
            self.controller = controller
            self.establishmentBytes = establishmentBytes
        }
        
        func onEvent(_ event: TransferEvent) {
            guard let controller = controller else { return }
            
            DispatchQueue.main.async {
                switch event {
                case .connecting:
                    print("\(controller.TAG): Event - Connecting")
                    controller.currentCallback?(.connecting)
                case .connected:
                    print("\(controller.TAG): Event - Connected, sending SessionEstablishment")
                    controller.currentCallback?(.connected)
                    controller.transportManager?.sendRequest(deviceRequestBytes: self.establishmentBytes)
                case .deviceEngagementCompleted:
                    print("\(controller.TAG): Event - Device Engagement Completed")
                    controller.currentCallback?(.deviceEngagementCompleted)
                case .requestSent:
                    print("\(controller.TAG): Event - Request Sent")
                    controller.currentCallback?(.requestSent)
                case .responseReceived(let data):
                    print("\(controller.TAG): Event - Response Received, bytes: \(data.count)")
                    controller.processResponse(data)
                case .disconnected:
                    print("\(controller.TAG): Event - Disconnected")
                    controller.currentCallback?(.disconnected)
                case .error(let message):
                    print("\(controller.TAG): Event - Error: \(message)")
                    controller.currentCallback?(.error(message))
                }
            }
        }
    }
    
    private func processResponse(_ data: Data) {
        do {
            print("\(TAG): Processing response")
            print("\(TAG): Received SessionData (hex): \(data.map { String(format: "%02x", $0) }.joined())")
            
            // Parse SessionData wrapper
            guard let cbor = try? CBOR.decode(data.map { $0 }), case let .map(map) = cbor,
                  let encryptedDataBytes = map[.utf8String("data")], case let .byteString(encrypted) = encryptedDataBytes else {
                print("\(TAG): Failed to parse SessionData wrapper")
                currentCallback?(.error("Invalid SessionData wrapper"))
                return
            }
            
            // Decrypt DeviceResponse
            guard let encryption = sessionEncryption else { return }
            let decryptedResponse = try encryption.decryptResponse(Data(encrypted))
            print("\(TAG): Response decrypted successfully, size: \(decryptedResponse.count)")
            print("\(TAG): Decrypted DeviceResponse (hex): \(decryptedResponse.map { String(format: "%02x", $0) }.joined())")
            
            // Parse Decrypted DeviceResponse
            let parsed = try DeviceResponseParser.parse(decryptedResponse, sessionEncryption: encryption)
            
            let receivedDocs = parsed.documents.map { doc in
                ReceivedDocument(isTrusted: true, docType: doc.docType, claims: doc.claims, validity: doc.validity)
            }
            
            print("\(TAG): Completed processing \(receivedDocs.count) documents. Updating UI.")
            currentCallback?(.responseReceived(receivedDocs))
        } catch {
            print("\(TAG): Error processing response: \(error.localizedDescription)")
            currentCallback?(.error("Response processing failed: \(error.localizedDescription)"))
        }
    }
    
    public func stopConnection() {
        print("\(TAG): Stopping connection")
        transportManager?.stopSession()
        currentCallback = nil
    }
}
