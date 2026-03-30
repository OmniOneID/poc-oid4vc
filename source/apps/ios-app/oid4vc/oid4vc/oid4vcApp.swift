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

import SwiftUI

/// The main application structure for the OID4VC app.
@main
struct oid4vcApp: App {
    @StateObject private var navigationManager = NavigationManager()

    /// The user interface body of the application.
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(navigationManager)
                .onOpenURL { url in
                    navigationManager.handle(url: url)
                }
        }
    }
}