import Foundation

public struct BleConstants {
    public static let serviceUUID = "0000fff0-0000-1000-8000-00805f9b34fb"
    public static let characteristicStateUUID = "00000001-a123-48ce-896b-4c76973373e6"
    public static let characteristicClient2ServerUUID = "00000002-a123-48ce-896b-4c76973373e6"
    public static let characteristicServer2ClientUUID = "00000003-a123-48ce-896b-4c76973373e6"
    public static let characteristicIdentUUID = "00000004-a123-48ce-896b-4c76973373e6"
    
    public static let stateStart: UInt8 = 0x01
    public static let stateEnd: UInt8 = 0x02
}
