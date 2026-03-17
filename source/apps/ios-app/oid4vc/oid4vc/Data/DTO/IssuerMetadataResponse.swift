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

struct IssuerMetadataResponse: Codable {
    let credentialIssuer: String
    let authorizationServer: [String]?
    let credentialEndpoint: String
    let tokenEndpoint: String?
    let nonceEndpoint: String?
    let deferredCredentialEndpoint: String?
    let notificationEndpoint: String?
    let credentialResponseEncryptionAlgValuesSupported: [String]?
    let credentialResponseEncryptionEncValuesSupported: [String]?
    let requireCredentialResponseEncryption: Bool?
    let credentialIdentifiersSupported: Bool?
    let credentialConfigurationsSupported: [String: CredentialConfiguration]
    
    enum CodingKeys: String, CodingKey {
        case credentialIssuer = "credential_issuer"
        case authorizationServer = "authorization_server"
        case credentialEndpoint = "credential_endpoint"
        case tokenEndpoint = "token_endpoint"
        case nonceEndpoint = "nonce_endpoint"
        case deferredCredentialEndpoint = "deferred_credential_endpoint"
        case notificationEndpoint = "notification_endpoint"
        case credentialResponseEncryptionAlgValuesSupported = "credential_response_encryption_alg_values_supported"
        case credentialResponseEncryptionEncValuesSupported = "credential_response_encryption_enc_values_supported"
        case requireCredentialResponseEncryption = "require_credential_response_encryption"
        case credentialIdentifiersSupported = "credential_identifiers_supported"
        case credentialConfigurationsSupported = "credential_configurations_supported"
    }
}

// MARK: - Flexible Type for Signing Algorithms (handles String and Int)
enum SigningAlg: Codable {
    case string(String)
    case int(Int)
    
    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let x = try? container.decode(String.self) {
            self = .string(x)
        } else if let x = try? container.decode(Int.self) {
            self = .int(x)
        } else {
            throw DecodingError.typeMismatch(SigningAlg.self, DecodingError.Context(codingPath: decoder.codingPath, debugDescription: "Wrong type for SigningAlg"))
        }
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .string(let x): try container.encode(x)
        case .int(let x): try container.encode(x)
        }
    }
}

// MARK: - Credential Configuration (Main Expanded Model)
struct CredentialConfiguration: Codable {
    let format: String
    let scope: String?
    let cryptographicBindingMethodsSupported: [String]?
    let credentialSigningAlgValuesSupported: [SigningAlg]?
    let display: [DisplayInfo]?
    let proofTypesSupported: [String: ProofSupport]?
    let vct: String?
    let claims: [String: ClaimDetail]? // Keep for backward compatibility if needed
    let doctype: String?
    let credentialMetadata: CredentialMetadata?

    enum CodingKeys: String, CodingKey {
        case format, scope, display, vct, claims, doctype
        case cryptographicBindingMethodsSupported = "cryptographic_binding_methods_supported"
        case credentialSigningAlgValuesSupported = "credential_signing_alg_values_supported"
        case proofTypesSupported = "proof_types_supported"
        case credentialMetadata = "credential_metadata"
    }
}

struct CredentialMetadata: Codable {
    let claims: [ClaimDetail]?
    let display: [DisplayInfo]?
}

// MARK: - Sub-Models for Nested JSON

struct DisplayInfo: Codable {
    let name: String?
    let logo: LogoInfo?
    let locale: String?
    let backgroundColor: String?
    let textColor: String?

    enum CodingKeys: String, CodingKey {
        case name, logo, locale
        case backgroundColor = "background_color"
        case textColor = "text_color"
    }
}

struct LogoInfo: Codable {
    let uri: String?
    let altText: String?

    enum CodingKeys: String, CodingKey {
        case uri
        case altText = "alt_text"
    }
}

struct ProofSupport: Codable {
    let proofSigningAlgValuesSupported: [String]?

    enum CodingKeys: String, CodingKey {
        case proofSigningAlgValuesSupported = "proof_signing_alg_values_supported"
    }
}

struct ClaimDetail: Codable {
    let display: [DisplayInfo]? // Flexible: use DisplayInfo which covers name/locale
    let mandatory: Bool?
    let path: [String]?
    let valueType: String?
    
    enum CodingKeys: String, CodingKey {
        case display, mandatory, path
        case valueType = "value_type"
    }
}
