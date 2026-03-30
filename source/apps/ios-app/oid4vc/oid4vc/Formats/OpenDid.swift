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

/// A class providing utility methods for handling the OpenDID format.
public class OpenDid {
    
    /// Checks if the given format is supported for OpenDID.
    /// - Parameter format: The format string to check.
    /// - Returns: true if the format is supported, false otherwise.
    public static func isSupported(format: String) -> Bool {
        return format == "TEC" || format == "UCR"
    }

    /// Creates a VP token for OpenDID.
    /// - Parameters:
    ///   - nonce: The nonce for the VP token.
    ///   - aud: The audience for the VP token.
    ///   - vcCredential: The Verifiable Credential data in Base64 format.
    /// - Returns: The generated VP token string.
    public static func createVpToken(nonce: String, aud: String, vcCredential: String) -> String {
        let header: [String: String] = [
            "alg": "ES256",
            "typ": "JWT",
            "kid": "did:example:holder#key-1"
        ]
        
        let iat = Int(Date().timeIntervalSince1970)
        let exp = iat + 2592000
        let jti = UUID().uuidString
        
        let payload: [String: Any] = [
            "iss": "did:example:holder",
            "aud": aud,
            "nonce": nonce,
            "iat": iat,
            "exp": exp,
            "jti": jti,
            "vp": [
                "context": ["https://www.w3.org/ns/credentials/v2"],
                "type": ["VerifiablePresentation"],
                "proof": [
                    "type": "DataIntegrityProof",
                    "cryptosuite": "ecdsa-rdfc-2019",
                    "created": ISO8601DateFormatter().string(from: Date()),
                    "proofPurpose": "authentication",
                    "verificationMethod": "did:example:holder#key-1",
                    "challenge": nonce,
                    "domain": aud,
                    "proofValue": "zQeVbY4oQowNiQoClz9Qg8X6PpuKy4tP9t8rKHHB3P4..."
                ],
                "verifiableCredential": [vcCredential]
            ]
        ]
        
        if let headerData = try? JSONSerialization.data(withJSONObject: header),
           let payloadData = try? JSONSerialization.data(withJSONObject: payload) {
            let headerB64 = Base64URL.encode(headerData)
            let payloadB64 = Base64URL.encode(payloadData)
            let vpToken = "\(headerB64).\(payloadB64)"
            LogUtil.logLongString("sangjun", "vpToken: \(vpToken)")
            return vpToken
        }
        
        return ""
    }

    /// Extracts claims from an OpenDID Verifiable Credential.
    /// - Parameter vcCredential: The Verifiable Credential data in Base64 format.
    /// - Returns: A list of claims extracted from the credential.
    public static func getClaims(vcCredential: String) -> [[String: Any]] {
        guard let decodedData = Data(base64Encoded: vcCredential),
              let json = try? JSONSerialization.jsonObject(with: decodedData) as? [String: Any],
              let credentialSubject = json["credentialSubject"] as? [String: Any],
              let claims = credentialSubject["claims"] as? [[String: Any]] else {
            return []
        }
        return claims
    }
}
