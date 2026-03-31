package org.omnione.did.sdk.mdoc.proximity.reader.communication;

import android.nfc.tech.IsoDep;
import android.util.Log;

import org.omnione.did.sdk.mdoc.proximity.reader.core.*;
import org.omnione.did.sdk.mdoc.proximity.reader.exception.MdocReaderErrorCode;
import org.omnione.did.sdk.mdoc.proximity.reader.exception.MdocReaderException;
import org.omnione.did.sdk.mdoc.proximity.reader.utility.ProtocolLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NfcTransportManager implements TransportManager {
    private static final String TAG = "MDR/NfcTransport";
    private static final byte[] MDOC_AID = {(byte)0xA0, 0x00, 0x00, 0x02, 0x48, 0x04, 0x00};
    private static final byte CLA_NO_CHAINING = (byte)0x00;
    private static final byte CLA_CHAINING = (byte)0x10;
    private static final byte INS_SELECT = (byte)0xA4;
    private static final byte INS_ENVELOPE = (byte)0xC2;
    private static final byte INS_GET_RESPONSE = (byte)0xC0;
    private static final int SW_SUCCESS = 0x9000;
    private static final int SW_MORE_DATA_PREFIX = 0x6100;
    private static final int DEFAULT_MAX_COMMAND_DATA_LENGTH = 255;
    private static final int DEFAULT_MAX_RESPONSE_DATA_LENGTH = 256;

    private final IsoDep isoDep;
    private final int maxCommandDataLength;
    private final int maxResponseDataLength;
    private final List<TransferEvent.Listener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private DeviceEngagement deviceEngagement;
    private SessionEncryption sessionEncryption;

    public NfcTransportManager(IsoDep isoDep, int maxCommandDataLength, int maxResponseDataLength) {
        this.isoDep = isoDep;
        // short-form Lc(1바이트) 호환을 위해 255로 제한 — APDU chaining으로 분할 전송
        this.maxCommandDataLength = Math.min(
            maxCommandDataLength > 0 ? maxCommandDataLength : DEFAULT_MAX_COMMAND_DATA_LENGTH, 255);
        this.maxResponseDataLength = maxResponseDataLength > 0 ? maxResponseDataLength : DEFAULT_MAX_RESPONSE_DATA_LENGTH;
    }

    @Override public void addListener(TransferEvent.Listener listener) { listeners.add(listener); }
    @Override public void removeListener(TransferEvent.Listener listener) { listeners.remove(listener); }
    @Override public SessionEncryption getSessionEncryption() { return sessionEncryption; }

    private void notifyListeners(TransferEvent event) {
        for (TransferEvent.Listener l : listeners) l.onEvent(event);
    }

    @Override
    public void startDeviceEngagement(EngagementSource source) {
        try {
            deviceEngagement = source.resolve();
            notifyListeners(TransferEvent.DeviceEngagementCompleted.INSTANCE);
            sessionEncryption = new SessionEncryption(
                deviceEngagement.getEDeviceKey(), deviceEngagement.getEncodedEngagement());
            ProtocolLogger.logNfcConnecting();
            ProtocolLogger.logNfcConnectionMethod(maxCommandDataLength, maxResponseDataLength);
            notifyListeners(TransferEvent.Connecting.INSTANCE);
            ioExecutor.execute(() -> {
                try {
                    if (!isoDep.isConnected()) isoDep.connect();
                    isoDep.setTimeout(30000);
                    boolean selected = selectMdocApplication();
                    ProtocolLogger.logNfcSelectResult(selected);
                    if (!selected) {
                        notifyListeners(new TransferEvent.Error(new MdocReaderException(MdocReaderErrorCode.NFC_SELECT_FAILED)));
                        return;
                    }
                    notifyListeners(TransferEvent.Connected.INSTANCE);
                } catch (IOException e) {
                    Log.e(TAG, "NFC connection failed", e);
                    notifyListeners(new TransferEvent.Error(new MdocReaderException(MdocReaderErrorCode.NFC_NOT_AVAILABLE, e.getMessage())));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Device engagement failed", e);
            notifyListeners(new TransferEvent.Error(e));
        }
    }

    private boolean selectMdocApplication() throws IOException {
        byte[] apdu = buildApdu(CLA_NO_CHAINING, INS_SELECT, (byte)0x04, (byte)0x00, MDOC_AID);
        byte[] response = isoDep.transceive(apdu);
        return getSw(response) == SW_SUCCESS;
    }

    @Override
    public void sendRequest(byte[] deviceRequestBytes) {
        ioExecutor.execute(() -> {
            try {
                byte[] encrypted = sessionEncryption.encryptRequest(deviceRequestBytes);
                byte[] sessionEstablishment = sessionEncryption.buildSessionEstablishment(encrypted);
                sendViaEnvelopeChunks(sessionEstablishment);
                notifyListeners(TransferEvent.RequestSent.INSTANCE);
                byte[] responseData = receiveViaGetResponse();
                // Parse and decrypt
                byte[] sessionData = sessionEncryption.parseSessionData(responseData);
                if (sessionData == null) {
                    notifyListeners(TransferEvent.Disconnected.INSTANCE);
                    return;
                }
                byte[] decrypted = sessionEncryption.decryptResponse(sessionData);
                notifyListeners(new TransferEvent.ResponseReceived(decrypted));
                sendSessionTermination();
            } catch (Exception e) {
                Log.e(TAG, "NFC transfer failed", e);
                if (e instanceof android.nfc.TagLostException) {
                    notifyListeners(new TransferEvent.Error(new MdocReaderException(MdocReaderErrorCode.NFC_TAG_LOST)));
                } else {
                    notifyListeners(new TransferEvent.Error(e));
                }
            }
        });
    }

    private void sendViaEnvelopeChunks(byte[] data) throws Exception {
        int offset = 0;
        int totalChunks = (int) Math.ceil((double) data.length / maxCommandDataLength);
        int chunkIndex = 0;
        while (offset < data.length) {
            int chunkLen = Math.min(maxCommandDataLength, data.length - offset);
            boolean isLast = (offset + chunkLen >= data.length);
            byte[] chunk = new byte[chunkLen];
            System.arraycopy(data, offset, chunk, 0, chunkLen);
            byte cla = isLast ? CLA_NO_CHAINING : CLA_CHAINING;
            byte[] apdu = buildApdu(cla, INS_ENVELOPE, (byte)0x00, (byte)0x00, chunk);
            ProtocolLogger.logNfcEnvelopeSent(chunkIndex, totalChunks, chunkLen);
            byte[] response = isoDep.transceive(apdu);
            int sw = getSw(response);
            if (sw != SW_SUCCESS && (sw & 0xFF00) != SW_MORE_DATA_PREFIX)
                throw new MdocReaderException(MdocReaderErrorCode.NFC_APDU_ERROR, "ENVELOPE failed SW: " + String.format("%04X", sw));
            offset += chunkLen;
            chunkIndex++;
        }
    }

    // Holder가 응답을 준비할 시간이 필요하므로, 6A80(데이터 없음) 시 재시도
    private static final int GET_RESPONSE_MAX_RETRIES = 40;
    private static final long GET_RESPONSE_RETRY_INTERVAL_MS = 250;
    private static final int SW_DATA_NOT_AVAILABLE = 0x6A80;

    private byte[] receiveViaGetResponse() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int retryCount = 0;

        while (true) {
            byte[] apdu = buildApdu(CLA_NO_CHAINING, INS_GET_RESPONSE, (byte)0x00, (byte)0x00, null);
            byte[] response = isoDep.transceive(apdu);
            int sw = getSw(response);

            // Holder가 아직 응답을 준비 중이면 대기 후 재시도
            if (sw == SW_DATA_NOT_AVAILABLE) {
                retryCount++;
                if (retryCount > GET_RESPONSE_MAX_RETRIES) {
                    throw new MdocReaderException(MdocReaderErrorCode.NFC_APDU_ERROR,
                        "GET RESPONSE timeout: holder did not prepare response after " + retryCount + " retries");
                }
                Log.d(TAG, "GET RESPONSE: data not ready, retry " + retryCount + "/" + GET_RESPONSE_MAX_RETRIES);
                Thread.sleep(GET_RESPONSE_RETRY_INTERVAL_MS);
                continue;
            }

            int dataLen = response.length - 2;
            if (dataLen > 0) baos.write(response, 0, dataLen);
            boolean hasMore = (sw & 0xFF00) == SW_MORE_DATA_PREFIX;
            ProtocolLogger.logNfcGetResponseReceived(dataLen, hasMore);
            if (sw == SW_SUCCESS) break;
            else if (hasMore) continue;
            else throw new MdocReaderException(MdocReaderErrorCode.NFC_APDU_ERROR,
                "GET RESPONSE failed SW: " + String.format("%04X", sw));
        }
        return baos.toByteArray();
    }

    private void sendSessionTermination() {
        try {
            byte[] term = sessionEncryption.buildSessionTermination();
            sendViaEnvelopeChunks(term);
        } catch (Exception e) { Log.w(TAG, "Failed to send session termination", e); }
    }

    // short-form Lc만 사용 (Holder 호환성)
    private byte[] buildApdu(byte cla, byte ins, byte p1, byte p2, byte[] data) {
        ByteArrayOutputStream apdu = new ByteArrayOutputStream();
        apdu.write(cla); apdu.write(ins); apdu.write(p1); apdu.write(p2);
        if (data != null && data.length > 0) {
            apdu.write((byte) data.length);
            apdu.write(data, 0, data.length);
        }
        if (data == null && ins == INS_GET_RESPONSE) apdu.write(0x00);
        return apdu.toByteArray();
    }

    private int getSw(byte[] response) {
        if (response == null || response.length < 2) return -1;
        return ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
    }

    @Override
    public void stopSession() {
        ioExecutor.execute(() -> { try { if (isoDep != null && isoDep.isConnected()) isoDep.close(); } catch (IOException ignored) {} });
        ioExecutor.shutdown();
        notifyListeners(TransferEvent.Disconnected.INSTANCE);
    }
}
