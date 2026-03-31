package org.omnione.did.sdk.mdoc.proximity.reader.communication;

import org.omnione.did.sdk.mdoc.proximity.reader.core.EngagementSource;
import org.omnione.did.sdk.mdoc.proximity.reader.core.SessionEncryption;

public interface TransportManager {
    void addListener(TransferEvent.Listener listener);
    void removeListener(TransferEvent.Listener listener);
    void startDeviceEngagement(EngagementSource source);
    void sendRequest(byte[] deviceRequestBytes);
    SessionEncryption getSessionEncryption();
    void stopSession();
}
