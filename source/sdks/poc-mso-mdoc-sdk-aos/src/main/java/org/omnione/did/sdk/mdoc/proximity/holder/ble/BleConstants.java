package org.omnione.did.sdk.mdoc.proximity.holder.ble;

import java.nio.ByteBuffer;
import java.util.UUID;

public class BleConstants {
    public static final UUID SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");

    // Holder = GATT server (Reader = central client)
    public static final UUID CHARACTERISTIC_STATE_UUID = UUID.fromString("00000001-a123-48ce-896b-4c76973373e6");
    public static final UUID CHARACTERISTIC_CLIENT_2_SERVER_UUID = UUID.fromString("00000002-a123-48ce-896b-4c76973373e6");
    public static final UUID CHARACTERISTIC_SERVER_2_CLIENT_UUID = UUID.fromString("00000003-a123-48ce-896b-4c76973373e6");
    public static final UUID CHARACTERISTIC_IDENT_UUID = UUID.fromString("00000004-a123-48ce-896b-4c76973373e6");

    // Holder = central client (Reader = GATT server)
    public static final UUID READER_CHARACTERISTIC_STATE_UUID = UUID.fromString("00000005-a123-48ce-896b-4c76973373e6");
    public static final UUID READER_CHARACTERISTIC_CLIENT_2_SERVER_UUID = UUID.fromString("00000006-a123-48ce-896b-4c76973373e6");
    public static final UUID READER_CHARACTERISTIC_SERVER_2_CLIENT_UUID = UUID.fromString("00000007-a123-48ce-896b-4c76973373e6");
    public static final UUID READER_CHARACTERISTIC_IDENT_UUID = UUID.fromString("00000008-a123-48ce-896b-4c76973373e6");

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
        UUID uuid = UUID.randomUUID();
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }
}
