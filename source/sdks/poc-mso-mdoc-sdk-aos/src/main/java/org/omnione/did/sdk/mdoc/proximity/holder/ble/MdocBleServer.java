package org.omnione.did.sdk.mdoc.proximity.holder.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.security.PrivateKey;
import java.util.List;
import java.util.UUID;

import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocProximityListener;
import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocProximityServer;
import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocSessionManager;
import org.omnione.did.sdk.mdoc.proximity.holder.core.MdocTransferUtil;

/**
 * MdocBleServer implements the Peripheral role for mDoc offline data transfer.
 * It manages GATT services, advertising, and the secure transmission of the DeviceResponse.
 */
@SuppressLint("MissingPermission")
public class MdocBleServer implements MdocProximityServer {
    private static final String TAG = "BleServer";
    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothGattServer gattServer;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothDevice connectedDevice;
    private final UUID serviceUuid;
    private int mtu = 23; // Initial default MTU

    private final MdocBleMessageHandler messageHandler;
    private MdocProximityListener listener;

    public MdocBleServer(Context context, byte[] bleUuidBytes, PrivateKey privateKey, byte[] deviceEngagementBytes) {
        this.context = context;
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        this.serviceUuid = bytesToUuid(bleUuidBytes);
        this.messageHandler = new MdocBleMessageHandler(privateKey, deviceEngagementBytes);
    }

    @Override
    public void setListener(MdocProximityListener listener) {
        this.listener = listener;
    }

    @Override
    public void start() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is disabled or not supported.");
            return;
        }

        setupGattServer();
        startAdvertising();
    }

    @Override
    public void stop() {
        Log.i(TAG, "Stopping BLE Server...");
        if (advertiser != null) advertiser.stopAdvertising(advertiseCallback);
        if (gattServer != null) gattServer.close();
    }

    public MdocSessionManager getSessionManager() {
        return messageHandler.getSessionManager();
    }

    private void setupGattServer() {
        Log.d(TAG, "Setting up GATT Server...");
        gattServer = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).openGattServer(context, gattServerCallback);
        BluetoothGattService service = new BluetoothGattService(serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // State: Read/Write/Notify
        BluetoothGattCharacteristic stateChar = new BluetoothGattCharacteristic(
                BleConstants.CHARACTERISTIC_STATE_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE
        );
        stateChar.addDescriptor(new BluetoothGattDescriptor(BleConstants.DESC_CLIENT_CHAR_CONFIG, BluetoothGattDescriptor.PERMISSION_WRITE));
        stateChar.setValue(new byte[]{BleConstants.STATE_START});

        // Client2Server: Write without response
        BluetoothGattCharacteristic client2ServerChar = new BluetoothGattCharacteristic(
                BleConstants.CHARACTERISTIC_CLIENT_2_SERVER_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
        );

        // Server2Client: Notify
        BluetoothGattCharacteristic server2ClientChar = new BluetoothGattCharacteristic(
                BleConstants.CHARACTERISTIC_SERVER_2_CLIENT_UUID,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                0
        );
        server2ClientChar.addDescriptor(new BluetoothGattDescriptor(BleConstants.DESC_CLIENT_CHAR_CONFIG, BluetoothGattDescriptor.PERMISSION_WRITE));

        // IDENT: Read
        BluetoothGattCharacteristic identChar = new BluetoothGattCharacteristic(
                BleConstants.CHARACTERISTIC_IDENT_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
        );
        identChar.setValue(new byte[16]); 

        service.addCharacteristic(stateChar);
        service.addCharacteristic(client2ServerChar);
        service.addCharacteristic(server2ClientChar);
        service.addCharacteristic(identChar);

        gattServer.addService(service);
    }

    private void startAdvertising() {
        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(new ParcelUuid(serviceUuid))
                .build();

        Log.i(TAG, "Advertising BLE service: " + serviceUuid);
        advertiser.startAdvertising(settings, data, advertiseCallback);
    }

    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.i(TAG, "Device connected: " + device.getAddress());
                connectedDevice = device;
                if (listener != null) listener.onDeviceConnected();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i(TAG, "Device disconnected.");
                connectedDevice = null;
                if (listener != null) listener.onDeviceDisconnected();
                startAdvertising();
            }
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            MdocBleServer.this.mtu = 512; // Force to 512 as per Android 15+ trends for large transfers
            Log.d(TAG, "MTU updated to " + MdocBleServer.this.mtu + " for device " + device.getAddress());
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "Read request received for " + characteristic.getUuid());
            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if (BleConstants.CHARACTERISTIC_CLIENT_2_SERVER_UUID.equals(characteristic.getUuid())) {
                byte[] fullRequest = messageHandler.receiveChunk(value);

                if (fullRequest != null) {
                    Log.i(TAG, "Full request received via BLE (" + fullRequest.length + " bytes).");
                    if (listener != null) listener.onRequestReceived(fullRequest);
                }

                if (responseNeeded) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
                }
            } else if (BleConstants.CHARACTERISTIC_STATE_UUID.equals(characteristic.getUuid())) {
                characteristic.setValue(value);
                Log.d(TAG, "Reader updated State to: " + (value != null && value.length > 0 ? value[0] : "empty"));
                if (responseNeeded) gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if (BleConstants.DESC_CLIENT_CHAR_CONFIG.equals(descriptor.getUuid())) {
                Log.d(TAG, "Descriptor write: Client Characteristic Configuration updated.");
                if (responseNeeded) gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
        }
    };

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "BLE Advertising started successfully.");
        }
        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "BLE Advertising failed with error code: " + errorCode);
        }
    };

    /**
     * Sends the complete DeviceResponse by chunking it according to the negotiated MTU.
     */
    public void sendResponse(byte[] response) {
        if (connectedDevice == null || gattServer == null) {
            Log.e(TAG, "Cannot send response: No connected device.");
            return;
        }

        Log.i(TAG, "Sending DeviceResponse via BLE (" + response.length + " bytes)");

        BluetoothGattService service = gattServer.getService(serviceUuid);
        BluetoothGattCharacteristic charToNotify = service.getCharacteristic(BleConstants.CHARACTERISTIC_SERVER_2_CLIENT_UUID);

        int maxChunkSize = mtu - 3 - 1; 
        List<byte[]> chunks = MdocTransferUtil.chunkData(response, maxChunkSize);

        new Thread(() -> {
            try {
                int sentBytes = 0;
                for (byte[] chunk : chunks) {
                    charToNotify.setValue(chunk);
                    gattServer.notifyCharacteristicChanged(connectedDevice, charToNotify, false);
                    sentBytes += chunk.length - 1;
                    Log.d(TAG, "Sent BLE chunk. Header: " + chunk[0] + ", Progress: " + sentBytes + "/" + response.length);
                    Thread.sleep(25); // Throttling for stability
                }
                Log.i(TAG, "BLE Transfer complete.");

                // Update state to END
                BluetoothGattCharacteristic stateChar = service.getCharacteristic(BleConstants.CHARACTERISTIC_STATE_UUID);
                stateChar.setValue(new byte[]{BleConstants.STATE_END});
                gattServer.notifyCharacteristicChanged(connectedDevice, stateChar, false);
            } catch (InterruptedException e) {
                Log.e(TAG, "BLE transfer interrupted: " + e.getMessage());
            }
        }).start();
    }

    private UUID bytesToUuid(byte[] bytes) {
        long msb = 0, lsb = 0;
        for (int i=0; i<8; i++) msb = (msb << 8) | (bytes[i] & 0xff);
        for (int i=8; i<16; i++) lsb = (lsb << 8) | (bytes[i] & 0xff);
        return new UUID(msb, lsb);
    }
}
