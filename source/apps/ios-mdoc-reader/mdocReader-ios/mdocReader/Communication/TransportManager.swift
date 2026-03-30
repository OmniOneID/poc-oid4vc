import Foundation

public protocol TransportManager: AnyObject {
    func addListener(_ listener: TransferEvent.Listener)
    func removeListener(_ listener: TransferEvent.Listener)
    func startDeviceEngagement(source: EngagementSource)
    func sendRequest(deviceRequestBytes: Data)
    func getSessionEncryption() -> SessionEncryption?
    func stopSession()
}

public struct TransportConfig {
    public init() {}
}
