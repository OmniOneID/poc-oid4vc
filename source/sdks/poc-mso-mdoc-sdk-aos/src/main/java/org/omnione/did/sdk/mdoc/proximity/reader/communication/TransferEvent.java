package org.omnione.did.sdk.mdoc.proximity.reader.communication;

public abstract class TransferEvent {
    private TransferEvent() {}

    public static final class Connecting extends TransferEvent {
        public static final Connecting INSTANCE = new Connecting();
        private Connecting() {}
    }

    public static final class Connected extends TransferEvent {
        public static final Connected INSTANCE = new Connected();
        private Connected() {}
    }

    public static final class DeviceEngagementCompleted extends TransferEvent {
        public static final DeviceEngagementCompleted INSTANCE = new DeviceEngagementCompleted();
        private DeviceEngagementCompleted() {}
    }

    public static final class RequestSent extends TransferEvent {
        public static final RequestSent INSTANCE = new RequestSent();
        private RequestSent() {}
    }

    public static final class ResponseReceived extends TransferEvent {
        private final byte[] data;
        public ResponseReceived(byte[] data) { this.data = data; }
        public byte[] getData() { return data; }
    }

    public static final class Error extends TransferEvent {
        private final Throwable error;
        public Error(Throwable error) { this.error = error; }
        public Throwable getError() { return error; }
        public String getMessage() {
            return error.getLocalizedMessage() != null ?
                error.getLocalizedMessage() : "Unknown error";
        }
    }

    public static final class Disconnected extends TransferEvent {
        public static final Disconnected INSTANCE = new Disconnected();
        private Disconnected() {}
    }

    public interface Listener {
        void onEvent(TransferEvent event);
    }
}
