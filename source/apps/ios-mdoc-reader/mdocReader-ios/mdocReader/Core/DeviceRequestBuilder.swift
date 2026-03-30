import Foundation
import SwiftCBOR

public struct DeviceRequestBuilder {
    public static func build(docRequests: [DocRequest]) throws -> Data {
        var map = [CBOR: CBOR]()
        map["version"] = "1.0"
        
        var docRequestsArray = [CBOR]()
        for req in docRequests {
            var itemsRequestMap = [CBOR: CBOR]()
            itemsRequestMap["docType"] = CBOR.utf8String(req.docType)
            
            var nameSpacesMap = [CBOR: CBOR]()
            for (ns, items) in req.itemsRequest {
                var nsItemsMap = [CBOR: CBOR]()
                for (item, intent) in items {
                    nsItemsMap[CBOR.utf8String(item)] = CBOR.boolean(intent)
                }
                nameSpacesMap[CBOR.utf8String(ns)] = CBOR.map(nsItemsMap)
            }
            itemsRequestMap["nameSpaces"] = CBOR.map(nameSpacesMap)
            
            // Encode the itemsRequest internally
            let itemsRequestBytes = CBOR.map(itemsRequestMap).encode()
            
            // Create actual DocRequest containing itemsRequest embedded in Tag 24
            var docRequestMap = [CBOR: CBOR]()
            docRequestMap["itemsRequest"] = CBOR.tagged(CBOR.Tag(rawValue: 24), CBOR.byteString(itemsRequestBytes))
            
            docRequestsArray.append(CBOR.map(docRequestMap))
        }
        map["docRequests"] = CBOR.array(docRequestsArray)
        
        return Data(CBOR.map(map).encode())
    }
    
    public struct DocRequest {
        public let docType: String
        public let itemsRequest: [String: [String: Bool]]
        
        public init(_ docType: String, _ itemsRequest: [String: [String: Bool]]) {
            self.docType = docType
            self.itemsRequest = itemsRequest
        }
    }
}
