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
