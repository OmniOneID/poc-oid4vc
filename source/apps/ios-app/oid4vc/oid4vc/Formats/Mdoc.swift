/*
 * Copyright 2025 OmniOne.
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

/// A class providing utility methods for handling the mDoc (mobile document) format.
public class Mdoc {
    private static let PRIVATE_KEY = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT"

    /// Checks if the given format is supported for mDoc.
    /// - Parameter format: The format string to check.
    /// - Returns: true if the format is supported, false otherwise.
    public static func isSupported(format: String) -> Bool {
        return format == "mDL" || format == "mDocPID"
    }

    /// Creates a selectively disclosed VP token for mDoc based on DCQL.
    /// - Parameters:
    ///   - mDoc: The raw mDoc data in Base64URL format.
    ///   - selectedClaimsKeys: The list of claim keys selected for disclosure.
    ///   - selectedClaimsNamespaces: The list of namespaces for the selected claims.
    ///   - aud: The audience for the VP token.
    ///   - nonce: The nonce for the VP token.
    ///   - responseUri: The response URI for the VP token.
    /// - Returns: The generated VP token string.
    public static func createVpToken(mDoc: String, selectedClaimsKeys: [String]?, selectedClaimsNamespaces: [String]?, aud: String, nonce: String, responseUri: String) throws -> String {
        LogUtil.logLongString("sangjun", "mdoc: \(mDoc)")
        
        guard let privateKey = CryptoUtil.getPrivateKeyFromBase64(PRIVATE_KEY) else {
            throw NSError(domain: "Mdoc", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid private key"])
        }

        guard let decodedData = Base64URL.decode(mDoc) else {
            throw NSError(domain: "Mdoc", code: 2, userInfo: [NSLocalizedDescriptionKey: "Invalid mDoc data"])
        }
        
        let mDocBytes = [UInt8](decodedData)
        guard let decodedCBOR = try? CBOR.decode(mDocBytes),
              case var .map(issuerSignedMap) = decodedCBOR else {
            throw NSError(domain: "Mdoc", code: 3, userInfo: [NSLocalizedDescriptionKey: "Failed to decode mDoc CBOR"])
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
        
        if let selectedKeys = selectedClaimsKeys, !selectedKeys.isEmpty {
            if let originalNameSpaces = issuerSignedMap[.utf8String("nameSpaces")],
               case let .map(nsMap) = originalNameSpaces {
                
                var filteredNameSpaces: [CBOR: CBOR] = [:]
                
                for (nsKey, nsValue) in nsMap {
                    guard case let .utf8String(currentNamespace) = nsKey,
                          case let .array(items) = nsValue else { continue }
                    
                    var filteredItems: [CBOR] = []
                    
                    for item in items {
                        if let itemBytes = getByteString(item) {
                            if let decodedItem = try? CBOR.decode(itemBytes),
                               case let .map(itemMap) = decodedItem,
                               let idCBOR = itemMap[.utf8String("elementIdentifier")],
                               case let .utf8String(elementIdentifier) = idCBOR {
                                
                                var isRequested = false
                                for (index, reqKey) in selectedKeys.enumerated() {
                                    let reqNs = (selectedClaimsNamespaces != nil && selectedClaimsNamespaces!.count > index) ? selectedClaimsNamespaces![index] : ""
                                    
                                    if elementIdentifier == reqKey && currentNamespace == reqNs {
                                        isRequested = true
                                        break
                                    }
                                }
                                
                                if isRequested {
                                    filteredItems.append(item)
                                }
                            }
                        }
                    }
                    
                    if !filteredItems.isEmpty {
                        filteredNameSpaces[.utf8String(currentNamespace)] = .array(filteredItems)
                    }
                }
                issuerSignedMap[.utf8String("nameSpaces")] = .map(filteredNameSpaces)
            }
        }

        let deviceSigned = try generateDeviceAuth(privateKey: privateKey, docType: docType, aud: aud, nonce: nonce, responseUri: responseUri)
        
        var document: [CBOR: CBOR] = [:]
        document[.utf8String("docType")] = .utf8String(docType)
        document[.utf8String("issuerSigned")] = .map(issuerSignedMap)
        document[.utf8String("deviceSigned")] = deviceSigned
        
        var deviceResponse: [CBOR: CBOR] = [:]
        deviceResponse[.utf8String("status")] = .unsignedInt(0)
        deviceResponse[.utf8String("version")] = .utf8String("1.0")
        deviceResponse[.utf8String("documents")] = .array([.map(document)])
        
        let deviceResponseBytes = deviceResponse.encode()
        let vpToken = Base64URL.encode(Data(deviceResponseBytes))
        LogUtil.logLongString("sangjun", "vpToken: \(vpToken)")
        
        let dcqlId = "query_0"
        return "{\"\(dcqlId)\":[\"\(vpToken)\"]}"
    }

    /// Builds the session transcript used for device authentication.
    /// - Parameters:
    ///   - aud: The audience.
    ///   - nonce: The nonce.
    ///   - responseUri: The response URI.
    /// - Returns: A CBOR representation of the session transcript.
    private static func buildSessionTranscript(aud: String, nonce: String, responseUri: String) throws -> CBOR {
        let handoverInfo: CBOR = .array([
            .utf8String(aud),
            .utf8String(nonce),
            .null,
            .utf8String(responseUri)
        ])

        let handoverInfoBytes = handoverInfo.encode()
        let handoverInfoHash = SHA256.hash(data: Data(handoverInfoBytes))
        
        let handover: CBOR = .array([
            .utf8String("OpenID4VPHandover"),
            .byteString(Array(handoverInfoHash))
        ])

        let sessionTranscript: CBOR = .array([
            .null,
            .null,
            handover
        ])

        return sessionTranscript
    }

    /// Generates the device authentication CBOR object including a COSE Sign1 signature.
    /// - Parameters:
    ///   - privateKey: The signing private key.
    ///   - docType: The document type.
    ///   - aud: The audience.
    ///   - nonce: The nonce.
    ///   - responseUri: The response URI.
    /// - Returns: A CBOR object representing the device signed data.
    private static func generateDeviceAuth(privateKey: P256.Signing.PrivateKey, docType: String, aud: String, nonce: String, responseUri: String) throws -> CBOR {
        let sessionTranscript = try buildSessionTranscript(aud: aud, nonce: nonce, responseUri: responseUri)

        let emptyDeviceNameSpaces: CBOR = .map([:])
        let deviceNameSpacesBytes = emptyDeviceNameSpaces.encode()
        let deviceNameSpacesTagged = CBOR.tagged(CBOR.Tag(rawValue: 24), .byteString(deviceNameSpacesBytes))

        let deviceAuth: CBOR = .array([
            .utf8String("DeviceAuthentication"),
            sessionTranscript,
            .utf8String(docType),
            deviceNameSpacesTagged
        ])

        let deviceAuthBytes = deviceAuth.encode()
        let contentCBOR = CBOR.tagged(CBOR.Tag(rawValue: 24), .byteString(deviceAuthBytes))
        let contentBytes = contentCBOR.encode()
        
        let protectedHeader: CBOR = .map([.unsignedInt(1): .negativeInt(6)])
        let protectedHeaderBytes = protectedHeader.encode()
        
        let externalAad: [UInt8] = []
        let sigStructure: CBOR = .array([
            .utf8String("Signature1"),
            .byteString(protectedHeaderBytes),
            .byteString(externalAad),
            .byteString(contentBytes)
        ])
        
        let sigStructureBytes = sigStructure.encode()
        
        let signature = try privateKey.signature(for: Data(sigStructureBytes))
        let signatureBytes = Array(signature.rawRepresentation)
        
        let coseSign1: CBOR = .array([
            .byteString(protectedHeaderBytes),
            .map([:]),
            .byteString(contentBytes),
            .byteString(signatureBytes)
        ])
        
        var deviceAuthMap: [CBOR: CBOR] = [:]
        deviceAuthMap[.utf8String("deviceSignature")] = coseSign1
        
        var deviceSigned: [CBOR: CBOR] = [:]
        deviceSigned[.utf8String("deviceAuth")] = .map(deviceAuthMap)
        
        return .map(deviceSigned)
    }

    /// Extracts claims from an mDL/mDoc Verifiable Credential.
    /// - Parameter mDlData: The raw mDoc data in Base64URL format.
    /// - Returns: A map containing the extracted claims.
    public static func getClaims(mDlData: String) -> [String: Any] {
        var result: [String: Any] = [:]
        
        guard let decodedData = Base64URL.decode(mDlData) else {
            return result
        }
        
        let bytes = [UInt8](decodedData)
        guard let cbor = try? CBOR.decode(bytes),
              case let .map(map) = cbor else {
            return result
        }
        
        if let nameSpacesCBOR = map[.utf8String("nameSpaces")],
           case let .map(nameSpacesMap) = nameSpacesCBOR {
            
            for (nsKey, nsValue) in nameSpacesMap {
                guard case let .utf8String(namespace) = nsKey,
                      case let .array(items) = nsValue else { continue }
                
                var nsClaims: [String: Any] = [:]
                
                for item in items {
                    if let itemBytes = getByteString(item) {
                        if let decodedItem = try? CBOR.decode(itemBytes),
                           case let .map(itemMap) = decodedItem {
                            
                            if let idCBOR = itemMap[.utf8String("elementIdentifier")],
                               let valCBOR = itemMap[.utf8String("elementValue")],
                               case let .utf8String(elementId) = idCBOR {
                                
                                nsClaims[elementId] = cborToAny(valCBOR)
                            }
                        }
                    }
                }
                result[namespace] = nsClaims
            }
        }
        
        return result
    }
    
    /// Extracts a ByteString from a CBOR object, handling potential tagging.
    /// - Parameter cbor: The CBOR object.
    /// - Returns: A byte array if extraction is successful.
    private static func getByteString(_ cbor: CBOR) -> [UInt8]? {
        switch cbor {
        case .byteString(let bytes):
            return bytes
        case .tagged(let tag, let content):
            if tag.rawValue == 24 {
                return getByteString(content)
            }
        default:
            break
        }
        return nil
    }
    
    /// Converts a CBOR object into a Swift Any type.
    /// - Parameter cbor: The CBOR object.
    /// - Returns: A Swift Any representation.
    private static func cborToAny(_ cbor: CBOR) -> Any {
        switch cbor {
        case .utf8String(let s): return s
        case .byteString(let b): return Data(b)
        case .unsignedInt(let i): return Int(i)
        case .negativeInt(let i): return -Int(i) - 1
        case .boolean(let b): return b
        case .array(let a): return a.map { cborToAny($0) }
        case .map(let m):
            var dict: [String: Any] = [:]
            for (key, value) in m {
                if case let .utf8String(s) = key {
                    dict[s] = cborToAny(value)
                }
            }
            return dict
        case .double(let d): return d
        case .float(let f): return f
        case .null: return NSNull()
        case .tagged(_, let content): return cborToAny(content)
        default: return String(describing: cbor)
        }
    }
}
