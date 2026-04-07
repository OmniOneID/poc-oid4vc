package org.omnione.did.sdk.mdoc.proximity.holder.ble;

import android.content.Context;
import android.content.SharedPreferences;

public final class BleModePreferences {
    private static final String PREFS_NAME = "offline_presentation_settings";
    private static final String KEY_BLE_MODE = "ble_mode";

    private BleModePreferences() {
    }

    public static BleMode getMode(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return BleMode.fromValue(preferences.getString(KEY_BLE_MODE, BleMode.PERIPHERAL_SERVER.getValue()));
    }

    public static void setMode(Context context, BleMode mode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BLE_MODE, mode.getValue())
                .apply();
    }
}
