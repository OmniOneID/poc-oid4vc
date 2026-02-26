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

// MARK: - AnyJSON (lossless JSON value container)

public enum AnyJSON: Codable, Equatable, Hashable {
    case null
    case bool(Bool)
    case number(Double)          // JSON numbers; keep as Double
    case string(String)
    case array([AnyJSON])
    case object([String: AnyJSON])

    // Convenience accessors
    public var asBool: Bool? { if case .bool(let v) = self { return v } else { return nil } }
    public var asDouble: Double? { if case .number(let v) = self { return v } else { return nil } }
    public var asString: String? { if case .string(let v) = self { return v } else { return nil } }
    public var asArray: [AnyJSON]? { if case .array(let v) = self { return v } else { return nil } }
    public var asObject: [String: AnyJSON]? { if case .object(let v) = self { return v } else { return nil } }

    // Convert to Foundation types for JSONSerialization / path navigation
    public func toFoundation() -> Any {
        switch self {
        case .null: return NSNull()
        case .bool(let b): return b
        case .number(let n): return n
        case .string(let s): return s
        case .array(let a): return a.map { $0.toFoundation() }
        case .object(let o): return o.mapValues { $0.toFoundation() }
        }
    }

    // Create from Foundation JSON types
    public static func fromFoundation(_ value: Any) -> AnyJSON? {
        if value is NSNull { return .null }
        if let b = value as? Bool { return .bool(b) }
        if let n = value as? NSNumber {
            // NSNumber can represent Bool too; make sure we didn't already catch Bool above
            return .number(n.doubleValue)
        }
        if let s = value as? String { return .string(s) }
        if let a = value as? [Any] {
            return .array(a.compactMap { AnyJSON.fromFoundation($0) })
        }
        if let o = value as? [String: Any] {
            var dict: [String: AnyJSON] = [:]
            for (k, v) in o {
                if let j = AnyJSON.fromFoundation(v) {
                    dict[k] = j
                } else {
                    // Non-JSON value -> drop as null (shouldn't happen with JSONSerialization)
                    dict[k] = .null
                }
            }
            return .object(dict)
        }
        return nil
    }

    // MARK: Codable

    public init(from decoder: Decoder) throws {
        let c = try decoder.singleValueContainer()

        if c.decodeNil() {
            self = .null
            return
        }

        if let b = try? c.decode(Bool.self) {
            self = .bool(b)
            return
        }

        // Decode numbers as Double. This preserves JSON numeric semantics.
        if let n = try? c.decode(Double.self) {
            self = .number(n)
            return
        }

        if let s = try? c.decode(String.self) {
            self = .string(s)
            return
        }

        if let a = try? c.decode([AnyJSON].self) {
            self = .array(a)
            return
        }

        if let o = try? c.decode([String: AnyJSON].self) {
            self = .object(o)
            return
        }

        throw DecodingError.typeMismatch(
            AnyJSON.self,
            .init(codingPath: decoder.codingPath, debugDescription: "Unsupported JSON value")
        )
    }

    public func encode(to encoder: Encoder) throws {
        var c = encoder.singleValueContainer()

        switch self {
        case .null:
            try c.encodeNil()
        case .bool(let b):
            try c.encode(b)
        case .number(let n):
            try c.encode(n)
        case .string(let s):
            try c.encode(s)
        case .array(let a):
            try c.encode(a)
        case .object(let o):
            try c.encode(o)
        }
    }

    // MARK: Hashable (canonical JSON)

    public func hash(into hasher: inout Hasher) {
        // Stable canonical JSON ensures hash stability for object key ordering etc.
        hasher.combine(canonicalJSONString())
    }

    private func canonicalJSONString() -> String {
        // Encode as canonical JSON (sorted keys for objects).
        // Not the fastest, but robust and deterministic for hashing/equality validation use-cases.
        switch self {
        case .null:
            return "null"
        case .bool(let b):
            return b ? "true" : "false"
        case .number(let n):
            // Use JSON-style minimal representation where possible.
            // Avoid scientific notation differences by using a normalized string.
            if n.isNaN || n.isInfinite { return "null" } // JSON can't represent these
            // Strip trailing .0 if integer-like
            if floor(n) == n { return String(format: "%.0f", n) }
            // Use a reasonable precision
            return String(n)
        case .string(let s):
            return AnyJSON.escapeJSONString(s)
        case .array(let a):
            return "[" + a.map { $0.canonicalJSONString() }.joined(separator: ",") + "]"
        case .object(let o):
            let keys = o.keys.sorted()
            let parts = keys.map { key -> String in
                let k = AnyJSON.escapeJSONString(key)
                let v = o[key]?.canonicalJSONString() ?? "null"
                return "\(k):\(v)"
            }
            return "{" + parts.joined(separator: ",") + "}"
        }
    }

    private static func escapeJSONString(_ s: String) -> String {
        // Minimal JSON string escaping
        var out = "\""
        for ch in s.unicodeScalars {
            switch ch.value {
            case 0x22: out += "\\\""        // "
            case 0x5C: out += "\\\\"        // \
            case 0x08: out += "\\b"
            case 0x0C: out += "\\f"
            case 0x0A: out += "\\n"
            case 0x0D: out += "\\r"
            case 0x09: out += "\\t"
            default:
                if ch.value < 0x20 {
                    out += String(format: "\\u%04X", ch.value)
                } else {
                    out += String(ch)
                }
            }
        }
        out += "\""
        return out
    }
}

// MARK: - DCQL Path Element

public enum DCQLPathElement: Codable, Hashable {
    case key(String)
    case index(Int)
    case wildcard   // corresponds to null in Java path list

    public init(from decoder: Decoder) throws {
        let c = try decoder.singleValueContainer()
        if c.decodeNil() { self = .wildcard; return }
        if let i = try? c.decode(Int.self) { self = .index(i); return }
        if let s = try? c.decode(String.self) { self = .key(s); return }
        throw DecodingError.typeMismatch(
            DCQLPathElement.self,
            .init(codingPath: decoder.codingPath, debugDescription: "Path element must be string, int, or null")
        )
    }

    public func encode(to encoder: Encoder) throws {
        var c = encoder.singleValueContainer()
        switch self {
        case .wildcard: try c.encodeNil()
        case .index(let i): try c.encode(i)
        case .key(let s): try c.encode(s)
        }
    }
}



public enum DCQLCredentialMatcher {

    public static func matchesFormat(requiredFormat: String?) -> Bool {
        guard let requiredFormat = requiredFormat else { return true }
        let supported: Set<String> = ["dc+sd-jwt", "vc+sd-jwt"]
        return supported.contains(requiredFormat)
    }

    public static func matchesMetadata(sdjwt: SDJWT, metadata: [String: AnyJSON]?) -> Bool {
        guard let metadata = metadata, !metadata.isEmpty else { return true }

        if let v = metadata["vct_values"] {
            // vct_values must be array of strings
            guard let required = v.asArray?.compactMap({ $0.asString }) else { return false }
            return checkVctValues(sdjwt: sdjwt, requiredVcts: required)
        }

        return true
    }

    private static func checkVctValues(sdjwt: SDJWT, requiredVcts: [String]?) -> Bool {
        guard let requiredVcts = requiredVcts, !requiredVcts.isEmpty else { return true }
        do {
            let jwt = try SimpleJWTDecoder.parse(sdjwt.credentialJwt)
            guard let vct = jwt.payload["vct"] else { 
                print("❌ 'vct' claim missing in JWT payload")
                return false 
            }
            let actual = String(describing: vct)
            print("--- checkVctValues ---")
            print("Required VCTs: \(requiredVcts)")
            print("Actual VCT: \(actual)")
            let isContained = requiredVcts.contains(actual)
            print("Match result: \(isContained)")
            print("-----------------------")
            return isContained
        } catch {
            print("❌ SimpleJWTDecoder failed: \(error)")
            return false
        }
    }

    public static func extractMatchingClaimNames(dcqlQuery: DCQLQuery?, sdjwt: SDJWT?) -> Set<String> {
        guard let dcqlQuery = dcqlQuery, let creds = dcqlQuery.credentials, let sdjwt = sdjwt else {
            return []
        }

        var matching: Set<String> = []
        let allClaims = extractAllCredentialClaims(sdjwt: sdjwt)

        for credential in creds {
            if credential.claims == nil {
                matching.formUnion(allClaims.keys)
                continue
            }

            for claimQuery in credential.claims ?? [] {
                guard let path = claimQuery.path, !path.isEmpty else { continue }
                processPathAndCollectClaims(allClaims: allClaims,
                                           path: path,
                                           claimQuery: claimQuery,
                                           matchingClaims: &matching)
            }
        }

        return matching
    }

    private static func extractAllCredentialClaims(sdjwt: SDJWT) -> [String: Any] {
        var allClaims: [String: Any] = [:]

        if let jwt = try? SimpleJWTDecoder.parse(sdjwt.credentialJwt) {
            for (k, v) in jwt.payload where !isReservedJWTClaim(k) {
                allClaims[k] = v
            }
        }

        if !sdjwt.disclosures.isEmpty {
            for d in sdjwt.disclosures {
                if let name = d.claimName {
                    allClaims[name] = d.claimValue as Any
                }
            }
            integrateDisclosuresIntoCredential(allClaims: &allClaims, sdjwt: sdjwt)
        }

        return allClaims
    }

    private static func integrateDisclosuresIntoCredential(allClaims: inout [String: Any], sdjwt: SDJWT) {
        var digestToDisclosure: [String: Disclosure] = [:]
        for d in sdjwt.disclosures {
            digestToDisclosure[d.digest()] = d
        }

        let snapshot = allClaims
        for (claimName, claimValue) in snapshot {
            if var obj = claimValue as? [String: Any] {
                integrateDisclosuresIntoObject(parentName: claimName,
                                              parentObject: &obj,
                                              digestToDisclosure: digestToDisclosure)
                allClaims[claimName] = obj
            }
        }
    }

    private static func integrateDisclosuresIntoObject(parentName: String,
                                                       parentObject: inout [String: Any],
                                                       digestToDisclosure: [String: Disclosure]) {
        guard let sd = parentObject["_sd"] as? [Any] else { return }

        for item in sd {
            guard let digest = item as? String else { continue }
            guard let disclosure = digestToDisclosure[digest], let claimName = disclosure.claimName else { continue }

            let claimValue = disclosure.claimValue as Any
            parentObject[claimName] = claimValue

            if var nested = claimValue as? [String: Any] {
                integrateDisclosuresIntoObject(parentName: parentName + "." + claimName,
                                              parentObject: &nested,
                                              digestToDisclosure: digestToDisclosure)
                parentObject[claimName] = nested
            }
        }
    }

    private static func processPathAndCollectClaims(allClaims: [String: Any],
                                                    path: [DCQLPathElement],
                                                    claimQuery: DCQLQuery.ClaimQuery,
                                                    matchingClaims: inout Set<String>) {

        guard case .key(let topLevel) = path[0] else { return }
        guard let rootValue = allClaims[topLevel] else { return }

        if path.count == 1 {
            if meetsClaimConditions(claimQuery: claimQuery, actualValue: rootValue) {
                matchingClaims.insert(topLevel)
            }
            return
        }

        let remaining = Array(path.dropFirst())
        collectMatchingValuesFromPath(current: rootValue,
                                      remainingPath: remaining,
                                      claimQuery: claimQuery,
                                      claimNamePrefix: topLevel,
                                      matchingClaims: &matchingClaims)
    }

    private static func collectMatchingValuesFromPath(current: Any,
                                                      remainingPath: [DCQLPathElement],
                                                      claimQuery: DCQLQuery.ClaimQuery,
                                                      claimNamePrefix: String,
                                                      matchingClaims: inout Set<String>) {
        if remainingPath.isEmpty {
            if meetsClaimConditions(claimQuery: claimQuery, actualValue: current) {
                matchingClaims.insert(claimNamePrefix)
            }
            return
        }

        let next = remainingPath[0]
        let nextRemaining = Array(remainingPath.dropFirst())

        switch next {
        case .wildcard:
            guard let list = current as? [Any] else { return }
            for i in 0..<list.count {
                let newName = "\(claimNamePrefix)[\(i)]"
                collectMatchingValuesFromPath(current: list[i],
                                              remainingPath: nextRemaining,
                                              claimQuery: claimQuery,
                                              claimNamePrefix: newName,
                                              matchingClaims: &matchingClaims)
            }

        case .index(let idx):
            guard let list = current as? [Any] else { return }
            guard idx >= 0, idx < list.count else { return }
            let newName = "\(claimNamePrefix)[\(idx)]"
            collectMatchingValuesFromPath(current: list[idx],
                                          remainingPath: nextRemaining,
                                          claimQuery: claimQuery,
                                          claimNamePrefix: newName,
                                          matchingClaims: &matchingClaims)

        case .key(let key):
            guard let dict = current as? [String: Any] else { return }
            guard let nextValue = dict[key] else { return }
            let newName = "\(claimNamePrefix).\(key)"
            collectMatchingValuesFromPath(current: nextValue,
                                          remainingPath: nextRemaining,
                                          claimQuery: claimQuery,
                                          claimNamePrefix: newName,
                                          matchingClaims: &matchingClaims)
        }
    }

    private static func meetsClaimConditions(claimQuery: DCQLQuery.ClaimQuery, actualValue: Any) -> Bool {
        // Convert actualValue to AnyJSON for robust comparisons
        let actualJSON = AnyJSON.fromFoundation(actualValue) ?? .null

        if let values = claimQuery.values, !values.isEmpty {
            if !values.contains(actualJSON) { return false }
        }

        if let v = claimQuery.value {
            if actualJSON != v { return false }
        }

        if let min = claimQuery.min, !checkMinCondition(actualValue: actualJSON, minValue: min) {
            return false
        }
        if let max = claimQuery.max, !checkMaxCondition(actualValue: actualJSON, maxValue: max) {
            return false
        }

        return true
    }

    private static func checkMinCondition(actualValue: AnyJSON, minValue: AnyJSON) -> Bool {
        switch (actualValue, minValue) {
        case (.number(let a), .number(let m)):
            return a >= m
        case (.string(let a), .string(let m)):
            return a.compare(m) != .orderedAscending
        default:
            return true
        }
    }

    private static func checkMaxCondition(actualValue: AnyJSON, maxValue: AnyJSON) -> Bool {
        switch (actualValue, maxValue) {
        case (.number(let a), .number(let m)):
            return a <= m
        case (.string(let a), .string(let m)):
            return a.compare(m) != .orderedDescending
        default:
            return true
        }
    }

    private static func isReservedJWTClaim(_ claimName: String) -> Bool {
        let reserved: Set<String> = ["iss","sub","aud","exp","nbf","iat","jti","_sd_alg","_sd","cnf","vct"]
        return reserved.contains(claimName)
    }
}

