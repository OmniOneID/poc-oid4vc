package org.omnione.did.sdk.mdoc.proximity.holder.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocProximityListener;
import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocProximityServer;
import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocSessionManager;
import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocTransferUtil;

import java.security.PrivateKey;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressLint("MissingPermission")
public class MdocBleCentralClient implements MdocProximityServer {
    private static final String TAG = "BleCentralClient";
    private static final int TARGET_MTU = 512;
    private static final long SCAN_TIMEOUT_MS = 30_000L;

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final UUID serviceUuid;
    private final MdocBleMessageHandler messageHandler;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private BluetoothLeScanner scanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic stateCharacteristic;
    private BluetoothGattCharacteristic clientToServerCharacteristic;
    private BluetoothGattCharacteristic serverToClientCharacteristic;
    private MdocProximityListener listener;
    private int mtu = 23;
    private boolean scanStopped;
    private final Queue<Runnable> gattOperations = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean gattOperationInProgress = new AtomicBoolean(false);

    public MdocBleCentralClient(Context context, byte[] bleUuidBytes, PrivateKey privateKey, byte[] deviceEngagementBytes) {
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
        this.serviceUuid = bytesToUuid(bleUuidBytes);
        this.messageHandler = new MdocBleMessageHandler(privateKey, deviceEngagementBytes);
    }

    @Override
    public void start() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is disabled or not supported.");
            return;
        }

        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            Log.e(TAG, "BLE scanner is not available.");
            return;
        }

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(serviceUuid))
                .build();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanStopped = false;
        try {
            scanner.startScan(Collections.singletonList(filter), settings, scanCallback);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to start BLE scan", e);
            return;
        }
        mainHandler.postDelayed(() -> {
            if (!scanStopped && bluetoothGatt == null) {
                Log.w(TAG, "BLE scan timed out.");
                stopScan();
            }
        }, SCAN_TIMEOUT_MS);
    }

    @Override
    public void stop() {
        stopScan();
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        gattOperations.clear();
        gattOperationInProgress.set(false);
    }

    @Override
    public void sendResponse(byte[] response) {
        if (bluetoothGatt == null || clientToServerCharacteristic == null) {
            Log.e(TAG, "Cannot send response: GATT client is not ready.");
            return;
        }

        int maxChunkSize = Math.max(20, mtu - 3 - 1);
        List<byte[]> chunks = MdocTransferUtil.chunkData(response, maxChunkSize);

        new Thread(() -> {
            try {
                int sentBytes = 0;
                for (byte[] chunk : chunks) {
                    clientToServerCharacteristic.setValue(chunk);
                    clientToServerCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    bluetoothGatt.writeCharacteristic(clientToServerCharacteristic);
                    sentBytes += chunk.length - 1;
                    Log.d(TAG, "Sent BLE chunk. Header: " + chunk[0] + ", Progress: " + sentBytes + "/" + response.length);
                    Thread.sleep(25L);
                }
                writeState(BleConstants.STATE_END);
            } catch (InterruptedException e) {
                Log.e(TAG, "BLE transfer interrupted", e);
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    public void setListener(MdocProximityListener listener) {
        this.listener = listener;
    }

    public MdocSessionManager getSessionManager() {
        return messageHandler.getSessionManager();
    }

    private void stopScan() {
        if (scanner != null && !scanStopped) {
            try {
                scanner.stopScan(scanCallback);
            } catch (Exception e) {
                Log.w(TAG, "Failed to stop BLE scan", e);
            }
            scanStopped = true;
        }
    }

    private void setupCharacteristics(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(serviceUuid);
        if (service == null) {
            Log.e(TAG, "BLE service not found: " + serviceUuid);
            return;
        }

        stateCharacteristic = service.getCharacteristic(BleConstants.READER_CHARACTERISTIC_STATE_UUID);
        clientToServerCharacteristic = service.getCharacteristic(BleConstants.READER_CHARACTERISTIC_CLIENT_2_SERVER_UUID);
        serverToClientCharacteristic = service.getCharacteristic(BleConstants.READER_CHARACTERISTIC_SERVER_2_CLIENT_UUID);

        if (stateCharacteristic != null) {
            queueEnableNotification(gatt, stateCharacteristic);
        }
        if (serverToClientCharacteristic != null) {
            queueEnableNotification(gatt, serverToClientCharacteristic);
        }
        queueWriteState(BleConstants.STATE_START);
        runNextGattOperation();
    }

    private void queueEnableNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        gattOperations.add(() -> {
            boolean notificationSet = gatt.setCharacteristicNotification(characteristic, true);
            if (!notificationSet) {
                Log.w(TAG, "Failed to enable local notification flag for " + characteristic.getUuid());
            }

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BleConstants.DESC_CLIENT_CHAR_CONFIG);
            if (descriptor == null) {
                onGattOperationCompleted();
                return;
            }

            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean started = gatt.writeDescriptor(descriptor);
            if (!started) {
                Log.w(TAG, "Failed to write CCCD for " + characteristic.getUuid());
                onGattOperationCompleted();
            }
        });
    }

    private void queueWriteState(byte state) {
        gattOperations.add(() -> writeStateInternal(state));
    }

    private void writeState(byte state) {
        if (bluetoothGatt == null || stateCharacteristic == null) {
            return;
        }
        if (gattOperationInProgress.get()) {
            queueWriteState(state);
            return;
        }
        gattOperations.add(() -> writeStateInternal(state));
        runNextGattOperation();
    }

    private void writeStateInternal(byte state) {
        if (bluetoothGatt == null || stateCharacteristic == null) {
            onGattOperationCompleted();
            return;
        }
        stateCharacteristic.setValue(new byte[]{state});
        stateCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        boolean started = bluetoothGatt.writeCharacteristic(stateCharacteristic);
        if (!started) {
            Log.w(TAG, "Failed to write state characteristic");
            onGattOperationCompleted();
        }
    }

    private void runNextGattOperation() {
        if (gattOperationInProgress.getAndSet(true)) {
            return;
        }

        Runnable operation = gattOperations.poll();
        if (operation == null) {
            gattOperationInProgress.set(false);
            return;
        }
        operation.run();
    }

    private void onGattOperationCompleted() {
        gattOperationInProgress.set(false);
        if (!gattOperations.isEmpty()) {
            runNextGattOperation();
        }
    }

    private UUID bytesToUuid(byte[] bytes) {
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (bytes[i] & 0xff);
        }
        return new UUID(msb, lsb);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            stopScan();
            BluetoothDevice device = result.getDevice();
            try {
                bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to connect GATT", e);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE scan failed: " + errorCode);
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (listener != null) {
                    listener.onDeviceConnected();
                }
                gatt.requestMtu(TARGET_MTU);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (listener != null) {
                    listener.onDeviceDisconnected();
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            MdocBleCentralClient.this.mtu = status == BluetoothGatt.GATT_SUCCESS ? Math.min(mtu, TARGET_MTU) : 23;
            gatt.discoverServices();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setupCharacteristics(gatt);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            onGattOperationCompleted();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            onGattOperationCompleted();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            handleIncomingCharacteristic(characteristic.getUuid(), value);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            handleIncomingCharacteristic(characteristic.getUuid(), characteristic.getValue());
        }
    };

    private void handleIncomingCharacteristic(UUID characteristicUuid, byte[] value) {
        if (value == null) {
            return;
        }

        if (BleConstants.READER_CHARACTERISTIC_STATE_UUID.equals(characteristicUuid)) {
            Log.d(TAG, "Reader updated State to: " + (value.length > 0 ? value[0] : "empty"));
            return;
        }

        if (!BleConstants.READER_CHARACTERISTIC_SERVER_2_CLIENT_UUID.equals(characteristicUuid)) {
            return;
        }

        byte[] fullRequest = messageHandler.receiveChunk(value);
        if (fullRequest != null && listener != null) {
            listener.onRequestReceived(fullRequest);
        }
    }
}
