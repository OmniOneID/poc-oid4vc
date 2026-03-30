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
import Combine

/// A manager that handles navigation within the application.
class NavigationManager: ObservableObject {
    
    /// The potential navigation destinations.
    enum Destination: Hashable {
        case issuance(uri: String)
        case verification(uri: String, selectedClaimsKeys: [String]?, selectedClaimsNamespaces: [String]?)
        case mdocOffline(mDoc: String, selectedKeys: [String], namespaces: [String])
        case viewVc
    }
    
    /// The current navigation path.
    @Published var path: [Destination] = []
    
    /// Handles an incoming URL and updates the navigation path.
    /// - Parameter url: The URL to be handled.
    func handle(url: URL) {
        if url.scheme == "openid-credential-offer" {
            path.append(.issuance(uri: url.absoluteString))
        } else if url.scheme == "openid4vp" {
            path.append(.verification(uri: url.absoluteString, selectedClaimsKeys: nil, selectedClaimsNamespaces: nil))
        }
    }
}
