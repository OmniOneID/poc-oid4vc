//
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
import CryptoKit

public class CryptoUtil {
    public static func getPrivateKeyFromBase64(_ base64: String) -> P256.Signing.PrivateKey? {
        guard let data = Data(base64Encoded: base64) else { return nil }
        return try? P256.Signing.PrivateKey(derRepresentation: data)
    }

    public static func readCertFromAssets(name: String) -> [String] {
        if let path = Bundle.main.path(forResource: name, ofType: nil) {
            if let content = try? String(contentsOfFile: path) {
                // Return certificate chain, for simplicity we return the content in list
                return [content]
            }
        }
        return []
    }
}

extension Data {
    func base64URLEncodedString() -> String {
        return self.base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}

public struct Base64URL {
    public static func encode(_ data: Data) -> String {
        return data.base64URLEncodedString()
    }

    public static func decode(_ string: String) -> Data? {
        var base64 = string
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        
        let padding = base64.count % 4
        if padding > 0 {
            base64 += String(repeating: "=", count: 4 - padding)
        }
        
        return Data(base64Encoded: base64)
    }
}
