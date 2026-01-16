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

// MARK: - SD-JWT minimal models/utilities (used by CredentialMatcher)

public struct SDJWT : Jsonable {
    public let credentialJwt: String
    public let disclosures: [Disclosure]
    public var keyBindingJwt: String?
    
    public init(credentialJwt: String, disclosures: [Disclosure] = [], keyBindingJwt: String? = nil) {
        self.credentialJwt = credentialJwt
        self.disclosures   = disclosures
        self.keyBindingJwt = keyBindingJwt
    }
    
    public static func parse(raw: String) -> SDJWT
    {
        // SD-JWT is formatted as: credentialJwt~disclosure1~disclosure2~...~[keyBindingJwt]
        // Split by '~' preserving order
        let parts = raw.components(separatedBy: "~")
        guard !parts.isEmpty else {
            return SDJWT(credentialJwt: raw, disclosures: [], keyBindingJwt: nil)
        }

        // First segment is the credential JWT
        let credentialJwt = parts[0]

        // Determine if there is a keyBindingJwt by counting dots in the entire raw string
        // Typical JWT has 2 dots. If there are 4 dots overall, then a key binding JWT exists at the end.
        let hasKeyBinding = raw.getCount(of: ".") >= 4

        // Disclosures are any middle segments that are valid disclosure encodings
        let startIndexForDisclosures = 1
        let endIndexForDisclosuresExclusive = hasKeyBinding ? (parts.count - 1) : parts.count
        let disclosureSegments: [String]
        if startIndexForDisclosures < endIndexForDisclosuresExclusive {
            disclosureSegments = Array(parts[startIndexForDisclosures..<endIndexForDisclosuresExclusive])
        } else {
            disclosureSegments = []
        }

        let disclosures = disclosureSegments.compactMap { Disclosure.parse(raw: $0) }

        // Optional key binding JWT is the last segment if present
        let keyBindingJwt: String? = hasKeyBinding ? parts.last : nil

        return SDJWT(credentialJwt: credentialJwt, disclosures: disclosures, keyBindingJwt: keyBindingJwt)
    }
    
    public func toString() -> String {
        
        var jwt = credentialJwt
        
        for disclosure in disclosures {
            jwt.append("~\(disclosure.getDisclosure())")
        }
        
        jwt.append("~")
        
        if let keyBinding = keyBindingJwt
        {
            jwt.append(keyBinding)
        }
        
        return jwt
    }
}

extension String
{
    func getCount(of word: String) -> Int
    {
        guard !word.isEmpty else { return 0 }

        var count = 0
        var searchRange: Range<String.Index>? = startIndex..<endIndex

        while let range = self.range(of: word, range: searchRange) {
            count += 1
            searchRange = range.upperBound..<endIndex
        }

        return count
    }
}

public struct Disclosure: Codable, Equatable {
    public let salt: String
    public let claimName: String?
    public let claimValue: JSON

    public init(salt: String, claimName: String?, claimValue: JSON) {
        self.salt = salt
        self.claimName = claimName
        self.claimValue = claimValue
    }

    // JSON array payload (sorted keys for stable digest)
    func toData() -> Data {
        var arr: [JSON] = [.string(salt)]
        if let claimName = claimName {
            arr.append(.string(claimName))
        }
        arr.append(claimValue)

        let encoder = RNJSONEncoder()
        return (try? encoder.encode(arr)) ?? Data()
    }

    public func getDisclosure() -> String {
        self.toData().base64URLEncodedString()
    }

    public func digest() -> String {
        let hash = SHA256.hash(data: self.toData())
        return Base64URL.encode(Data(hash))
    }

    /// Parse SD-JWT disclosure (base64url JSON array)
    /// Typical forms:
    /// - ["salt","claimName", claimValue]
    /// - ["salt", claimValue]  (array item disclosure)
    public static func parse(raw: String) -> Disclosure? {
        guard let decoded = Base64URL.decode(raw) else { return nil }

        let top: JSON
        do {
            top = try JSONParser().parse(data: decoded)
        } catch {
            return nil
        }

        guard case let .array(items) = top, items.count >= 2 else { return nil }

        guard case let .string(salt) = items[0] else { return nil }

        if items.count == 3 {
            guard case let .string(claimName) = items[1] else { return nil }
            let claimValue = items[2]
            return Disclosure(salt: salt, claimName: claimName, claimValue: claimValue)
        }

        if items.count == 2 {
            let claimValue = items[1]
            return Disclosure(salt: salt, claimName: nil, claimValue: claimValue)
        }

        return nil
    }
}
