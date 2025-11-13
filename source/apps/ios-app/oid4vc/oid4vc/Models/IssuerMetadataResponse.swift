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

// MARK: - Credential Configuration (Main Expanded Model)
struct CredentialConfiguration: Codable {
    let format: String
    let scope: String?
    let cryptographicBindingMethodsSupported: [String]?
    let credentialSigningAlgValuesSupported: [String]?
    let display: [DisplayInfo]?
    let proofTypesSupported: [String: ProofSupport]?
    let vct: String?
    let claims: [String: ClaimDetail]?
    let doctype: String?

    enum CodingKeys: String, CodingKey {
        case format, scope, display, vct, claims, doctype
        case cryptographicBindingMethodsSupported = "cryptographic_binding_methods_supported"
        case credentialSigningAlgValuesSupported = "credential_signing_alg_values_supported"
        case proofTypesSupported = "proof_types_supported"
    }
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
    let proofSigningAlgValuesSupported: [String]

    enum CodingKeys: String, CodingKey {
        case proofSigningAlgValuesSupported = "proof_signing_alg_values_supported"
    }
}

struct ClaimDetail: Codable {
    let display: [DisplayLocaleName]?
}

struct DisplayLocaleName: Codable {
    let name: String?
    let locale: String?
}
