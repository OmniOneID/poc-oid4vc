package org.omnione.did.sdk.mdoc.proximity.holder.wifi;

import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocBaseMessageHandler;
import java.security.PrivateKey;

public class MdocWifiMessageHandler extends MdocBaseMessageHandler {
    private static final String TAG = "MdocWifiMessageHandler";

    public MdocWifiMessageHandler(PrivateKey eDevicePrivateKey, byte[] deviceEngagementBytes) {
        super(eDevicePrivateKey, deviceEngagementBytes);
    }

    public byte[] receiveChunk(byte[] chunk) {
        return super.receiveChunk(chunk, TAG);
    }
}
