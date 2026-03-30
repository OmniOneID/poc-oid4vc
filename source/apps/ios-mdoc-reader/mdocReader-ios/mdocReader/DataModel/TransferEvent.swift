import Foundation

public enum TransferEvent {
    case connecting
    case connected
    case deviceEngagementCompleted
    case requestSent
    case responseReceived(data: Data)
    case disconnected
    case error(message: String)
    
    public protocol Listener: AnyObject {
        func onEvent(_ event: TransferEvent)
    }
}
