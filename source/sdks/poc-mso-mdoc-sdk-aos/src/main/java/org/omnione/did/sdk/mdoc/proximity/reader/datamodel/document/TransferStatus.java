package org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document;

import java.util.List;

public class TransferStatus {
    public enum Type {
        CONNECTING, CONNECTED, DEVICE_ENGAGEMENT_COMPLETED,
        REQUEST_SENT, RESPONSE_RECEIVED, ERROR, DISCONNECTED
    }

    private final Type type;
    private final String errorMessage;
    private final List<ReceivedDocument> receivedDocuments;

    private TransferStatus(Type type, String errorMessage, List<ReceivedDocument> documents) {
        this.type = type;
        this.errorMessage = errorMessage;
        this.receivedDocuments = documents;
    }

    public Type getType() { return type; }
    public String getErrorMessage() { return errorMessage; }
    public List<ReceivedDocument> getReceivedDocuments() { return receivedDocuments; }

    public static TransferStatus connecting() { return new TransferStatus(Type.CONNECTING, null, null); }
    public static TransferStatus connected() { return new TransferStatus(Type.CONNECTED, null, null); }
    public static TransferStatus deviceEngagementCompleted() { return new TransferStatus(Type.DEVICE_ENGAGEMENT_COMPLETED, null, null); }
    public static TransferStatus requestSent() { return new TransferStatus(Type.REQUEST_SENT, null, null); }
    public static TransferStatus responseReceived(List<ReceivedDocument> docs) { return new TransferStatus(Type.RESPONSE_RECEIVED, null, docs); }
    public static TransferStatus error(String message) { return new TransferStatus(Type.ERROR, message, null); }
    public static TransferStatus disconnected() { return new TransferStatus(Type.DISCONNECTED, null, null); }
}
