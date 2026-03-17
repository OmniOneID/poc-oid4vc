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

/// An extension of SDJWT to provide format-specific logic for Selective Disclosure.
extension SDJWT {
    
    /// The private key used for signing (placeholder for actual key management).
    private static let PRIVATE_KEY = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT"

    /// Checks if the given format is supported for SD-JWT.
    /// - Parameter format: The format string to check.
    /// - Returns: True if supported, false otherwise.
    public static func isSupported(format: String) -> Bool {
        return format == "NationalID" || format == "NationalIDCert"
    }

    /// Creates a selectively disclosed VP token based on selected claims.
    /// - Parameters:
    ///   - sdJwtVc: The raw SD-JWT Verifiable Credential.
    ///   - selectedClaimsKeys: The keys selected for disclosure.
    ///   - aud: The audience (verifier's client ID).
    ///   - nonce: The nonce for fresh proof.
    /// - Returns: The final VP Token string.
    public static func createVpToken(sdJwtVc: String, selectedClaimsKeys: [String]?, aud: String, nonce: String) throws -> String {
        let pkcs8PrivateKey = SDJWT.PRIVATE_KEY
        let signer = try HolderSigner(pkcs8PrivateKeyBase64: pkcs8PrivateKey)
        
        let parsedVC = SDJWT.parse(raw: sdJwtVc)
        
        var requestedClaims: Set<String>
        if let selectedKeys = selectedClaimsKeys, !selectedKeys.isEmpty {
            requestedClaims = Set(selectedKeys)
            requestedClaims.remove("Issuer")
            requestedClaims.remove("Subject")
            requestedClaims.remove("vct")
            requestedClaims.remove("Format")
        } else {
            requestedClaims = ["family_name", "given_name", "phone_number", "birth_date", "email"]
        }
        
        let selectiveDisclosure = parsedVC.disclosures.filter { disclosure in
            if let name = disclosure.claimName {
                return requestedClaims.contains(name)
            }
            return false
        }
        
        let header: [String: Any] = [
            "alg": signer.algorithm,
            "typ": "kb+jwt",
            "jwk": signer.getPublicKeyJwk() as Any
        ]
        let headerData = try JSONSerialization.data(withJSONObject: header)
        let headerBase64Url = headerData.base64URLEncodedString()
        
        guard let sdJwtData = sdJwtVc.data(using: .utf8) else {
            throw "Failed to convert SD-JWT string to data"
        }
        let sdHash = SHA256.hash(data: sdJwtData)
        let sdHashBase64Url = Data(sdHash).base64URLEncodedString()

        let payload: [String: Any] = [
            "nonce": nonce,
            "aud": aud,
            "iat": Int(Date().timeIntervalSince1970),
            "sd_hash": sdHashBase64Url
        ]
        let payloadData = try JSONSerialization.data(withJSONObject: payload)
        let payloadBase64Url = payloadData.base64URLEncodedString()
        
        let signingInput = "\(headerBase64Url).\(payloadBase64Url)"
        guard let signingInputData = signingInput.data(using: .utf8) else {
            throw "Failed to create signing input data"
        }
        let signature = try signer.sign(data: signingInputData)
        let signatureBase64Url = signature.base64URLEncodedString()
        
        let keyBindingJwt = "\(signingInput).\(signatureBase64Url)"
        
        let keyBindedJWT = SDJWT(credentialJwt: parsedVC.credentialJwt,
                                 disclosures: selectiveDisclosure,
                                 keyBindingJwt: keyBindingJwt)
        
        let vpToken = keyBindedJWT.toString()
        
        let dcqlId = "national_id"
        let vpJsonObject: [String: [String]] = [dcqlId : [vpToken]]
        let vpJsonData = try JSONSerialization.data(withJSONObject: vpJsonObject, options: [])
        guard let finalVpJsonString = String(data: vpJsonData, encoding: .utf8) else {
            throw "Failed to create final VP JSON string"
        }
        
        LogUtil.logLongString("sangjun", "vpToken: \(finalVpJsonString)")
        return finalVpJsonString
    }

    /// Extracts claims from an SD-JWT Verifiable Credential.
    /// - Parameter sdJwtVc: The raw SD-JWT string.
    /// - Returns: A dictionary of extracted claims.
    public static func getClaims(sdJwtVc: String) -> [String: Any] {
        var claims: [String: Any] = [:]
        let parsedVC = SDJWT.parse(raw: sdJwtVc)
        
        for disclosure in parsedVC.disclosures {
            if let name = disclosure.claimName {
                claims[name] = SDJWT.jsonToAny(disclosure.claimValue)
            }
        }
        
        return claims
    }
}
