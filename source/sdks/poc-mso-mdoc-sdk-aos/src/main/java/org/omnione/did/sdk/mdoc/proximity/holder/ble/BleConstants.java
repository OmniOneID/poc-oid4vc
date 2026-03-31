package org.omnione.did.sdk.mdoc.proximity.holder.ble;

import java.nio.ByteBuffer;
import java.util.UUID;

public class BleConstants {
    public static final UUID SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");

    // ISO/IEC 18013-5 표준 Characteristic UUIDs
    public static final UUID CHARACTERISTIC_STATE_UUID = UUID.fromString("00000001-a123-48ce-896b-4c76973373e6");
    public static final UUID CHARACTERISTIC_CLIENT_2_SERVER_UUID = UUID.fromString("00000002-a123-48ce-896b-4c76973373e6");
    public static final UUID CHARACTERISTIC_SERVER_2_CLIENT_UUID = UUID.fromString("00000003-a123-48ce-896b-4c76973373e6");
    public static final UUID CHARACTERISTIC_IDENT_UUID = UUID.fromString("00000004-a123-48ce-896b-4c76973373e6");

    // Descriptor UUID for Notifications (BLE 표준)
    public static final UUID DESC_CLIENT_CHAR_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // State values
    public static final byte STATE_START = 0x01;
    public static final byte STATE_END = 0x02;

    /**
     * Generates a random 16-byte UUID for BLE communication.
     * @return 16-byte array representing the UUID.
     */
    public static byte[] generateRandomBleUuid() {
        byte[] uuidBytes = new byte[16];
        new java.security.SecureRandom().nextBytes(uuidBytes);
        return uuidBytes;
    }
//    public static byte[] generateRandomBleUuid() {
//        // 1. RFC 4122 Variant 1 및 Version 4 규격을 충족하는 랜덤 UUID 생성
//        UUID uuid = UUID.randomUUID();
//
//        // 2. 16바이트 배열로 변환 (Big-Endian 바이트 순서 적용)
//        ByteBuffer bb = ByteBuffer.wrap(new byte[2]);
//        bb.putLong(uuid.getMostSignificantBits());
//        bb.putLong(uuid.getLeastSignificantBits());
//
//        return bb.array();
//    }
}
