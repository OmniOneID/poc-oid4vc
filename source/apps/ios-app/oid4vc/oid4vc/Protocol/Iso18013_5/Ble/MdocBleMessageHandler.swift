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
import SwiftCBOR
import CryptoKit

/// Handles incoming BLE chunks and assembles them into complete messages.
public class MdocBleMessageHandler {
    private let sessionManager: MdocSessionManager
    private var messageBuffer = [UInt8]()
    
    public init(eDevicePrivateKey: P256.KeyAgreement.PrivateKey, deviceEngagementBytes: [UInt8]) {
        self.sessionManager = MdocSessionManager(eDevicePrivateKey: eDevicePrivateKey, deviceEngagementBytes: deviceEngagementBytes)
    }
    
    /// Processes a received chunk and returns the complete message if assembly is finished.
    public func receiveChunk(_ chunk: [UInt8]) -> [UInt8]? {
        guard !chunk.isEmpty else { return nil }
        
        let header = chunk[0]
        let data = Array(chunk.suffix(from: 1))
        messageBuffer.append(contentsOf: data)
        
        if header == 0x01 {
            // More chunks coming
            return nil
        } else if header == 0x00 {
            // Last chunk
            let completeMessage = messageBuffer
            messageBuffer.removeAll()
            
            // Try to decrypt and log (mirroring Android behavior)
            if let deviceRequest = sessionManager.decryptSessionEstablishment(message: completeMessage) {
                print("==== ✅ Reader request decrypted successfully! ====")
            }
            
            return completeMessage
        }
        
        return nil
    }
    
    public func getSessionManager() -> MdocSessionManager {
        return sessionManager
    }
}
