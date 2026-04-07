package org.omnione.did.sdk.mdoc.proximity.holder.ble;

public enum BleMode {
    PERIPHERAL_SERVER("peripheral_server", "BLE Peripheral Server"),
    CENTRAL_CLIENT("central_client", "BLE Central Client");

    public static final String EXTRA_KEY = "BLE_MODE";

    private final String value;
    private final String displayName;

    BleMode(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static BleMode fromValue(String value) {
        for (BleMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        return PERIPHERAL_SERVER;
    }
}
