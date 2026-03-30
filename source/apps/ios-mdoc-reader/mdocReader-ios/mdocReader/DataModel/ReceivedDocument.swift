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

public struct ReceivedDocument {
    public let isTrusted: Bool
    public let docType: String
    public let claims: [String: Any]
    public let validity: DocumentValidity
    
    public init(isTrusted: Bool, docType: String, claims: [String: Any], validity: DocumentValidity) {
        self.isTrusted = isTrusted
        self.docType = docType
        self.claims = claims
        self.validity = validity
    }
}

public struct DocumentValidity {
    public let isDeviceSignatureValid: Bool?
    public let isIssuerSignatureValid: Bool?
    public let isDataIntegrityIntact: Bool?
    public let signed: Date?
    public let validFrom: Date?
    public let validUntil: Date?
    
    public init(isDeviceSignatureValid: Bool? = nil,
                isIssuerSignatureValid: Bool? = nil,
                isDataIntegrityIntact: Bool? = nil,
                signed: Date? = nil,
                validFrom: Date? = nil,
                validUntil: Date? = nil) {
        self.isDeviceSignatureValid = isDeviceSignatureValid
        self.isIssuerSignatureValid = isIssuerSignatureValid
        self.isDataIntegrityIntact = isDataIntegrityIntact
        self.signed = signed
        self.validFrom = validFrom
        self.validUntil = validUntil
    }
}
