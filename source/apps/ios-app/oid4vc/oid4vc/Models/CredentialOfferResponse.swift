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

//
//  CredentialOfferResponse.swift
//  oid4vc
//
//  Created by sjkim on 8/29/25.
//

import Foundation

struct CredentialOfferResponse: Codable {
    let credentialIssuer: String
    let credentialConfigurationIds: [String]?
    let grants: Grants

    enum CodingKeys: String, CodingKey {
        case credentialIssuer = "credential_issuer"
        case credentialConfigurationIds = "credential_configuration_ids"
        case grants
    }
}

struct Grants: Codable {
    let preAuthorizedCode: PreAuthorizedCode?
    let authorizationCode: AuthorizationCode?

    enum CodingKeys: String, CodingKey {
        case preAuthorizedCode = "urn:ietf:params:oauth:grant-type:pre-authorized_code"
        case authorizationCode = "authorization_code"
    }
}

struct PreAuthorizedCode: Codable {
    let preAuthorizedCode: String
    let txCode: TxCode?

    enum CodingKeys: String, CodingKey {
        case preAuthorizedCode = "pre-authorized_code"
        case txCode = "tx_code"
    }
}

struct TxCode: Codable {
    let inputMode: String?
    let length: Int?
    let description: String?

    enum CodingKeys: String, CodingKey {
        case inputMode = "input_mode"
        case length
        case description
    }
}

struct AuthorizationCode: Codable {
    let issuerState: String?

    enum CodingKeys: String, CodingKey {
        case issuerState = "issuer_state"
    }
}

struct TestCredentialOfferResponse: Codable {
    let credentialIssuer: String
    let credentialConfigurationIds: [String]?
    let grants: Grants
    let txCode: String

    enum CodingKeys: String, CodingKey {
        case credentialIssuer = "credential_issuer"
        case credentialConfigurationIds = "credential_configuration_ids"
        case grants
        case txCode = "tx_code"
    }
}
