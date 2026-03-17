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
//  VerifiableCredential.swift
//  oid4vc
//
//  Created by sjkim on 9/9/25.
//

import Foundation

struct VerifiableCredential: Codable {
    let context: [String]
    let type: [String]
    let id: String
    let issuer: Issuer
    let issuanceDate: String
    let validFrom: String?
    let validUntil: String?
    let credentialSchema: CredentialSchema?
    let credentialSubject: CredentialSubject
    let evidence: [Evidence]?
    let proof: Proof?
    let encoding: String?
    let formatVersion: String?
    let language: String?

    enum CodingKeys: String, CodingKey {
        case context = "@context"
        case type, id, issuer, issuanceDate, validFrom, validUntil, credentialSchema, credentialSubject, evidence, proof, encoding, formatVersion, language
    }
    
    struct Issuer: Codable {
        let id: String
        let name: String?
    }

    struct CredentialSchema: Codable {
        let id: String
        let type: String
    }

    struct CredentialSubject: Codable {
        let id: String?
        let claims: [Claim]
    }

    struct Claim: Codable, Identifiable {
        let id = UUID()
        let caption: String
        let code: String?
        let format: String?
        let hideValue: Bool?
        let type: String
        let value: String
        
        enum CodingKeys: String, CodingKey {
            case caption, code, format, hideValue, type, value
        }
    }

    struct Evidence: Codable {
        let type: String
        let verifier: String?
        let evidenceDocument: String?
        let subjectPresence: String?
        let documentPresence: String?
    }

    struct Proof: Codable {
        let type: String
        let created: String?
        let proofPurpose: String?
        let verificationMethod: String?
        let proofValue: String?
        let proofValueList: [String]?
        let nonce: String?
    }
}
