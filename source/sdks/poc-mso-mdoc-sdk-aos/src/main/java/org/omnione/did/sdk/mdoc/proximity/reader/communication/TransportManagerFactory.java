package org.omnione.did.sdk.mdoc.proximity.reader.communication;

import android.content.Context;
import android.os.Build;

public class TransportManagerFactory {

    public static TransportManager create(Context context, TransportConfig config) {
        if (config instanceof TransportConfig.Ble ble) {
            return new BleTransportManager(
                context,
                ble.usePeripheralServerMode(),
                ble.peripheralServerModeUuid(),
                ble.centralClientModeUuid()
            );
        } else if (config instanceof TransportConfig.WifiAware) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return new WifiAwareTransportManager(context);
            }
            throw new IllegalArgumentException("Wi-Fi Aware requires Android Q (API 29) or higher");
        } else if (config instanceof TransportConfig.Nfc nfc) {
            return new NfcTransportManager(nfc.isoDep(), nfc.maxCommandDataLength(), nfc.maxResponseDataLength());
        }
        throw new IllegalArgumentException("Unsupported transport config: " + config.getClass());
    }
}
