package org.omnione.did.sdk.mdoc.proximity.holder.nfc;

import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocBaseMessageHandler;
import java.security.PrivateKey;
import java.util.Arrays;

/**
 * Handles NFC APDU commands and delegates decryption/encryption to MdocSessionManager
 */
public class MdocNfcMessageHandler extends MdocBaseMessageHandler {
    private static final String TAG = "MdocNfcMessageHandler";

    private final byte[] deviceEngagementBytes;
    private boolean isAidSelected = false;
    private int selectedFile = 0; // 0: None, 1: CC, 2: NDEF Data
    
    private byte[] pendingResponsePayload;
    private int responseOffset = 0;

    public MdocNfcMessageHandler(PrivateKey eDevicePrivateKey, byte[] deviceEngagementBytes) {
        super(eDevicePrivateKey, deviceEngagementBytes);
        this.deviceEngagementBytes = deviceEngagementBytes;
    }

    public byte[] processCommandApdu(byte[] apdu) {
        if (apdu == null || apdu.length < 4) return NfcConstants.SW_UNKNOWN;
        
        byte ins = apdu[1];
        byte p1 = apdu[2];
        byte p2 = apdu[3];

        // 1. SELECT
        if (ins == NfcConstants.INS_SELECT) {
            byte[] data = extractData(apdu);
            if (p1 == 0x04) { // SELECT BY AID
                if (Arrays.equals(data, NfcConstants.AID_ISO_18013_5) || Arrays.equals(data, NfcConstants.AID_NDEF)) {
                    isAidSelected = true;
                    return NfcConstants.SW_SUCCESS;
                }
            } else if (p1 == 0x00 && p2 == 0x0C) { // SELECT FILE
                if (data.length >= 2) {
                    if (Arrays.equals(data, NfcConstants.FILE_ID_CC)) {
                        selectedFile = 1; // CC File
                        return NfcConstants.SW_SUCCESS;
                    } else if (Arrays.equals(data, NfcConstants.FILE_ID_NDEF)) {
                        selectedFile = 2; // NDEF File
                        return NfcConstants.SW_SUCCESS;
                    }
                }
            }
        }

        if (!isAidSelected) return NfcConstants.SW_FILE_NOT_FOUND;

        // 2. READ BINARY (NDEF Engagement)
        if (ins == NfcConstants.INS_READ_BINARY) {
            int offset = ((p1 & 0xFF) << 8) | (p2 & 0xFF);
            int expectedLength = (apdu.length >= 5) ? (apdu[4] & 0xFF) : 0;
            if (selectedFile == 1) return buildReadResponse(NfcConstants.CC_FILE, offset, expectedLength);
            if (selectedFile == 2) return buildReadResponse(buildNdefMessage(), offset, expectedLength);
        }

        // 3. ENVELOPE (ISO 18013-5 Retrieval - Command)
        if (ins == NfcConstants.INS_ENVELOPE) {
            // MdocHostApduService will handle notifying the listener and calling setResponse
            return NfcConstants.SW_SUCCESS; 
        }

        // 4. GET RESPONSE (Response Chaining)
        if (ins == NfcConstants.INS_GET_RESPONSE) {
            return getNextResponseChunk();
        }

        return NfcConstants.SW_CLA_NOT_SUPPORTED;
    }

    public void setResponsePayload(byte[] payload) {
        this.pendingResponsePayload = payload;
        this.responseOffset = 0;
    }

    public byte[] getNextResponseChunk() {
        if (pendingResponsePayload == null || responseOffset >= pendingResponsePayload.length) {
            return NfcConstants.SW_NO_DATA;
        }

        int remaining = pendingResponsePayload.length - responseOffset;
        int chunkSize = Math.min(remaining, NfcConstants.MAX_APDU_RESPONSE_SIZE);
        byte[] chunk = new byte[chunkSize + 2];
        System.arraycopy(pendingResponsePayload, responseOffset, chunk, 0, chunkSize);
        
        responseOffset += chunkSize;
        if (responseOffset < pendingResponsePayload.length) {
            chunk[chunkSize] = NfcConstants.SW_MORE_DATA;
            chunk[chunkSize + 1] = (byte)Math.min(pendingResponsePayload.length - responseOffset, 0xFF);
        } else {
            chunk[chunkSize] = NfcConstants.SW_END_OF_DATA;
            chunk[chunkSize + 1] = 0x00;
            pendingResponsePayload = null;
        }
        return chunk;
    }

    private byte[] buildReadResponse(byte[] sourceData, int offset, int expectedLength) {
        if (offset >= sourceData.length) return NfcConstants.SW_SUCCESS;
        int lengthToCopy = Math.min(sourceData.length - offset, (expectedLength == 0) ? sourceData.length : expectedLength);
        byte[] response = new byte[lengthToCopy + 2];
        System.arraycopy(sourceData, offset, response, 0, lengthToCopy);
        response[lengthToCopy] = NfcConstants.SW_SUCCESS[0];
        response[lengthToCopy + 1] = NfcConstants.SW_SUCCESS[1];
        return response;
    }

    private byte[] buildNdefMessage() {
        byte[] type = NfcConstants.NDEF_RECORD_TYPE.getBytes();
        byte[] ndef = new byte[deviceEngagementBytes.length + type.length + 3];
        ndef[0] = (byte)0xD4; ndef[1] = (byte)type.length; ndef[2] = (byte)deviceEngagementBytes.length;
        System.arraycopy(type, 0, ndef, 3, type.length);
        System.arraycopy(deviceEngagementBytes, 0, ndef, 3 + type.length, deviceEngagementBytes.length);
        byte[] file = new byte[ndef.length + 2];
        file[0] = (byte)((ndef.length >> 8) & 0xFF); file[1] = (byte)(ndef.length & 0xFF);
        System.arraycopy(ndef, 0, file, 2, ndef.length);
        return file;
    }

    private byte[] extractData(byte[] apdu) {
        if (apdu.length <= 5) return new byte[0];
        int lc = apdu[4] & 0xFF;
        return Arrays.copyOfRange(apdu, 5, Math.min(apdu.length, 5 + lc));
    }
}
