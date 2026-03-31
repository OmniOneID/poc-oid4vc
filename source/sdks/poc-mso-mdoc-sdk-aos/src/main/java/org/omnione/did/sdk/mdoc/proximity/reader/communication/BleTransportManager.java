package org.omnione.did.sdk.mdoc.proximity.reader.communication;

import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import org.omnione.did.sdk.mdoc.proximity.reader.exception.MdocReaderErrorCode;
import org.omnione.did.sdk.mdoc.proximity.reader.exception.MdocReaderException;
import org.omnione.did.sdk.mdoc.proximity.reader.core.*;
import org.omnione.did.sdk.mdoc.proximity.reader.utility.ProtocolLogger;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BleTransportManager implements TransportManager {
    private static final String TAG = "MDR/BleTransport";
    private static final int MTU_SIZE = 512;
    private static final byte STATE_CHARACTERISTIC_START = 0x01;
    private static final byte STATE_CHARACTERISTIC_END = 0x02;

    // ISO 18013-5 Table A.1: BLE characteristic UUID
    // mdoc이 GATT 서버일 때 사용하는 UUID (Reader=Central, mdoc=Peripheral)
    private static final UUID MDOC_STATE_CHAR_UUID          = UUID.fromString("00000001-A123-48CE-896B-4C76973373E6");
    private static final UUID MDOC_CLIENT2SERVER_CHAR_UUID   = UUID.fromString("00000002-A123-48CE-896B-4C76973373E6");
    private static final UUID MDOC_SERVER2CLIENT_CHAR_UUID   = UUID.fromString("00000003-A123-48CE-896B-4C76973373E6");

    // mdoc Reader가 GATT 서버일 때 사용하는 UUID (Reader=Peripheral, mdoc=Central)
    private static final UUID READER_STATE_CHAR_UUID         = UUID.fromString("00000005-A123-48CE-896B-4C76973373E6");
    private static final UUID READER_CLIENT2SERVER_CHAR_UUID  = UUID.fromString("00000006-A123-48CE-896B-4C76973373E6");
    private static final UUID READER_SERVER2CLIENT_CHAR_UUID  = UUID.fromString("00000007-A123-48CE-896B-4C76973373E6");
    private static final UUID READER_IDENT_CHAR_UUID         = UUID.fromString("00000008-A123-48CE-896B-4C76973373E6");

    // 현재 연결에 사용할 활성 UUID (연결 모드에 따라 설정)
    private UUID stateCharUuid;
    private UUID client2serverCharUuid;
    private UUID server2clientCharUuid;

    private final Context context;
    private final boolean usePeripheralServerMode;
    private final UUID peripheralServerModeUuid;
    private final UUID centralClientModeUuid;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt gattClient;
    private BluetoothGattServer gattServer;
    private BluetoothDevice connectedDevice;
    private BluetoothLeScanner scanner;
    private BluetoothLeAdvertiser advertiser;

    private DeviceEngagement deviceEngagement;
    private SessionEncryption sessionEncryption;

    @Override
    public SessionEncryption getSessionEncryption() { return sessionEncryption; }

    private final List<TransferEvent.Listener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ByteArrayOutputStream incomingData = new ByteArrayOutputStream();

    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicInteger negotiatedMtu = new AtomicInteger(23);

    // BLE 스캔 타임아웃 및 GATT 연결 리트라이
    private static final long SCAN_TIMEOUT_MS = 30_000;
    private static final int MAX_GATT_RETRIES = 3;
    private static final long GATT_RETRY_DELAY_MS = 1_000;
    private final AtomicInteger gattRetryCount = new AtomicInteger(0);
    private BluetoothDevice lastScannedDevice = null;
    private UUID currentServiceUuid = null;

    private final GattOperationQueue gattQueue = new GattOperationQueue();

    // GATT 콜백 전용 스레드: binder thread에서 콜백이 동기 실행되면
    // 후속 notification의 binder transaction이 유실됨 (S25 등)
    // 전용 HandlerThread를 사용하면 binder thread가 즉시 해제되어 모든 notification 수신 가능
    private HandlerThread gattCallbackThread;
    private Handler gattCallbackHandler;

    public BleTransportManager(Context context, boolean usePeripheralServerMode,
                               UUID peripheralServerModeUuid,
                               UUID centralClientModeUuid) {
        this.context = context;
        this.usePeripheralServerMode = usePeripheralServerMode;
        this.peripheralServerModeUuid = peripheralServerModeUuid;
        this.centralClientModeUuid = centralClientModeUuid;
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            this.bluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    @Override
    public void addListener(TransferEvent.Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(TransferEvent.Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(TransferEvent event) {
        for (TransferEvent.Listener listener : listeners) {
            listener.onEvent(event);
        }
    }

    @Override
    public void startDeviceEngagement(EngagementSource source) {
        try {
            deviceEngagement = source.resolve();
            notifyListeners(TransferEvent.DeviceEngagementCompleted.INSTANCE);
            sessionEncryption = new SessionEncryption(
                deviceEngagement.getEDeviceKey(),
                deviceEngagement.getEncodedEngagement()
            );
            connectToDevice();
        } catch (Exception e) {
            Log.e(TAG, "Device engagement failed", e);
            notifyListeners(new TransferEvent.Error(e));
        }
    }

    private void connectToDevice() {
        notifyListeners(TransferEvent.Connecting.INSTANCE);

        // Peripheral Server 토글 ON + Holder가 Central Client 모드 지원 시 → Reader가 GATT 서버
        // Peripheral Server Mode에서는 Holder가 write request(ACK 기반)로 데이터 전송하므로
        // S25 등에서 발생하는 BLE notification 유실 문제를 근본적으로 회피
        // 그 외 → Central Client 모드 (기본)
        UUID targetUuid = null;
        if (usePeripheralServerMode && deviceEngagement.getCentralClientModeUuid() != null) {
            // Reader=Peripheral, mdoc=Central → Reader가 GATT 서버 → Reader UUID 사용 (5/6/7)
            setActiveUuids(true);
            targetUuid = deviceEngagement.getCentralClientModeUuid();
            ProtocolLogger.logBleConnecting("Peripheral Server Mode", targetUuid);
            startPeripheralServerMode(targetUuid);
        } else if (deviceEngagement.getPeripheralServerModeUuid() != null) {
            // Reader=Central, mdoc=Peripheral → mdoc이 GATT 서버 → mdoc UUID 사용 (1/2/3)
            setActiveUuids(false);
            targetUuid = deviceEngagement.getPeripheralServerModeUuid();
            ProtocolLogger.logBleConnecting("Central Client Mode", targetUuid);
            startCentralClientMode(targetUuid);
        } else {
            notifyListeners(new TransferEvent.Error(
                new MdocReaderException(MdocReaderErrorCode.BLE_NO_CONNECTION_METHOD)));
        }
    }

    // ISO 18013-5 Table A.1: 연결 모드에 따라 활성 characteristic UUID 설정
    // readerIsServer: true이면 Reader가 GATT 서버 (Peripheral Server Mode)
    private void setActiveUuids(boolean readerIsServer) {
        if (readerIsServer) {
            stateCharUuid        = READER_STATE_CHAR_UUID;
            client2serverCharUuid = READER_CLIENT2SERVER_CHAR_UUID;
            server2clientCharUuid = READER_SERVER2CLIENT_CHAR_UUID;
        } else {
            stateCharUuid        = MDOC_STATE_CHAR_UUID;
            client2serverCharUuid = MDOC_CLIENT2SERVER_CHAR_UUID;
            server2clientCharUuid = MDOC_SERVER2CLIENT_CHAR_UUID;
        }
        Log.d(TAG, "Active UUIDs set: readerIsServer=" + readerIsServer
            + " state=" + stateCharUuid + " c2s=" + client2serverCharUuid + " s2c=" + server2clientCharUuid);
    }

    private void startCentralClientMode(UUID serviceUuid) {
        if (bluetoothAdapter == null) {
            notifyListeners(new TransferEvent.Error(new MdocReaderException(MdocReaderErrorCode.BLE_NOT_AVAILABLE)));
            return;
        }

        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            notifyListeners(new TransferEvent.Error(new MdocReaderException(MdocReaderErrorCode.BLE_SCANNER_NOT_AVAILABLE)));
            return;
        }

        ScanFilter filter = new ScanFilter.Builder()
            .setServiceUuid(new ParcelUuid(serviceUuid))
            .build();

        ScanSettings settings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build();

        currentServiceUuid = serviceUuid;
        gattRetryCount.set(0);

        try {
            scanner.startScan(Collections.singletonList(filter), settings, scanCallback);
        } catch (SecurityException e) {
            notifyListeners(new TransferEvent.Error(e));
            return;
        }

        // 스캔 타임아웃: 지정 시간 내 디바이스를 찾지 못하면 에러
        mainHandler.postDelayed(() -> {
            if (!isConnected.get() && scanner != null) {
                try {
                    scanner.stopScan(scanCallback);
                } catch (SecurityException ignored) {}
                Log.w(TAG, "BLE 스캔 타임아웃 (" + SCAN_TIMEOUT_MS + "ms)");
                notifyListeners(new TransferEvent.Error(
                    new MdocReaderException(MdocReaderErrorCode.BLE_SCAN_FAILED, "Scan timeout")));
            }
        }, SCAN_TIMEOUT_MS);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            try {
                scanner.stopScan(this);
            } catch (SecurityException ignored) {}

            lastScannedDevice = result.getDevice();
            connectGatt(lastScannedDevice);
        }

        @Override
        public void onScanFailed(int errorCode) {
            notifyListeners(new TransferEvent.Error(
                new MdocReaderException(MdocReaderErrorCode.BLE_SCAN_FAILED, "error code: " + errorCode)));
        }
    };

    // GATT 연결 시도 (리트라이 포함)
    private void connectGatt(BluetoothDevice device) {
        Log.d(TAG, "GATT 연결 시도 (" + (gattRetryCount.get() + 1) + "/" + MAX_GATT_RETRIES + ")");
        try {
            // 전용 HandlerThread로 GATT 콜백 수신:
            // Handler 없이 connectGatt 호출 시 콜백이 binder thread에서 동기 실행되어
            // S25 등에서 빠른 연속 notification이 유실됨
            ensureGattCallbackThread();
            gattClient = device.connectGatt(context, false, gattCallback,
                BluetoothDevice.TRANSPORT_LE, BluetoothDevice.PHY_LE_1M, gattCallbackHandler);
        } catch (SecurityException e) {
            notifyListeners(new TransferEvent.Error(e));
        }
    }

    private void ensureGattCallbackThread() {
        if (gattCallbackThread == null || !gattCallbackThread.isAlive()) {
            gattCallbackThread = new HandlerThread("BLE-GATT-Callback");
            gattCallbackThread.start();
            gattCallbackHandler = new Handler(gattCallbackThread.getLooper());
        }
    }

    // GATT 연결 실패 시 리트라이
    private void retryGattConnection() {
        if (gattRetryCount.get() < MAX_GATT_RETRIES && lastScannedDevice != null) {
            int count = gattRetryCount.incrementAndGet();
            Log.w(TAG, "GATT 연결 재시도 " + count + "/" + MAX_GATT_RETRIES);
            // 기존 연결 정리
            if (gattClient != null) {
                try { gattClient.close(); } catch (Exception ignored) {}
                gattClient = null;
            }
            mainHandler.postDelayed(() -> connectGatt(lastScannedDevice), GATT_RETRY_DELAY_MS);
        } else {
            Log.e(TAG, "GATT 연결 리트라이 소진");
            notifyListeners(TransferEvent.Disconnected.INSTANCE);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gattRetryCount.set(0);
                try {
                    // S25 등에서 BLE notification 유실 방지:
                    // connection interval을 최소(7.5ms)로 요청하여 throughput 확보
                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                    gatt.requestMtu(MTU_SIZE);
                } catch (SecurityException e) {
                    notifyListeners(new TransferEvent.Error(e));
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (!isConnected.get()) {
                    // 연결 수립 전 끊어진 경우 → 리트라이
                    retryGattConnection();
                } else {
                    // 정상 연결 후 끊어진 경우 → Disconnected 이벤트
                    isConnected.set(false);
                    notifyListeners(TransferEvent.Disconnected.INSTANCE);
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            // MTU를 512로 캡: 일부 Holder가 512 초과 페이로드를 처리하지 못함
            negotiatedMtu.set(Math.min(mtu, MTU_SIZE));
            ProtocolLogger.logMtuNegotiated(mtu);
            try {
                gatt.discoverServices();
            } catch (SecurityException e) {
                notifyListeners(new TransferEvent.Error(e));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                isConnected.set(true);
                connectedDevice = gatt.getDevice();
                setupNotifications(gatt);
                // 모든 descriptor 쓰기 완료 후 Connected 이벤트 발생
            } else {
                notifyListeners(new TransferEvent.Error(
                    new MdocReaderException(MdocReaderErrorCode.BLE_SERVICE_DISCOVERY_FAILED, "status: " + status)));
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite: " + descriptor.getCharacteristic().getUuid() + " status=" + status);
            gattQueue.onOperationCompleted();
        }

        // API 33+: byte[] value 파라미터 직접 사용 (characteristic.getValue() race condition 방지)
        // S25 등 Android 13+ 기기에서 BLE 알림이 빠르게 연속 도착 시
        // 공유 characteristic 객체의 value가 덮어쓰여지는 문제 해결
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            UUID charUuid = characteristic.getUuid();
            if (charUuid.equals(stateCharUuid)) {
                handleStateUpdate(value);
            } else if (charUuid.equals(server2clientCharUuid)) {
                handleIncomingData(value);
            }
        }

        // API 32 이하 fallback
        @SuppressWarnings("deprecation")
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            UUID charUuid = characteristic.getUuid();
            if (charUuid.equals(stateCharUuid)) {
                handleStateUpdate(value);
            } else if (charUuid.equals(server2clientCharUuid)) {
                handleIncomingData(value);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite: " + characteristic.getUuid() + " status=" + status);
            gattQueue.onOperationCompleted();
        }
    };

    @SuppressWarnings("deprecation")
    private void setupNotifications(BluetoothGatt gatt) {
        try {
            BluetoothGattService service = gatt.getServices().stream()
                .filter(s -> s.getCharacteristics().stream()
                    .anyMatch(c -> c.getUuid().equals(stateCharUuid)))
                .findFirst()
                .orElse(null);

            if (service == null) {
                notifyListeners(new TransferEvent.Error(
                    new MdocReaderException(MdocReaderErrorCode.BLE_SERVICE_NOT_FOUND)));
                return;
            }

            BluetoothGattCharacteristic stateChar = service.getCharacteristic(stateCharUuid);
            BluetoothGattCharacteristic server2clientChar = service.getCharacteristic(server2clientCharUuid);
            BluetoothGattCharacteristic client2serverChar = service.getCharacteristic(client2serverCharUuid);

            ProtocolLogger.logGattServicesDiscovered(
                service.getUuid().toString(),
                stateChar != null,
                client2serverChar != null,
                server2clientChar != null);

            // descriptor 쓰기를 순차 큐잉 - Android BLE는 한 번에 하나의 GATT 작업만 허용
            if (stateChar != null) {
                gatt.setCharacteristicNotification(stateChar, true);
                BluetoothGattDescriptor desc = stateChar.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                if (desc != null) {
                    gattQueue.enqueue(() -> {
                        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(desc);
                    });
                }
            }
            if (server2clientChar != null) {
                gatt.setCharacteristicNotification(server2clientChar, true);
                BluetoothGattDescriptor desc = server2clientChar.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                if (desc != null) {
                    // Holder가 INDICATE를 지원하면 indication(ACK 기반) 우선 사용
                    // 작은 청크를 대량 전송하는 Holder에서 S25 등의 notification 유실 방지
                    int props = server2clientChar.getProperties();
                    boolean supportsIndicate = (props & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
                    byte[] cccdValue = supportsIndicate
                        ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                        : BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                    Log.d(TAG, "Server2Client CCCD: indicate=" + supportsIndicate
                        + " props=0x" + Integer.toHexString(props));
                    gattQueue.enqueue(() -> {
                        desc.setValue(cccdValue);
                        gatt.writeDescriptor(desc);
                    });
                }
            }

            // 모든 descriptor 쓰기 완료 후 State characteristic에 0x01 쓰기
            // ISO 18013-5에 따라 검증자가 연결되었음을 Holder에게 알림
            if (stateChar != null) {
                gattQueue.enqueue(() -> {
                    Log.d(TAG, "Writing STATE_CHARACTERISTIC_START to state char");
                    stateChar.setValue(new byte[]{STATE_CHARACTERISTIC_START});
                    stateChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    gatt.writeCharacteristic(stateChar);
                });
            }

            // State 쓰기 후 Connected 이벤트 발생
            gattQueue.enqueue(() -> {
                Log.d(TAG, "All setup completed, notifying Connected");
                notifyListeners(TransferEvent.Connected.INSTANCE);
                gattQueue.onOperationCompleted();
            });
        } catch (SecurityException e) {
            notifyListeners(new TransferEvent.Error(e));
        }
    }

    private void startPeripheralServerMode(UUID serviceUuid) {
        // Peripheral 서버 모드 구현
        // 이 모드에서 검증자가 GATT 서버로 동작
        if (bluetoothManager == null || bluetoothAdapter == null) {
            notifyListeners(new TransferEvent.Error(new MdocReaderException(MdocReaderErrorCode.BLE_NOT_AVAILABLE)));
            return;
        }

        try {
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback);
            if (gattServer == null) {
                notifyListeners(new TransferEvent.Error(new MdocReaderException(MdocReaderErrorCode.BLE_GATT_SERVER_FAILED)));
                return;
            }

            BluetoothGattService service = new BluetoothGattService(
                serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);

            // Reader가 GATT 서버 → Reader UUID 사용 (5/6/7)
            service.addCharacteristic(new BluetoothGattCharacteristic(
                stateCharUuid,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE));

            service.addCharacteristic(new BluetoothGattCharacteristic(
                client2serverCharUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE));

            service.addCharacteristic(new BluetoothGattCharacteristic(
                server2clientCharUuid,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ));

            gattServer.addService(service);

            // 광고 시작
            advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            if (advertiser != null) {
                AdvertiseSettings advSettings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setConnectable(true)
                    .setTimeout(0)
                    .build();

                AdvertiseData advData = new AdvertiseData.Builder()
                    .addServiceUuid(new ParcelUuid(serviceUuid))
                    .setIncludeDeviceName(false)
                    .build();

                advertiser.startAdvertising(advSettings, advData, advertiseCallback);
            }
        } catch (SecurityException e) {
            notifyListeners(new TransferEvent.Error(e));
        }
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "BLE advertising started");
        }

        @Override
        public void onStartFailure(int errorCode) {
            notifyListeners(new TransferEvent.Error(
                new MdocReaderException(MdocReaderErrorCode.BLE_ADVERTISING_FAILED, "error code: " + errorCode)));
        }
    };

    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device;
                isConnected.set(true);
                stopAdvertising();
                notifyListeners(TransferEvent.Connected.INSTANCE);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected.set(false);
                notifyListeners(TransferEvent.Disconnected.INSTANCE);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                BluetoothGattCharacteristic characteristic, boolean preparedWrite,
                boolean responseNeeded, int offset, byte[] value) {
            UUID charUuid = characteristic.getUuid();
            if (charUuid.equals(stateCharUuid)) {
                handleStateUpdate(value);
            } else if (charUuid.equals(client2serverCharUuid)) {
                handleIncomingData(value);
            }
            if (responseNeeded) {
                try {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                } catch (SecurityException ignored) {}
            }
        }
    };

    private void handleStateUpdate(byte[] value) {
        if (value == null || value.length == 0) return;
        ProtocolLogger.logBleStateUpdate(value[0]);
        if (value[0] == STATE_CHARACTERISTIC_END) {
            stopSession();
        }
    }

    private synchronized void handleIncomingData(byte[] chunk) {
        if (chunk == null || chunk.length == 0) return;

        // ISO 18013-5 BLE 청크 프로토콜:
        // 0x01 = 추가 데이터 있음, 0x00 = 마지막 청크
        boolean isLast = (chunk[0] == 0x00);

        // 데이터 추가 (제어 바이트인 첫 번째 바이트 건너뜀)
        incomingData.write(chunk, 1, chunk.length - 1);
        ProtocolLogger.logBleChunkReceived(chunk, isLast, incomingData.size());

        if (isLast) {
            byte[] completeData = incomingData.toByteArray();
            incomingData.reset();
            ProtocolLogger.logBleDataAssembled(completeData);
            processReceivedData(completeData);
        }
    }

    private void processReceivedData(byte[] data) {
        try {
            byte[] decrypted;
            byte[] sessionData = sessionEncryption.parseSessionData(data);
            if (sessionData == null) {
                notifyListeners(TransferEvent.Disconnected.INSTANCE);
                return;
            }
            decrypted = sessionEncryption.decryptResponse(sessionData);
            notifyListeners(new TransferEvent.ResponseReceived(decrypted));
            sendSessionTermination();
        } catch (Exception e) {
            Log.e(TAG, "Failed to process received data", e);
            notifyListeners(new TransferEvent.Error(e));
        }
    }

    // ISO 18013-5 8.3.3.1.1.5: 세션 종료 메시지 전송 후 전송 닫기
    private void sendSessionTermination() {
        try {
            if (sessionEncryption == null) return;
            byte[] termination = sessionEncryption.buildSessionTermination();
            Log.d(TAG, "Sending session termination (" + termination.length + " bytes)");
            sendData(termination);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send session termination", e);
        }
    }

    @Override
    public void sendRequest(byte[] deviceRequestBytes) {
        try {
            byte[] encrypted = sessionEncryption.encryptRequest(deviceRequestBytes);
            byte[] sessionEstablishment = sessionEncryption.buildSessionEstablishment(encrypted);
            sendData(sessionEstablishment);
            notifyListeners(TransferEvent.RequestSent.INSTANCE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send request", e);
            notifyListeners(new TransferEvent.Error(e));
        }
    }

    private void sendData(byte[] data) {
        int maxChunkSize = negotiatedMtu.get() - 3 - 1; // MTU - ATT overhead - control byte
        if (maxChunkSize <= 0) maxChunkSize = 20;

        List<byte[]> chunks = new ArrayList<>();
        int offset = 0;
        while (offset < data.length) {
            int chunkSize = Math.min(maxChunkSize, data.length - offset);
            byte[] chunk = new byte[chunkSize + 1];
            boolean moreDataComing = (offset + chunkSize < data.length);
            chunk[0] = moreDataComing ? (byte) 0x01 : (byte) 0x00;
            System.arraycopy(data, offset, chunk, 1, chunkSize);
            chunks.add(chunk);
            offset += chunkSize;
        }

        ProtocolLogger.logBleSendStart(data.length, chunks.size(), maxChunkSize);
        // GATT 큐 대신 mainHandler로 청크 간 간격을 두고 순차 전송
        // Android 13+ BLE 스택에서 NO_RESPONSE 콜백이 불안정하므로 타이머 기반 전송
        long chunkIntervalMs = 100;
        for (int i = 0; i < chunks.size(); i++) {
            final int chunkIndex = i;
            final int totalChunks = chunks.size();
            byte[] chunk = chunks.get(i);
            mainHandler.postDelayed(() -> {
                ProtocolLogger.logBleChunkSent(chunkIndex, totalChunks, chunk);
                writeChunk(chunk);
            }, (long) chunkIndex * chunkIntervalMs);
        }
    }

    @SuppressWarnings("deprecation")
    private void writeChunk(byte[] chunk) {
        try {
            if (gattClient != null) {
                // Central 클라이언트 모드: client2server characteristic에 쓰기
                BluetoothGattService service = gattClient.getServices().stream()
                    .filter(s -> s.getCharacteristic(client2serverCharUuid) != null)
                    .findFirst().orElse(null);
                if (service != null) {
                    BluetoothGattCharacteristic c2s = service.getCharacteristic(client2serverCharUuid);
                    if (c2s != null) {
                        // deprecated API 사용: Android 15에서도 NO_RESPONSE와 호환
                        c2s.setValue(chunk);
                        c2s.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                        gattClient.writeCharacteristic(c2s);
                    }
                }
            } else if (gattServer != null && connectedDevice != null) {
                // Peripheral 서버 모드: server2client characteristic으로 알림
                BluetoothGattService service = gattServer.getServices().stream()
                    .filter(s -> s.getCharacteristic(server2clientCharUuid) != null)
                    .findFirst().orElse(null);
                if (service != null) {
                    BluetoothGattCharacteristic s2c = service.getCharacteristic(server2clientCharUuid);
                    if (s2c != null) {
                        s2c.setValue(chunk);
                        gattServer.notifyCharacteristicChanged(connectedDevice, s2c, false);
                    }
                }
            }
        } catch (SecurityException e) {
            notifyListeners(new TransferEvent.Error(e));
        }
    }

    private void stopAdvertising() {
        if (advertiser != null) {
            try {
                advertiser.stopAdvertising(advertiseCallback);
            } catch (SecurityException ignored) {}
        }
    }

    @Override
    public void stopSession() {
        // ISO 18013-5 8.3.3.1.1.5: 닫기 전 State characteristic에 END 쓰기
        writeStateCharacteristic(STATE_CHARACTERISTIC_END);

        try {
            if (scanner != null) {
                scanner.stopScan(scanCallback);
            }
        } catch (SecurityException ignored) {}

        stopAdvertising();

        try {
            if (gattClient != null) {
                gattClient.close();
                gattClient = null;
            }
        } catch (Exception ignored) {}

        try {
            if (gattServer != null) {
                gattServer.close();
                gattServer = null;
            }
        } catch (Exception ignored) {}

        gattQueue.clear();
        isConnected.set(false);
        connectedDevice = null;
        incomingData.reset();

        if (gattCallbackThread != null) {
            gattCallbackThread.quitSafely();
            gattCallbackThread = null;
            gattCallbackHandler = null;
        }
    }

    @SuppressWarnings("deprecation")
    private void writeStateCharacteristic(byte state) {
        try {
            if (gattClient != null) {
                BluetoothGattService service = gattClient.getServices().stream()
                    .filter(s -> s.getCharacteristic(stateCharUuid) != null)
                    .findFirst().orElse(null);
                if (service != null) {
                    BluetoothGattCharacteristic stateChar = service.getCharacteristic(stateCharUuid);
                    if (stateChar != null) {
                        stateChar.setValue(new byte[]{state});
                        stateChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                        gattClient.writeCharacteristic(stateChar);
                    }
                }
            } else if (gattServer != null && connectedDevice != null) {
                BluetoothGattService service = gattServer.getServices().stream()
                    .filter(s -> s.getCharacteristic(stateCharUuid) != null)
                    .findFirst().orElse(null);
                if (service != null) {
                    BluetoothGattCharacteristic stateChar = service.getCharacteristic(stateCharUuid);
                    if (stateChar != null) {
                        stateChar.setValue(new byte[]{state});
                        gattServer.notifyCharacteristicChanged(connectedDevice, stateChar, false);
                    }
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "State characteristic 쓰기 실패", e);
        }
    }
}
