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

//MARK: - Jsonable
/// Model to Json, and vice versa
public protocol Jsonable : Codable
{
    init(from jsonData: Data) throws
    init(from jsonString: String) throws
    func toJsonData(isPretty: Bool) throws -> Data
    func toJson(isPretty: Bool) throws -> String
}

public extension Jsonable
{
    init(from jsonData: Data) throws {
        let decoder = JSONDecoder()
        self = try decoder.decode(Self.self, from: jsonData)
    }
    
    init(from jsonString: String) throws {
        let data = jsonString.data(using: .utf8)!
        try self.init(from: data)
    }
    
    func toJsonData(isPretty: Bool = false) throws -> Data
    {
        var formatting : JSONEncoder.OutputFormatting = [.sortedKeys, .withoutEscapingSlashes]
        if isPretty
        {
            formatting.insert(.prettyPrinted)
        }
        
        let jsonEncoder = JSONEncoder()
        jsonEncoder.outputFormatting = formatting
        
        let data = try jsonEncoder.encode(self)
        return data
    }
    
    func toJson(isPretty: Bool = false) throws -> String {
        return String(data: try toJsonData(isPretty: isPretty), encoding: .utf8)!
    }
}
