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

public class LogUtil {
    public static func d(_ tag: String, _ message: String) {
        print("[\(tag)] \(message)")
    }
    
    public static func e(_ tag: String, _ message: String) {
        print("[\(tag)] ERROR: \(message)")
    }
    
    public static func logLongString(_ tag: String, _ message: String) {
        print("[\(tag)] \(message)")
    }
    
    /**
     * Extracts a specific field from a JSON response string.
     */
    public static func extractJsonField(_ jsonResponse: String, _ fieldName: String) -> String? {
        guard let data = jsonResponse.data(using: .utf8) else { return nil }
        do {
            if let json = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any] {
                if let value = json[fieldName] {
                    if let strValue = value as? String {
                        return strValue
                    } else if let dictValue = value as? [String: Any] {
                        if let jsonData = try? JSONSerialization.data(withJSONObject: dictValue, options: []),
                           let jsonString = String(data: jsonData, encoding: .utf8) {
                            return jsonString
                        }
                    } else if let numValue = value as? NSNumber {
                        return numValue.stringValue
                    } else if let boolValue = value as? Bool {
                        return String(boolValue)
                    } else {
                        return String(describing: value)
                    }
                }
            }
        } catch {
            return nil
        }
        return nil
    }
}
