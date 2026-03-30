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
import CoreBluetooth

public struct MdocEngagement {
    public static func parse(_ qrData: String) throws -> EngagementSource {
        print("MDR/Engagement: Parsing QR data")
        guard qrData.hasPrefix("mdoc:") else {
            throw NSError(domain: "MdocEngagement", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid scheme"])
        }
        
        let base64Url = qrData.replacingOccurrences(of: "mdoc:", with: "")
        
        // Base64URL to Data
        var base64 = base64Url
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        while base64.count % 4 != 0 {
            base64.append("=")
        }
        
        guard let data = Data(base64Encoded: base64) else {
            throw NSError(domain: "MdocEngagement", code: 2, userInfo: [NSLocalizedDescriptionKey: "Invalid base64url"])
        }
        
        print("MDR/Engagement: Decoded Base64URL, bytes: \(data.count)")
        
        // Parse CBOR to extract Device Public Key (eDeviceKey)
        // ISO 18013-5: root is a Map. Key 1 is Security (Array).
        // Security[1] is EDeviceKeyBytes (Tag 24).
        guard let cbor = try? CBOR.decode(data.map { $0 }) else {
            throw NSError(domain: "MdocEngagement", code: 3, userInfo: [NSLocalizedDescriptionKey: "CBOR decode failed"])
        }
        
        var devicePublicKeyRaw: Data?
        var bleUUID: CBUUID?
        
        if case let .map(map) = cbor {
            // Extract Public Key
            if let security = map[1], case let .array(secArray) = security, secArray.count > 1 {
                let eDeviceKeyCBOR = secArray[1]
                if case let .tagged(_, inner) = eDeviceKeyCBOR, case let .byteString(coseKeyBytes) = inner {
                    if let coseKey = try? CBOR.decode(coseKeyBytes), case let .map(keyMap) = coseKey {
                        if let xBytes = keyMap[-2], case let .byteString(x) = xBytes,
                           let yBytes = keyMap[-3], case let .byteString(y) = yBytes {
                            let xData = x.count < 32 ? Data(repeating: 0, count: 32 - x.count) + Data(x) : Data(x)
                            let yData = y.count < 32 ? Data(repeating: 0, count: 32 - y.count) + Data(y) : Data(y)
                            var raw = Data([0x04])
                            raw.append(xData)
                            raw.append(yData)
                            devicePublicKeyRaw = raw
                        }
                    }
                }
            }
            
            // Extract BLE UUID (Retrieval Methods)
            if let methods = map[2], case let .array(methodArray) = methods {
                print("MDR/Engagement: Found \(methodArray.count) retrieval methods")
                for method in methodArray {
                    if case let .array(m) = method, m.count > 2 {
                        // Get Type ID (index 0)
                        let typeId: Int64
                        if case let .unsignedInt(val) = m[0] { typeId = Int64(val) }
                        else if case let .negativeInt(val) = m[0] { typeId = -1 - Int64(val) }
                        else { continue }
                        
                        print("MDR/Engagement: Checking method type: \(typeId)")
                        
                        // mdoc-reader-android uses Type 2 for BLE
                        if typeId == 1 || typeId == 2 { 
                            if case let .map(options) = m[2] {
                                // Key 10: Peripheral Mode UUID
                                if let uuidCbor = options[10], case let .byteString(bytes) = uuidCbor {
                                    bleUUID = CBUUID(data: Data(bytes))
                                    print("MDR/Engagement: Successfully extracted BLE UUID: \(bleUUID!.uuidString)")
                                }
                            }
                        }
                    }
                }
            }
        }
        
        guard let pubKey = devicePublicKeyRaw else {
            throw NSError(domain: "MdocEngagement", code: 4, userInfo: [NSLocalizedDescriptionKey: "Failed to extract eDeviceKey"])
        }
        
        return EngagementSource(deviceEngagementBytes: data, devicePublicKeyRaw: pubKey, bleServiceUUID: bleUUID)
    }
}
