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

/// A structure representing an SD-JWT (Selective Disclosure JSON Web Token).
public struct SDJWT: Jsonable {
    public let credentialJwt: String
    public let disclosures: [Disclosure]
    public var keyBindingJwt: String?
    
    /// Initializes a new instance of SDJWT.
    public init(credentialJwt: String, disclosures: [Disclosure] = [], keyBindingJwt: String? = nil) {
        self.credentialJwt = credentialJwt
        self.disclosures   = disclosures
        self.keyBindingJwt = keyBindingJwt
    }
    
    /// Parses a raw SD-JWT string into an SDJWT structure.
    /// - Parameter raw: The raw SD-JWT string (with tildes).
    /// - Returns: An SDJWT instance.
    public static func parse(raw: String) -> SDJWT {
        let parts = raw.components(separatedBy: "~")
        guard !parts.isEmpty else {
            return SDJWT(credentialJwt: raw, disclosures: [], keyBindingJwt: nil)
        }

        let credentialJwt = parts[0]
        let hasKeyBinding = raw.getCount(of: ".") >= 4

        let startIndexForDisclosures = 1
        let endIndexForDisclosuresExclusive = hasKeyBinding ? (parts.count - 1) : parts.count
        let disclosureSegments: [String]
        if startIndexForDisclosures < endIndexForDisclosuresExclusive {
            disclosureSegments = Array(parts[startIndexForDisclosures..<endIndexForDisclosuresExclusive])
        } else {
            disclosureSegments = []
        }

        let disclosures = disclosureSegments.compactMap { Disclosure.parse(raw: $0) }
        let keyBindingJwt: String? = hasKeyBinding ? parts.last : nil

        return SDJWT(credentialJwt: credentialJwt, disclosures: disclosures, keyBindingJwt: keyBindingJwt)
    }
    
    /// Converts the SDJWT structure into its string representation.
    /// - Returns: The SD-JWT string.
    public func toString() -> String {
        var jwt = credentialJwt
        for disclosure in disclosures {
            jwt.append("~\(disclosure.getDisclosure())")
        }
        jwt.append("~")
        if let keyBinding = keyBindingJwt {
            jwt.append(keyBinding)
        }
        return jwt
    }
    
    /// Helper to convert JSON values to Swift types.
    internal static func jsonToAny(_ json: JSON) -> Any {
        switch json {
        case .string(let s): return s
        case .number(let n):
            if let i = Int(n) { return i }
            if let d = Double(n) { return d }
            return n
        case .bool(let b): return b
        case .object(let keyValues):
            var dict: [String: Any] = [:]
            for (key, value) in keyValues {
                dict[key] = jsonToAny(value)
            }
            return dict
        case .array(let array):
            return array.map { jsonToAny($0) }
        case .null:
            return NSNull()
        }
    }
}

/// A structure representing a disclosure within an SD-JWT.
public struct Disclosure: Codable, Equatable {
    public let salt: String
    public let claimName: String?
    public let claimValue: JSON

    /// Initializes a new instance of Disclosure.
    public init(salt: String, claimName: String?, claimValue: JSON) {
        self.salt = salt
        self.claimName = claimName
        self.claimValue = claimValue
    }

    /// Converts the disclosure into its raw data representation.
    /// - Returns: The data representation.
    func toData() -> Data {
        var arr: [JSON] = [.string(salt)]
        if let claimName = claimName {
            arr.append(.string(claimName))
        }
        arr.append(claimValue)

        let encoder = RNJSONEncoder()
        return (try? encoder.encode(arr)) ?? Data()
    }

    /// Returns the base64URL-encoded disclosure string.
    /// - Returns: The disclosure string.
    public func getDisclosure() -> String {
        self.toData().base64URLEncodedString()
    }

    /// Returns a base64-encoded digest of the disclosure.
    /// - Returns: The digest string.
    public func digest() -> String {
        let hash = SHA256.hash(data: self.toData())
        return Base64URL.encode(Data(hash))
    }

    /// Parses a raw disclosure string into a Disclosure structure.
    /// - Parameter raw: The base64URL-encoded disclosure string.
    /// - Returns: A Disclosure instance if parsing is successful.
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

/// An extension to provide utility methods for string manipulation.
extension String {
    /// Counts the occurrences of a specified substring.
    /// - Parameter word: The substring to count.
    /// - Returns: The number of occurrences.
    func getCount(of word: String) -> Int {
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
