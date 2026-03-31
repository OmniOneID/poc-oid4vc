package org.omnione.did.sdk.mdoc.proximity.holder.nfc;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;
import com.upokecenter.cbor.CBORObject;
import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocProximityListener;

import java.io.ByteArrayOutputStream;
import java.security.PrivateKey;
import java.util.Arrays;

/**
 * Android HostApduService implementation for ISO 18013-5 NFC Retrieval.
 * Supports APDU chaining for large ENVELOPE commands.
 */
public class MdocHostApduService extends HostApduService {
    private static final String TAG = "MdocHostApduService";

    private static MdocNfcMessageHandler staticMessageHandler;
    private static MdocProximityListener staticListener;

    // Buffer to accumulate data from APDU chaining
    private final ByteArrayOutputStream chainingBuffer = new ByteArrayOutputStream();

    /**
     * Initializes the static handler and listener.
     */
    public static void setup(PrivateKey privateKey, byte[] deviceEngagementBytes, byte[] handoverBytes, MdocProximityListener listener) {
        if (privateKey == null) {
            staticMessageHandler = null;
            staticListener = null;
            return;
        }
        staticMessageHandler = new MdocNfcMessageHandler(privateKey, deviceEngagementBytes);
        staticListener = listener;
        Log.d(TAG, "NFC HostApduService setup complete.");
    }

    /**
     * Sets the response payload to be retrieved by the reader.
     */
    public static void setResponse(byte[] response) {
        if (staticMessageHandler != null) {
            staticMessageHandler.setResponsePayload(response);
        }
    }

    public static MdocNfcMessageHandler getMessageHandler() {
        return staticMessageHandler;
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        if (commandApdu == null || commandApdu.length < 4) {
            return NfcConstants.SW_UNKNOWN;
        }

        if (staticMessageHandler == null) {
            Log.e(TAG, "NFC Message Handler not initialized.");
            return NfcConstants.SW_UNKNOWN;
        }

        byte cla = commandApdu[0];
        byte ins = commandApdu[1];

        // Handle ISO 18013-5 ENVELOPE (Command) with APDU chaining support
        if (ins == NfcConstants.INS_ENVELOPE) {
            byte[] chunkData = extractData(commandApdu);
            chainingBuffer.write(chunkData, 0, chunkData.length);

            // Check CLA bit 4 for chaining (0x10 means more chunks are coming)
            if ((cla & 0x10) != 0) {
                Log.d(TAG, "ENVELOPE chaining: accumulated " + chainingBuffer.size() + " bytes");
                return NfcConstants.SW_SUCCESS;
            }

            // Last chunk received: process the complete data
            byte[] completeData = chainingBuffer.toByteArray();
            chainingBuffer.reset();
            Log.i(TAG, "ENVELOPE Complete data received: " + completeData.length + " bytes");

            // Decrypt the request to establish the session if it's the first message
            CBORObject request = staticMessageHandler.getSessionManager().decryptSessionEstablishment(completeData);

            if (request != null && staticListener != null) {
                staticListener.onDeviceConnected();
                staticListener.onRequestReceived(completeData);
                // 0x61 00 indicates more data is available for GET RESPONSE
                return new byte[]{NfcConstants.SW_MORE_DATA, 0x00};
            }
        }

        // Delegate other commands (SELECT, READ BINARY, GET RESPONSE) to the handler
        return staticMessageHandler.processCommandApdu(commandApdu);
    }

    @Override
    public void onDeactivated(int reason) {
        Log.i(TAG, "NFC Link Deactivated. Reason: " + reason);
        chainingBuffer.reset();
        if (staticListener != null) {
            staticListener.onDeviceDisconnected();
        }
    }

    private byte[] extractData(byte[] apdu) {
        if (apdu.length <= 5) return new byte[0];
        int lc = apdu[4] & 0xFF;
        return Arrays.copyOfRange(apdu, 5, Math.min(apdu.length, 5 + lc));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
