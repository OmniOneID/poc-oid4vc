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

/// Protocol for a proximity server (BLE or NFC) in the ISO 18013-5 context.
public protocol MdocProximityServer {
    /// Sets the listener for server events.
    func setListener(_ listener: MdocProximityListener)
    
    /// Starts the server (e.g., starts BLE advertising).
    func start()
    
    /// Stops the server.
    func stop()
}
