import Foundation

public enum TransferStatus {
    case initializing
    case connecting
    case connected
    case deviceEngagementCompleted
    case requestSent
    case responseReceived([ReceivedDocument])
    case disconnected
    case error(String)
}
