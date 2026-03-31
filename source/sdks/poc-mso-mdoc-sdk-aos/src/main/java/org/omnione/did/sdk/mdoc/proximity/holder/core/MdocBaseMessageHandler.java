package org.omnione.did.sdk.mdoc.proximity.holder.core;

import android.util.Log;
import com.upokecenter.cbor.CBORObject;
import java.io.ByteArrayOutputStream;
import java.security.PrivateKey;

/**
 * Base class for mDoc message handlers.
 * Provides common session management and chunking logic.
 */
public abstract class MdocBaseMessageHandler {
    protected final MdocSessionManager sessionManager;
    protected final ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();

    public MdocBaseMessageHandler(PrivateKey eDevicePrivateKey, byte[] deviceEngagementBytes) {
        this.sessionManager = new MdocSessionManager(eDevicePrivateKey, deviceEngagementBytes);
    }

    /**
     * Handles incoming data chunks based on ISO 18013-5 (0x01: More, 0x00: Last).
     * @param chunk The received data chunk.
     * @param tag Tag for logging.
     * @return The complete message if all chunks are received, null otherwise.
     */
    public byte[] receiveChunk(byte[] chunk, String tag) {
        if (chunk == null || chunk.length == 0) return null;

        byte header = chunk[0];
        messageBuffer.write(chunk, 1, chunk.length - 1);

        if (header == 0x01) {
            Log.d(tag, "Chunk 수신 중...");
            return null;
        } else if (header == 0x00) {
            byte[] completeMessage = messageBuffer.toByteArray();
            messageBuffer.reset();

            // Decrypt SessionEstablishment
            CBORObject deviceRequest = sessionManager.decryptSessionEstablishment(completeMessage);
            if (deviceRequest != null) {
                Log.i(tag, "Reader request decrypted successfully.");
            }

            return completeMessage;
        }

        return null;
    }

    public MdocSessionManager getSessionManager() {
        return sessionManager;
    }
}
