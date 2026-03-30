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
import CryptoKit

/// Holds an ephemeral P-256 key pair for mDoc session security.
public struct EphemeralKeyHolder {
    /// The private key for the session.
    public let privateKey: P256.KeyAgreement.PrivateKey
    
    /// The public key bytes in uncompressed format (65 bytes: 0x04 + X + Y).
    public let eDeviceKeyBytes: [UInt8]
    
    public init() {
        let privateKey = P256.KeyAgreement.PrivateKey()
        self.privateKey = privateKey
        // x963Representation is uncompressed (0x04 || X || Y)
        self.eDeviceKeyBytes = [UInt8](privateKey.publicKey.x963Representation)
    }
}
