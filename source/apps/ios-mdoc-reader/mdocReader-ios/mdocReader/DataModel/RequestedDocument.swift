import Foundation

public struct RequestedDocument {
    public let docType: String
    public let namespace: String
    public let claims: [String]
    
    public init(docType: String, namespace: String, claims: [String]) {
        self.docType = docType
        self.namespace = namespace
        self.claims = claims
    }
}
