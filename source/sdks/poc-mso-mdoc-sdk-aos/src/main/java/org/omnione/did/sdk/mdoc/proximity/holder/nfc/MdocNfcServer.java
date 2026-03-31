package org.omnione.did.sdk.mdoc.proximity.holder.nfc;

import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocSessionManager;
import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocProximityListener;
import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocProximityServer;
import java.security.PrivateKey;

/**
 * NFC Implementation of MdocProximityServer (Proxy for HCE Service)
 */
public class MdocNfcServer implements MdocProximityServer {
    private final PrivateKey privateKey;
    private final byte[] deviceEngagementBytes;
    private final byte[] handoverBytes;
    private MdocProximityListener listener;

    public MdocNfcServer(PrivateKey privateKey, byte[] deviceEngagementBytes, byte[] handoverBytes) {
        this.privateKey = privateKey;
        this.deviceEngagementBytes = deviceEngagementBytes;
        this.handoverBytes = handoverBytes;
    }

    @Override
    public void start() {
        // Initialize HCE Service with necessary keys and listener
        MdocHostApduService.setup(privateKey, deviceEngagementBytes, handoverBytes, listener);
    }

    @Override
    public void stop() {
        // Reset HCE static state
        MdocHostApduService.setup(null, null, null, null);
    }

    public MdocSessionManager getSessionManager() {
        MdocNfcMessageHandler handler = MdocHostApduService.getMessageHandler();
        return handler != null ? handler.getSessionManager() : null;
    }

    @Override
    public void sendResponse(byte[] response) {
        // Provide the response payload to the HCE service.
        // The Reader will retrieve it using the GET RESPONSE (0xC0) command.
        MdocHostApduService.setResponse(response);
    }

    @Override
    public void setListener(MdocProximityListener listener) {
        this.listener = listener;
    }
}
