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
import CoreBluetooth

public struct EngagementSource {
    public let deviceEngagementBytes: Data
    public let devicePublicKeyRaw: Data // 65 bytes (0x04 + X + Y)
    public let bleServiceUUID: CBUUID?
    
    public init(deviceEngagementBytes: Data, devicePublicKeyRaw: Data, bleServiceUUID: CBUUID? = nil) {
        self.deviceEngagementBytes = deviceEngagementBytes
        self.devicePublicKeyRaw = devicePublicKeyRaw
        self.bleServiceUUID = bleServiceUUID
    }
}
