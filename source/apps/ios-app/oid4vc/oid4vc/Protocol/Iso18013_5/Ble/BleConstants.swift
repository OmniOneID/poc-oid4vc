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
import CoreBluetooth

/// Constants for ISO 18013-5 BLE communication.
/// Updated to match the specific implementation used in the Android project.
public struct BleConstants {
    /// Service UUID: 0000fff0-0000-1000-8000-00805f9b34fb
    public static let SERVICE_UUID = CBUUID(string: "0000fff0-0000-1000-8000-00805f9b34fb")
    
    /// State characteristic UUID: 00000001-a123-48ce-896b-4c76973373e6
    public static let CHARACTERISTIC_STATE_UUID = CBUUID(string: "00000001-a123-48ce-896b-4c76973373e6")
    
    /// Client2Server characteristic UUID: 00000002-a123-48ce-896b-4c76973373e6
    public static let CHARACTERISTIC_CLIENT_2_SERVER_UUID = CBUUID(string: "00000002-a123-48ce-896b-4c76973373e6")
    
    /// Server2Client characteristic UUID: 00000003-a123-48ce-896b-4c76973373e6
    public static let CHARACTERISTIC_SERVER_2_CLIENT_UUID = CBUUID(string: "00000003-a123-48ce-896b-4c76973373e6")
    
    /// Ident characteristic UUID: 00000004-a123-48ce-896b-4c76973373e6
    public static let CHARACTERISTIC_IDENT_UUID = CBUUID(string: "00000004-a123-48ce-896b-4c76973373e6")
    
    /// State: Start (Reader ready to receive request)
    public static let STATE_START: UInt8 = 0x01
    
    /// State: End (Session terminated)
    public static let STATE_END: UInt8 = 0x02
    
    /// Client Characteristic Configuration Descriptor (standard)
    public static let DESC_CLIENT_CHAR_CONFIG = CBUUID(string: "2902")
}
