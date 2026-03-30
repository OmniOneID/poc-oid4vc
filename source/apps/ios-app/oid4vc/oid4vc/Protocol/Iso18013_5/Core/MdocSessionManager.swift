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
import CryptoKit
import SwiftCBOR

/// Manages an ISO 18013-5 session, including encryption/decryption and session transcript.
public class MdocSessionManager {
    private let eDevicePrivateKey: P256.KeyAgreement.PrivateKey
    private let deviceEngagementBytes: [UInt8]
    private var eReaderKey: CBOR?
    private var handover: CBOR = .null
    
    public init(eDevicePrivateKey: P256.KeyAgreement.PrivateKey, deviceEngagementBytes: [UInt8]) {
        self.eDevicePrivateKey = eDevicePrivateKey
        self.deviceEngagementBytes = deviceEngagementBytes
    }
    
    public func setHandover(_ handover: CBOR?) {
        self.handover = handover ?? .null
    }
    
    public func getEReaderKey() -> CBOR? {
        return eReaderKey
    }

    public func decryptSessionEstablishment(message: [UInt8]) -> CBOR? {
        do {
            guard let sessionEstablishment = try? CBOR.decode(message),
                  case let .map(map) = sessionEstablishment,
                  let eReaderKeyCBOR = map[.utf8String("eReaderKey")],
                  let dataCBOR = map[.utf8String("data")],
                  case let .byteString(encryptedDeviceRequest) = dataCBOR else {
                return nil
            }
            
            self.eReaderKey = eReaderKeyCBOR
            
            let eReaderPublicKey = try getPublicKeyFromCoseKey(eReaderKeyCBOR)
            let sharedSecret = try eDevicePrivateKey.sharedSecretFromKeyAgreement(with: eReaderPublicKey)
            let rawZ = [UInt8](sharedSecret.withUnsafeBytes { Data($0) })
            let sharedSecretZ = normalizeSharedSecret(rawZ)
            
            let salt = try generateSalt(engagement: deviceEngagementBytes, eReaderKey: eReaderKeyCBOR, handover: handover)
            let skReader = deriveKey(z: sharedSecretZ, salt: salt, infoStr: "SKReader")
            
            var iv = [UInt8](repeating: 0, count: 12)
            iv[11] = 1
            
            let decryptedBytes = try aesGcmDecrypt(key: skReader, iv: iv, encrypted: encryptedDeviceRequest)
            return try? CBOR.decode(decryptedBytes)
            
        } catch {
            return nil
        }
    }

    public func generateDeviceResponse(mDoc: String, selectedClaimsKeys: [String]?, selectedClaimsNamespaces: [String]?, holderPrivateKey: P256.Signing.PrivateKey) throws -> [UInt8] {
        guard let eReaderKey = self.eReaderKey else {
            throw NSError(domain: "MdocSessionManager", code: 1, userInfo: [NSLocalizedDescriptionKey: "eReaderKey is missing."])
        }
        
        guard let decodedData = Base64URL.decode(mDoc) else {
            throw NSError(domain: "MdocSessionManager", code: 2, userInfo: [NSLocalizedDescriptionKey: "Invalid mDoc data"])
        }
        
        let mDocBytes = [UInt8](decodedData)
        guard let decodedCBOR = try? CBOR.decode(mDocBytes),
              case var .map(issuerSignedMap) = decodedCBOR else {
            throw NSError(domain: "MdocSessionManager", code: 3, userInfo: [NSLocalizedDescriptionKey: "Failed to decode mDoc CBOR"])
        }
        
        var docType = ""
        var namespaceStr = ""
        if let nameSpaces = issuerSignedMap[.utf8String("nameSpaces")],
           case let .map(nsMap) = nameSpaces {
            for (key, _) in nsMap {
                if case let .utf8String(ns) = key {
                    namespaceStr = ns
                    break
                }
            }
        }
        
        if namespaceStr == "eu.europa.ec.eudi.pid.1" {
            docType = "eu.europa.ec.eudi.pid.1"
        } else if namespaceStr == "org.iso.18013.5.1" {
            docType = "org.iso.18013.5.1.mDL"
        }
        
        // Filtering
        if let selectedKeys = selectedClaimsKeys, !selectedKeys.isEmpty {
            if let originalNameSpaces = issuerSignedMap[.utf8String("nameSpaces")],
               case let .map(nsMap) = originalNameSpaces {
                
                var filteredNameSpaces: [CBOR: CBOR] = [:]
                for (nsKey, nsValue) in nsMap {
                    guard case let .utf8String(currentNamespace) = nsKey,
                          case let .array(items) = nsValue else { continue }
                    
                    var filteredItems: [CBOR] = []
                    for item in items {
                        if let itemBytes = getByteString(item), let decodedItem = try? CBOR.decode(itemBytes), case let .map(itemMap) = decodedItem {
                            if let idCBOR = itemMap[.utf8String("elementIdentifier")], case let .utf8String(elementIdentifier) = idCBOR {
                                var isRequested = false
                                for (index, reqKey) in selectedKeys.enumerated() {
                                    let reqNs = (selectedClaimsNamespaces != nil && selectedClaimsNamespaces!.count > index) ? selectedClaimsNamespaces![index] : ""
                                    if elementIdentifier == reqKey && currentNamespace == reqNs { isRequested = true; break }
                                }
                                if isRequested { filteredItems.append(item) }
                            }
                        }
                    }
                    if !filteredItems.isEmpty { filteredNameSpaces[.utf8String(currentNamespace)] = .array(filteredItems) }
                }
                issuerSignedMap[.utf8String("nameSpaces")] = .map(filteredNameSpaces)
            }
        }
        
        let sessionTranscript: CBOR = .array([
            .tagged(CBOR.Tag(rawValue: 24), .byteString(deviceEngagementBytes)),
            eReaderKey,
            handover
        ])
        
        let deviceSigned = try generateDeviceAuth(holderPrivateKey: holderPrivateKey, docType: docType, sessionTranscript: sessionTranscript)
        
        var document: [CBOR: CBOR] = [:]
        document[.utf8String("docType")] = .utf8String(docType)
        document[.utf8String("issuerSigned")] = .map(issuerSignedMap)
        document[.utf8String("deviceSigned")] = deviceSigned
        
        var deviceResponse: [CBOR: CBOR] = [:]
        deviceResponse[.utf8String("version")] = .utf8String("1.0")
        deviceResponse[.utf8String("documents")] = .array([.map(document)])
        deviceResponse[.utf8String("status")] = .unsignedInt(0)
        
        let deviceResponseBytes = deviceResponse.encode()
        
        let eReaderPublicKey = try getPublicKeyFromCoseKey(eReaderKey)
        let sharedSecret = try eDevicePrivateKey.sharedSecretFromKeyAgreement(with: eReaderPublicKey)
        let rawZ = [UInt8](sharedSecret.withUnsafeBytes { Data($0) })
        let sharedSecretZ = normalizeSharedSecret(rawZ)
        
        let salt = try generateSalt(engagement: deviceEngagementBytes, eReaderKey: eReaderKey, handover: handover)
        let skDevice = deriveKey(z: sharedSecretZ, salt: salt, infoStr: "SKDevice")
        
        var iv = [UInt8](repeating: 0, count: 12)
        iv[7] = 1; iv[11] = 1
        
        let encryptedResponse = try aesGcmEncrypt(key: skDevice, iv: iv, data: deviceResponseBytes)
        
        var sessionData: [CBOR: CBOR] = [:]
        sessionData[.utf8String("data")] = .byteString(encryptedResponse)
        sessionData[.utf8String("status")] = .unsignedInt(20)
        
        return sessionData.encode()
    }

    private func generateDeviceAuth(holderPrivateKey: P256.Signing.PrivateKey, docType: String, sessionTranscript: CBOR) throws -> CBOR {
        let emptyNameSpaces = CBOR.tagged(CBOR.Tag(rawValue: 24), .byteString(CBOR.map([:]).encode()))
        
        let deviceAuthentication: CBOR = .array([
            .utf8String("DeviceAuthentication"),
            sessionTranscript,
            .utf8String(docType),
            emptyNameSpaces
        ])
        
        let deviceAuthBytes = deviceAuthentication.encode()
        let taggedDeviceAuthBytes = CBOR.tagged(CBOR.Tag(rawValue: 24), .byteString(deviceAuthBytes)).encode()
        
        let protectedHeader: CBOR = .map([.unsignedInt(1): .negativeInt(6)]) // ES256 (-7)
        let protectedHeaderBytes = protectedHeader.encode()
        
        let signatureStructure: CBOR = .array([
            .utf8String("Signature1"),
            .byteString(protectedHeaderBytes),
            .byteString([]),
            .byteString(taggedDeviceAuthBytes)
        ])
        
        let signature = try holderPrivateKey.signature(for: Data(signatureStructure.encode()))
        let signatureBytes = Array(signature.rawRepresentation)
        
        let coseSign1: CBOR = .array([
            .byteString(protectedHeaderBytes),
            .map([:]),
            .null,
            .byteString(signatureBytes)
        ])
        
        var deviceAuthMap: [CBOR: CBOR] = [:]
        deviceAuthMap[.utf8String("deviceSignature")] = coseSign1
        
        var deviceSignedDict: [CBOR: CBOR] = [:]
        deviceSignedDict[.utf8String("nameSpaces")] = emptyNameSpaces
        deviceSignedDict[.utf8String("deviceAuth")] = .map(deviceAuthMap)
        
        return .map(deviceSignedDict)
    }
    
    private func normalizeSharedSecret(_ rawZ: [UInt8]) -> [UInt8] {
        if rawZ.count > 32 { return Array(rawZ.suffix(32)) }
        if rawZ.count < 32 {
            var sharedSecretZ = [UInt8](repeating: 0, count: 32)
            sharedSecretZ.replaceSubrange((32 - rawZ.count)..<32, with: rawZ)
            return sharedSecretZ
        }
        return rawZ
    }

    private func generateSalt(engagement: [UInt8], eReaderKey: CBOR, handover: CBOR) throws -> [UInt8] {
        let sessionTranscript: CBOR = .array([
            .tagged(CBOR.Tag(rawValue: 24), .byteString(engagement)),
            eReaderKey,
            handover
        ])
        
        // Mirror Android's generateSalt exactly:
        // 1. Encode SessionTranscript array to bytes
        let encodedTranscriptArray = sessionTranscript.encode()
        // 2. Wrap those bytes in Tag 24
        let taggedTranscript = CBOR.tagged(CBOR.Tag(rawValue: 24), .byteString(encodedTranscriptArray))
        // 3. Encode the tagged object and hash it
        let finalTranscriptBytes = taggedTranscript.encode()
        
        return [UInt8](SHA256.hash(data: Data(finalTranscriptBytes)))
    }

    private func deriveKey(z: [UInt8], salt: [UInt8], infoStr: String) -> SymmetricKey {
        return HKDF<SHA256>.deriveKey(inputKeyMaterial: SymmetricKey(data: z), salt: salt, info: infoStr.data(using: .utf8)!, outputByteCount: 32)
    }

    private func aesGcmDecrypt(key: SymmetricKey, iv: [UInt8], encrypted: [UInt8]) throws -> [UInt8] {
        let nonce = try AES.GCM.Nonce(data: iv)
        let tagSize = 16
        let box = try AES.GCM.SealedBox(nonce: nonce, ciphertext: encrypted.prefix(encrypted.count - tagSize), tag: encrypted.suffix(tagSize))
        return [UInt8](try AES.GCM.open(box, using: key))
    }
    
    private func aesGcmEncrypt(key: SymmetricKey, iv: [UInt8], data: [UInt8]) throws -> [UInt8] {
        let sealedBox = try AES.GCM.seal(data, using: key, nonce: try AES.GCM.Nonce(data: iv))
        return [UInt8](sealedBox.ciphertext + sealedBox.tag)
    }

    private func getPublicKeyFromCoseKey(_ coseKeyObj: CBOR) throws -> P256.KeyAgreement.PublicKey {
        let coseKeyMap: [CBOR: CBOR]
        if case let .tagged(tag, content) = coseKeyObj, tag.rawValue == 24, case let .byteString(bytes) = content {
            guard let decoded = try? CBOR.decode(bytes), case let .map(m) = decoded else { throw NSError(domain: "Mdoc", code: 4) }
            coseKeyMap = m
        } else if case let .map(m) = coseKeyObj { coseKeyMap = m }
        else { throw NSError(domain: "Mdoc", code: 5) }
        
        guard let xCBOR = coseKeyMap[.negativeInt(1)], let yCBOR = coseKeyMap[.negativeInt(2)],
              case let .byteString(xBytes) = xCBOR, case let .byteString(yBytes) = yCBOR else { throw NSError(domain: "Mdoc", code: 6) }
        return try P256.KeyAgreement.PublicKey(x963Representation: [0x04] + xBytes + yBytes)
    }
    
    private func getByteString(_ cbor: CBOR) -> [UInt8]? {
        switch cbor {
        case .byteString(let bytes): return bytes
        case .tagged(let tag, let content): if tag.rawValue == 24 { return getByteString(content) }
        default: break
        }
        return nil
    }
    
    private func cborToAny(_ cbor: CBOR) -> Any {
        switch cbor {
        case .utf8String(let s): return s
        case .byteString(let b): return Data(b)
        case .unsignedInt(let i): return Int(i)
        case .negativeInt(let i): return -Int(i) - 1
        case .boolean(let b): return b
        case .array(let a): return a.map { cborToAny($0) }
        case .map(let m):
            var dict: [String: Any] = [:]
            for (key, value) in m { if case let .utf8String(s) = key { dict[s] = cborToAny(value) } }
            return dict
        case .double(let d): return d
        case .float(let f): return f
        case .null: return NSNull()
        case .tagged(_, let content): return cborToAny(content)
        default: return String(describing: cbor)
        }
    }
}
