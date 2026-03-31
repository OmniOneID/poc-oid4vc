package org.omnione.did.sdk.mdoc.proximity.holder.ble;

import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocBaseMessageHandler;
import java.security.PrivateKey;

public class MdocBleMessageHandler extends MdocBaseMessageHandler {
    private static final String TAG = "MdocBleMessageHandler";

    public MdocBleMessageHandler(PrivateKey eDevicePrivateKey, byte[] deviceEngagementBytes) {
        super(eDevicePrivateKey, deviceEngagementBytes);
    }

    public byte[] receiveChunk(byte[] chunk) {
        return super.receiveChunk(chunk, TAG);
    }
}
