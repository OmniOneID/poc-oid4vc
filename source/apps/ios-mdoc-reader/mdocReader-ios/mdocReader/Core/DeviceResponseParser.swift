import Foundation
import SwiftCBOR

public class DeviceResponseParser {
    private static let TAG = "MDR/Parser"
    
    public struct ParsedResponse {
        public let documents: [ParsedDocument]
    }
    
    public struct ParsedDocument {
        public let docType: String
        public let claims: [String: Any]
        public let validity: DocumentValidity
    }
    
    public static func parse(_ data: Data, sessionEncryption: SessionEncryption?) throws -> ParsedResponse {
        print("\(TAG): Starting to parse DeviceResponse")
        guard let cbor = try? CBOR.decode(data.map { $0 }), case let .map(rootMap) = cbor else {
            throw NSError(domain: "DeviceResponseParser", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid DeviceResponse CBOR"])
        }
        
        var parsedDocuments = [ParsedDocument]()
        
        if let documentsCBOR = rootMap[.utf8String("documents")], case let .array(docArray) = documentsCBOR {
            for docCBOR in docArray {
                if case let .map(docMap) = docCBOR,
                   let docTypeCBOR = docMap[.utf8String("docType")], case let .utf8String(docType) = docTypeCBOR {
                    
                    var claims = [String: Any]()
                    
                    if let issuerSignedCBOR = docMap[.utf8String("issuerSigned")], case let .map(isMap) = issuerSignedCBOR {
                        if let namespacesCBOR = isMap[.utf8String("nameSpaces")], case let .map(nsMap) = namespacesCBOR {
                            for (nsKey, nsValue) in nsMap {
                                if case .utf8String(_) = nsKey, case let .array(items) = nsValue {
                                    for itemCBOR in items {
                                        if case let .tagged(tag, inner) = itemCBOR, tag.rawValue == 24,
                                           case let .byteString(itemBytes) = inner {
                                            if let item = try? CBOR.decode(itemBytes), case let .map(itemMap) = item {
                                                if let idCBOR = itemMap[.utf8String("elementIdentifier")], case let .utf8String(id) = idCBOR,
                                                   let valCBOR = itemMap[.utf8String("elementValue")] {
                                                    claims[id] = convertCBORToAny(valCBOR)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    var isDeviceSignatureValid: Bool? = nil
                    var isIssuerSignatureValid: Bool? = nil
                    let isDataIntegrityIntact: Bool? = true
                    var signed: Date? = nil
                    var validFrom: Date? = nil
                    var validUntil: Date? = nil
                    
                    if let issuerSignedCBOR = docMap[.utf8String("issuerSigned")], case let .map(isMap) = issuerSignedCBOR {
                        isIssuerSignatureValid = true
                        if let issuerAuthCBOR = isMap[.utf8String("issuerAuth")] {
                            let coseSign1 = unwrapTag(issuerAuthCBOR)
                            if case let .array(arr) = coseSign1, arr.count >= 3 {
                                if case let .byteString(payloadBytes) = unwrapTag(arr[2]), let payloadCBOR = try? CBOR.decode(payloadBytes) {
                                    let mso = unwrapTag(payloadCBOR)
                                    if case let .map(msoMap) = mso, let viCBOR = msoMap[.utf8String("validityInfo")], case let .map(viMap) = unwrapTag(viCBOR) {
                                        signed = extractDate(from: viMap[.utf8String("signed")])
                                        validFrom = extractDate(from: viMap[.utf8String("validFrom")])
                                        validUntil = extractDate(from: viMap[.utf8String("validUntil")])
                                    }
                                }
                            }
                        }
                    }
                    if let deviceSignedCBOR = docMap[.utf8String("deviceSigned")], case let .map(dsMap) = deviceSignedCBOR {
                        if dsMap[.utf8String("deviceAuth")] != nil {
                            isDeviceSignatureValid = true
                        }
                    }
                    
                    let validity = DocumentValidity(isDeviceSignatureValid: isDeviceSignatureValid, isIssuerSignatureValid: isIssuerSignatureValid, isDataIntegrityIntact: isDataIntegrityIntact, signed: signed, validFrom: validFrom, validUntil: validUntil)
                    
                    parsedDocuments.append(ParsedDocument(docType: docType, claims: claims, validity: validity))
                    print("\(TAG): Successfully parsed document: \(docType) with \(claims.count) claims")
                }
            }
        }
        
        return ParsedResponse(documents: parsedDocuments)
    }
    
    private static func unwrapTag(_ cbor: CBOR) -> CBOR {
        if case let .tagged(tag, inner) = cbor {
            if tag.rawValue == 24, case let .byteString(bytes) = inner, let decoded = try? CBOR.decode(bytes) {
                return unwrapTag(decoded)
            }
            return unwrapTag(inner)
        }
        return cbor
    }
    
    private static func extractDate(from cbor: CBOR?) -> Date? {
        guard let c = cbor else { return nil }
        let unwrapped = unwrapTag(c)
        if case let .date(d) = unwrapped {
            return d
        }
        if case let .utf8String(s) = unwrapped {
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            if let d = formatter.date(from: s) { return d }
            
            let formatter2 = ISO8601DateFormatter()
            if let d = formatter2.date(from: s) { return d }
        }
        return nil
    }
    
    private static func convertCBORToAny(_ cbor: CBOR) -> Any {
        switch cbor {
        case .utf8String(let s): return s
        case .unsignedInt(let i): return Int(i)
        case .negativeInt(let i): return -1 - Int(i)
        case .boolean(let b): return b
        case .byteString(let b): return Data(b)
        case .date(let d):
            let formatter = DateFormatter()
            formatter.dateStyle = .medium
            formatter.timeStyle = .none
            return formatter.string(from: d)
        case .tagged(_, let inner):
            return convertCBORToAny(inner)
        case .array(let array):
            return array.map { convertCBORToAny($0) }
        case .map(let map):
            var dict = [String: Any]()
            for (key, value) in map {
                let keyStr = (convertCBORToAny(key) as? String) ?? String(describing: key)
                dict[keyStr] = convertCBORToAny(value)
            }
            return dict
        default: return String(describing: cbor)
        }
    }
}
