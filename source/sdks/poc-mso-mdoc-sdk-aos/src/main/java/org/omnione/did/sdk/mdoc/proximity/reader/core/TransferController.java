package org.omnione.did.sdk.mdoc.proximity.reader.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.omnione.did.sdk.mdoc.proximity.reader.communication.*;
import org.omnione.did.sdk.mdoc.proximity.reader.datamodel.document.*;
import org.omnione.did.sdk.mdoc.proximity.reader.utility.ProtocolLogger;

import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransferController {
    private static final String TAG = "MDR/Transfer";

    public interface TransferCallback {
        void onStatusChanged(TransferStatus status);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TransportManager transportManager;
    private TrustManager trustManager;
    private boolean skipIssuerTrust = false;
    private TransferEvent.Listener currentListener;
    private TransferCallback currentCallback;
    private ExecutorService executor;
    private volatile boolean requestSent = false;

    public TransferController(Context context) {
        this.context = context;
    }

    public void initializeVerifier(List<String> certificates, boolean skipIssuerTrust) {
        initializeVerifier(certificates, skipIssuerTrust, false);
    }

    public void initializeVerifier(List<String> certificates, boolean skipIssuerTrust, boolean includeSystemRoots) {
        this.skipIssuerTrust = skipIssuerTrust;
        List<X509Certificate> x509Certs = TrustManager.loadCertificatesFromPem(certificates);
        trustManager = new TrustManager(x509Certs, includeSystemRoots);
    }

    public void initializeTransferManager(TransportConfig config) {
        transportManager = TransportManagerFactory.create(context, config);
    }

    public void startEngagement(EngagementSource source) {
        if (transportManager != null) {
            transportManager.startDeviceEngagement(source);
        }
    }

    public void sendRequest(List<RequestedDocument> requestedDocs,
                             boolean retainData,
                             TransferCallback callback) {
        this.currentCallback = callback;
        this.requestSent = false;

        if (transportManager == null) {
            notifyCallback(TransferStatus.error("TransferManager not initialized"));
            return;
        }

        // 디바이스 요청 구성
        List<DeviceRequestBuilder.DocRequest> docRequests = new ArrayList<>();
        for (RequestedDocument doc : requestedDocs) {
            Map<String, Boolean> claims = new LinkedHashMap<>();
            for (String claim : doc.getClaims()) {
                claims.put(claim, retainData);
            }
            Map<String, Map<String, Boolean>> itemsRequest = new LinkedHashMap<>();
            itemsRequest.put(doc.getNamespace(), claims);
            docRequests.add(new DeviceRequestBuilder.DocRequest(doc.getDocType(), itemsRequest));
        }

        byte[] deviceRequestBytes;
        try {
            deviceRequestBytes = DeviceRequestBuilder.build(docRequests);
        } catch (Exception e) {
            notifyCallback(TransferStatus.error(e.getMessage()));
            return;
        }

        TransferEvent.Listener listener = new TransferEvent.Listener() {
            @Override
            public void onEvent(TransferEvent event) {
                if (event instanceof TransferEvent.Connecting) {
                    notifyCallback(TransferStatus.connecting());
                } else if (event instanceof TransferEvent.Connected) {
                    if (requestSent) {
                        Log.w(TAG, "Ignoring duplicate Connected event, request already sent");
                        return;
                    }
                    requestSent = true;
                    Log.d(TAG, "Connected event received, sending request (" + deviceRequestBytes.length + " bytes)");
                    try {
                        transportManager.sendRequest(deviceRequestBytes);
                        Log.d(TAG, "sendRequest completed");
                    } catch (Exception e) {
                        Log.e(TAG, "sendRequest failed", e);
                        requestSent = false;
                        notifyCallback(TransferStatus.error("Send failed: " + e.getMessage()));
                        return;
                    }
                    notifyCallback(TransferStatus.connected());
                } else if (event instanceof TransferEvent.DeviceEngagementCompleted) {
                    notifyCallback(TransferStatus.deviceEngagementCompleted());
                } else if (event instanceof TransferEvent.RequestSent) {
                    notifyCallback(TransferStatus.requestSent());
                } else if (event instanceof TransferEvent.ResponseReceived) {
                    TransferEvent.ResponseReceived resp = (TransferEvent.ResponseReceived) event;
                    getExecutor().execute(() -> processResponse(resp.getData()));
                } else if (event instanceof TransferEvent.Error) {
                    TransferEvent.Error err = (TransferEvent.Error) event;
                    notifyCallback(TransferStatus.error(err.getMessage()));
                } else if (event instanceof TransferEvent.Disconnected) {
                    notifyCallback(TransferStatus.disconnected());
                }
            }
        };

        if (currentListener != null) {
            transportManager.removeListener(currentListener);
        }
        currentListener = listener;
        transportManager.addListener(listener);
    }

    private void notifyCallback(TransferStatus status) {
        if (currentCallback != null) {
            mainHandler.post(() -> currentCallback.onStatusChanged(status));
        }
    }

    private void processResponse(byte[] responseData) {
        try {
            SessionEncryption se = transportManager != null ? transportManager.getSessionEncryption() : null;
            DeviceResponseParser.ParsedResponse parsed = DeviceResponseParser.parse(responseData, se);
            List<ReceivedDocument> documents = new ArrayList<>();

            for (DeviceResponseParser.ParsedDocument doc : parsed.getDocuments()) {
                boolean trusted = skipIssuerTrust || (trustManager != null && trustManager.isDocumentTrusted(doc));
                Map<String, Object> claims = doc.getFlattenedClaims();

                DocumentValidity validity = null;
                if (doc.getIssuerSignedInfo() != null) {
                    DeviceResponseParser.IssuerSignedInfo info = doc.getIssuerSignedInfo();
                    validity = new DocumentValidity(
                        doc.isDeviceSignatureValid(),
                        info.isIssuerSignatureValid(),
                        doc.isDataIntegrityIntact(),
                        info.getSigned(), info.getValidFrom(), info.getValidUntil()
                    );
                } else if (doc.isDeviceSignatureValid() != null) {
                    validity = new DocumentValidity(
                        doc.isDeviceSignatureValid(), null, null,
                        null, null, null
                    );
                }

                documents.add(new ReceivedDocument(
                    trusted, doc.getDocType(), claims,
                    validity != null ? validity : new DocumentValidity()
                ));
            }

            // 최종 결과 요약 로그
            List<String> docTypes = new ArrayList<>();
            List<Integer> claimCounts = new ArrayList<>();
            List<Boolean> trustedList = new ArrayList<>();
            List<Boolean> issuerSigList = new ArrayList<>();
            List<Boolean> deviceSigList = new ArrayList<>();
            List<Boolean> integrityList = new ArrayList<>();
            for (ReceivedDocument rd : documents) {
                docTypes.add(rd.getDocType());
                claimCounts.add(rd.getClaims() != null ? rd.getClaims().size() : 0);
                trustedList.add(rd.isTrusted());
                DocumentValidity v = rd.getValidity();
                issuerSigList.add(v != null ? v.isIssuerSignatureValid() : null);
                deviceSigList.add(v != null ? v.isDeviceSignatureValid() : null);
                integrityList.add(v != null ? v.isDataIntegrityIntact() : null);
            }
            ProtocolLogger.logTransferResult(documents.size(), docTypes, claimCounts,
                trustedList, issuerSigList, deviceSigList, integrityList);

            notifyCallback(TransferStatus.responseReceived(documents));
        } catch (Exception e) {
            Log.e(TAG, "Failed to process response", e);
            notifyCallback(TransferStatus.error(e.getMessage()));
        }
    }

    private synchronized ExecutorService getExecutor() {
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor();
        }
        return executor;
    }

    public void stopConnection() {
        currentCallback = null;
        requestSent = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        if (currentListener != null && transportManager != null) {
            transportManager.removeListener(currentListener);
            currentListener = null;
        }
        if (transportManager != null) {
            transportManager.stopSession();
            transportManager = null;
        }
    }
}
