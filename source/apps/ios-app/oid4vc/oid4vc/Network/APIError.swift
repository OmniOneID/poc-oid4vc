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

enum APIError: Error, LocalizedError {
    case badURL
    case requestFailed(Error)
    case jsonEncodingFailed(Error)
    case badServerResponse(statusCode: Int)
    case decodingError
    case serverError(details: ErrorResponse)
    case serverErrorString(body: String)
    
    var errorDescription: String? {
        switch self {
        case .badURL:
            return "Invalid URL"
        case .requestFailed(let error):
            return "The network request failed: \(error.localizedDescription)"
        case .jsonEncodingFailed(let error):
            return "Failed to encode the request body: \(error.localizedDescription)"
        case .badServerResponse(let statusCode):
            return "Server returned an error with status code: \(statusCode)"
        case .decodingError:
            return "Failed to decode the server response."
        case .serverError(let details):
            return "Server Error: \(details.error) - \(details.errorDescription)"
        case .serverErrorString(let body):
            return "Server Error: \(body)"
        }
    }
}