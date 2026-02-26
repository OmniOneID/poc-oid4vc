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

protocol Signer {
    var algorithm: String { get }
    func sign(data: Data) throws -> Data
    func getPublicKeyJwk() -> [String: Any]?
}

class HolderSigner: Signer {
    private var holderPrivateKey: P256.Signing.PrivateKey

    init(pkcs8PrivateKeyBase64: String) throws {
        guard let privateKeyData = Data(base64Encoded: pkcs8PrivateKeyBase64) else {
            throw "Invalid Base64 for PKCS#8 key"
        }
        self.holderPrivateKey = try P256.Signing.PrivateKey(derRepresentation: privateKeyData)
    }
    
    var algorithm: String { "ES256" }

    func sign(data: Data) throws -> Data {
        let signature = try holderPrivateKey.signature(for: data)
        return signature.rawRepresentation
    }

    func getPublicKeyJwk() -> [String : Any]? {
        let publicKey = holderPrivateKey.publicKey
        let x963Data = publicKey.x963Representation
        let x = x963Data.subdata(in: 1..<33)
        let y = x963Data.subdata(in: 33..<65)
        
        let jwk: [String: Any] = [
            "kty": "EC",
            "crv": "P-256",
            "x": x.base64URLEncodedString(),
            "y": y.base64URLEncodedString()
        ]
        return jwk
    }
}
