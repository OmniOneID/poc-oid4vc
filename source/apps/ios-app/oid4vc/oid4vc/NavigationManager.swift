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
import Combine

class NavigationManager: ObservableObject {
    
    enum Destination: Hashable {
        case issuance(uri: String)
        case verification(uri: String)
    }
    
    @Published var path: [Destination] = []
    
    func handle(url: URL) {
        if url.scheme == "openid-credential-offer" {
            path.append(.issuance(uri: url.absoluteString))
        } else if url.scheme == "openid4vp" {
            path.append(.verification(uri: url.absoluteString))
        }
    }
}
