package org.omnione.did.sdk.mdoc.proximity.holder.wifi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.aware.*;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocProximityListener;
import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocProximityServer;
import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocSessionManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;

/**
 * Wi-Fi Aware transport using TCP sockets (ISO 18013-5 compliant).
 * Flow: L2 handshake for peer discovery → TCP network → data over TCP.
 */
@SuppressLint("MissingPermission")
@RequiresApi(api = Build.VERSION_CODES.Q)
public class MdocWifiServer implements MdocProximityServer {
    private static final String TAG = "WifiServer";
    private static final int TCP_TIMEOUT_MS = 30000;

    private final Context context;
    private final WifiAwareManager wifiAwareManager;
    private final String passphrase;
    private final MdocWifiMessageHandler messageHandler;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private WifiAwareSession wifiAwareSession;
    private PublishDiscoverySession publishSession;
    private PeerHandle connectedPeerHandle;
    private MdocProximityListener listener;

    private ServerSocket serverSocket;
    private Socket tcpSocket;
    private ConnectivityManager.NetworkCallback networkCallback;

    public MdocWifiServer(Context context, PrivateKey privateKey, byte[] deviceEngagementBytes, String passphrase) {
        this.context = context;
        this.wifiAwareManager = (WifiAwareManager) context.getSystemService(Context.WIFI_AWARE_SERVICE);
        this.messageHandler = new MdocWifiMessageHandler(privateKey, deviceEngagementBytes);
        this.passphrase = passphrase;
    }

    // Backward-compatible constructor (passphrase defaults to null, will use L2 fallback)
    public MdocWifiServer(Context context, PrivateKey privateKey, byte[] deviceEngagementBytes) {
        this(context, privateKey, deviceEngagementBytes, null);
    }

    @Override
    public void setListener(MdocProximityListener listener) {
        this.listener = listener;
    }

    @Override
    public void start() {
        if (wifiAwareManager == null || !wifiAwareManager.isAvailable()) {
            Log.e(TAG, "WiFi Aware is not supported or currently disabled.");
            return;
        }
        Log.i(TAG, "Attaching to WiFi Aware...");
        wifiAwareManager.attach(new AttachCallback() {
            @Override
            public void onAttached(WifiAwareSession session) {
                Log.i(TAG, "WiFi Aware Attached.");
                wifiAwareSession = session;
                startPublishing();
            }

            @Override
            public void onAttachFailed() {
                Log.e(TAG, "WiFi Aware Attach Failed.");
            }
        }, mainHandler);
    }

    @Override
    public void stop() {
        Log.i(TAG, "Stopping WiFi Aware Server...");
        closeTcp();
        if (networkCallback != null) {
            try {
                ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
                cm.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
            networkCallback = null;
        }
        if (publishSession != null) { publishSession.close(); publishSession = null; }
        if (wifiAwareSession != null) { wifiAwareSession.close(); wifiAwareSession = null; }
    }

    public MdocSessionManager getSessionManager() {
        return messageHandler.getSessionManager();
    }

    private void startPublishing() {
        if (wifiAwareSession == null) return;

        PublishConfig config = new PublishConfig.Builder()
                .setServiceName(WifiAwareConstants.SERVICE_NAME)
                .build();

        Log.i(TAG, "Publishing service: " + WifiAwareConstants.SERVICE_NAME);
        wifiAwareSession.publish(config, new DiscoverySessionCallback() {
            @Override
            public void onPublishStarted(@NonNull PublishDiscoverySession session) {
                Log.i(TAG, "WiFi Aware Publish Started.");
                publishSession = session;
            }

            @Override
            public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                Log.i(TAG, "WiFi Aware handshake received from peer.");
                connectedPeerHandle = peerHandle;
                if (listener != null) listener.onDeviceConnected();
                establishTcpServer();
            }

            @Override
            public void onSessionConfigFailed() {
                Log.e(TAG, "WiFi Aware Publish Session Config Failed.");
            }
        }, mainHandler);
    }

    private void establishTcpServer() {
        if (publishSession == null || connectedPeerHandle == null || passphrase == null) {
            Log.e(TAG, "Cannot establish TCP: missing session, peer, or passphrase.");
            return;
        }

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(0);
                int port = serverSocket.getLocalPort();
                serverSocket.setSoTimeout(TCP_TIMEOUT_MS);
                Log.i(TAG, "TCP ServerSocket started on port " + port);

                WifiAwareNetworkSpecifier specifier = new WifiAwareNetworkSpecifier.Builder(
                        publishSession, connectedPeerHandle)
                        .setPskPassphrase(passphrase)
                        .setPort(port)
                        .build();

                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                        .setNetworkSpecifier(specifier)
                        .build();

                ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        Log.i(TAG, "Wi-Fi Aware network available, waiting for TCP connection...");
                        acceptTcpConnection();
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        Log.w(TAG, "Wi-Fi Aware network lost.");
                    }
                };
                cm.requestNetwork(request, networkCallback, mainHandler, TCP_TIMEOUT_MS);

            } catch (IOException e) {
                Log.e(TAG, "Failed to start TCP server: " + e.getMessage());
            }
        }).start();
    }

    private void acceptTcpConnection() {
        new Thread(() -> {
            try {
                Log.i(TAG, "Waiting for TCP connection...");
                tcpSocket = serverSocket.accept();
                tcpSocket.setSoTimeout(TCP_TIMEOUT_MS);
                Log.i(TAG, "TCP connection accepted from " + tcpSocket.getRemoteSocketAddress());

                // Read SessionEstablishment
                DataInputStream in = new DataInputStream(tcpSocket.getInputStream());
                int requestLen = in.readInt();
                byte[] requestData = new byte[requestLen];
                in.readFully(requestData);
                Log.i(TAG, "Received SessionEstablishment via TCP (" + requestLen + " bytes)");

                // Decrypt
                messageHandler.getSessionManager().decryptSessionEstablishment(requestData);

                mainHandler.post(() -> {
                    if (listener != null) listener.onRequestReceived(requestData);
                });
            } catch (Exception e) {
                Log.e(TAG, "TCP receive failed: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
            }
        }).start();
    }

    @Override
    public void sendResponse(byte[] response) {
        if (tcpSocket == null || tcpSocket.isClosed()) {
            Log.e(TAG, "Cannot send response: TCP socket not connected.");
            return;
        }

        Log.i(TAG, "Sending DeviceResponse via TCP (" + response.length + " bytes)");
        new Thread(() -> {
            try {
                DataOutputStream out = new DataOutputStream(tcpSocket.getOutputStream());
                out.writeInt(response.length);
                out.write(response);
                out.flush();
                Log.i(TAG, "TCP response sent successfully.");
            } catch (IOException e) {
                Log.e(TAG, "TCP send failed: " + e.getMessage());
            } finally {
                closeTcp();
            }
        }).start();
    }

    private void closeTcp() {
        try { if (tcpSocket != null) tcpSocket.close(); } catch (IOException ignored) {}
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        tcpSocket = null;
        serverSocket = null;
    }
}
