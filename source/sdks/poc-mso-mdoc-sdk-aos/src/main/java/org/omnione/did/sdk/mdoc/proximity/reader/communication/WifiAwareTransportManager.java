package org.omnione.did.sdk.mdoc.proximity.reader.communication;

import android.content.Context;
import android.net.wifi.aware.*;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.omnione.did.sdk.mdoc.proximity.reader.core.*;
import org.omnione.did.sdk.mdoc.proximity.reader.exception.MdocReaderErrorCode;
import org.omnione.did.sdk.mdoc.proximity.reader.exception.MdocReaderException;
import org.omnione.did.sdk.mdoc.proximity.reader.utility.ProtocolLogger;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Wi-Fi Aware L2 메시지 기반 데이터 전송 관리자
// Holder(Publisher)의 sendMessage/onMessageReceived와 동일한 프로토콜 사용
// 청크 프로토콜: 1바이트 헤더(0x01=계속, 0x00=마지막) + 페이로드 (최대 254바이트)
@RequiresApi(api = Build.VERSION_CODES.Q)
public class WifiAwareTransportManager implements TransportManager {
    private static final String TAG = "MDR/WifiAwareTransport";
    private static final String SERVICE_NAME = "mdoc";

    // Wi-Fi Aware sendMessage 최대 크기는 255바이트, 헤더 1바이트 제외
    private static final int MAX_CHUNK_PAYLOAD = 254;
    private static final long CHUNK_SEND_INTERVAL_MS = 50;

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<TransferEvent.Listener> listeners = new CopyOnWriteArrayList<>();

    private DeviceEngagement deviceEngagement;
    private SessionEncryption sessionEncryption;

    private WifiAwareSession awareSession;
    private SubscribeDiscoverySession discoverySession;
    private PeerHandle peerHandle;

    // 수신 데이터 조립용 버퍼
    private final ByteArrayOutputStream incomingData = new ByteArrayOutputStream();

    public WifiAwareTransportManager(Context context) {
        this.context = context;
    }

    @Override
    public void addListener(TransferEvent.Listener listener) { listeners.add(listener); }
    @Override
    public void removeListener(TransferEvent.Listener listener) { listeners.remove(listener); }
    @Override
    public SessionEncryption getSessionEncryption() { return sessionEncryption; }

    private void notifyListeners(TransferEvent event) {
        for (TransferEvent.Listener listener : listeners) listener.onEvent(event);
    }

    @Override
    public void startDeviceEngagement(EngagementSource source) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                notifyListeners(new TransferEvent.Error(
                    new MdocReaderException(MdocReaderErrorCode.WIFI_AWARE_NOT_SUPPORTED)));
                return;
            }
            deviceEngagement = source.resolve();
            notifyListeners(TransferEvent.DeviceEngagementCompleted.INSTANCE);
            sessionEncryption = new SessionEncryption(
                deviceEngagement.getEDeviceKey(), deviceEngagement.getEncodedEngagement());
            if (!deviceEngagement.hasWifiAwareConnectionMethod()) {
                notifyListeners(new TransferEvent.Error(
                    new MdocReaderException(MdocReaderErrorCode.WIFI_AWARE_NO_CONNECTION_METHOD)));
                return;
            }
            ProtocolLogger.logWifiAwareConnectionMethod(
                deviceEngagement.getWifiAwarePassphrase(),
                deviceEngagement.getWifiAwareChannelInfo(),
                deviceEngagement.getWifiAwareBandInfo());
            attachWifiAware();
        } catch (Exception e) {
            Log.e(TAG, "Device engagement failed", e);
            notifyListeners(new TransferEvent.Error(e));
        }
    }

    private void attachWifiAware() {
        ProtocolLogger.logWifiAwareConnecting();
        notifyListeners(TransferEvent.Connecting.INSTANCE);
        WifiAwareManager awareManager = context.getSystemService(WifiAwareManager.class);
        if (awareManager == null || !awareManager.isAvailable()) {
            notifyListeners(new TransferEvent.Error(
                new MdocReaderException(MdocReaderErrorCode.WIFI_AWARE_NOT_AVAILABLE)));
            return;
        }
        awareManager.attach(new AttachCallback() {
            @Override public void onAttached(WifiAwareSession session) {
                awareSession = session;
                subscribe(session);
            }
            @Override public void onAttachFailed() {
                notifyListeners(new TransferEvent.Error(
                    new MdocReaderException(MdocReaderErrorCode.WIFI_AWARE_ATTACH_FAILED)));
            }
        }, mainHandler);
    }

    // Holder(Publisher)를 구독하여 탐색
    private void subscribe(WifiAwareSession session) {
        SubscribeConfig config = new SubscribeConfig.Builder()
            .setServiceName(SERVICE_NAME)
            .build();

        session.subscribe(config, new DiscoverySessionCallback() {
            @Override
            public void onSubscribeStarted(@NonNull SubscribeDiscoverySession session) {
                discoverySession = session;
                Log.d(TAG, "Wi-Fi Aware subscribe started: " + SERVICE_NAME);
            }

            @Override
            public void onServiceDiscovered(PeerHandle handle,
                                             byte[] serviceSpecificInfo,
                                             List<byte[]> matchFilter) {
                peerHandle = handle;
                ProtocolLogger.logWifiAwarePeerDiscovered();
                // L2 메시지 방식에서는 피어 발견 = 연결 완료
                notifyListeners(TransferEvent.Connected.INSTANCE);
            }

            @Override
            public void onMessageReceived(PeerHandle handle, byte[] message) {
                handleIncomingChunk(message);
            }
        }, mainHandler);
    }

    // 수신 청크 처리: 1바이트 헤더 + 페이로드
    private synchronized void handleIncomingChunk(byte[] chunk) {
        if (chunk == null || chunk.length == 0) return;
        byte header = chunk[0];
        incomingData.write(chunk, 1, chunk.length - 1);
        boolean isLast = (header == 0x00);
        ProtocolLogger.logWifiAwareDataReceived(chunk.length - 1);

        if (isLast) {
            byte[] completeData = incomingData.toByteArray();
            incomingData.reset();
            Log.d(TAG, "Wi-Fi Aware 전체 응답 수신 완료: " + completeData.length + " bytes");

            // 복호화 처리
            try {
                byte[] sessionData = sessionEncryption.parseSessionData(completeData);
                if (sessionData == null) {
                    notifyListeners(TransferEvent.Disconnected.INSTANCE);
                    return;
                }
                byte[] decrypted = sessionEncryption.decryptResponse(sessionData);
                notifyListeners(new TransferEvent.ResponseReceived(decrypted));
            } catch (Exception e) {
                Log.e(TAG, "Failed to process Wi-Fi Aware response", e);
                notifyListeners(new TransferEvent.Error(e));
            }
        }
    }

    @Override
    public void sendRequest(byte[] deviceRequestBytes) {
        if (discoverySession == null || peerHandle == null) {
            notifyListeners(new TransferEvent.Error(
                new MdocReaderException(MdocReaderErrorCode.WIFI_AWARE_NETWORK_FAILED,
                    "No active session or peer")));
            return;
        }
        try {
            byte[] encrypted = sessionEncryption.encryptRequest(deviceRequestBytes);
            byte[] sessionEstablishment = sessionEncryption.buildSessionEstablishment(encrypted);
            ProtocolLogger.logWifiAwareSendStart(sessionEstablishment.length);
            sendChunkedMessage(sessionEstablishment);
            notifyListeners(TransferEvent.RequestSent.INSTANCE);
        } catch (Exception e) {
            Log.e(TAG, "Send request failed", e);
            notifyListeners(new TransferEvent.Error(e));
        }
    }

    // 데이터를 청크로 분할하여 sendMessage로 전송
    private void sendChunkedMessage(byte[] data) {
        List<byte[]> chunks = new ArrayList<>();
        int offset = 0;
        while (offset < data.length) {
            int chunkSize = Math.min(MAX_CHUNK_PAYLOAD, data.length - offset);
            boolean isLast = (offset + chunkSize >= data.length);
            byte[] chunk = new byte[chunkSize + 1];
            chunk[0] = isLast ? (byte) 0x00 : (byte) 0x01;
            System.arraycopy(data, offset, chunk, 1, chunkSize);
            chunks.add(chunk);
            offset += chunkSize;
        }
        Log.d(TAG, "Sending " + chunks.size() + " chunks via Wi-Fi Aware L2 message");
        for (int i = 0; i < chunks.size(); i++) {
            final byte[] chunk = chunks.get(i);
            final int idx = i;
            mainHandler.postDelayed(() -> {
                if (discoverySession != null && peerHandle != null) {
                    discoverySession.sendMessage(peerHandle, 0, chunk);
                    Log.d(TAG, "Sent chunk [" + (idx + 1) + "/" + chunks.size() + "]: " + chunk.length + " bytes");
                }
            }, (long) i * CHUNK_SEND_INTERVAL_MS);
        }
    }

    @Override
    public void stopSession() {
        if (discoverySession != null) { discoverySession.close(); discoverySession = null; }
        if (awareSession != null) { awareSession.close(); awareSession = null; }
        peerHandle = null;
        incomingData.reset();
        notifyListeners(TransferEvent.Disconnected.INSTANCE);
    }
}
