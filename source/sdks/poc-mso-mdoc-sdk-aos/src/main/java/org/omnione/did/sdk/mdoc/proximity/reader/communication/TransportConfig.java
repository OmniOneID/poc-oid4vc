package org.omnione.did.sdk.mdoc.proximity.reader.communication;

import java.util.UUID;

public sealed interface TransportConfig {

    record Ble(boolean usePeripheralServerMode,
               UUID peripheralServerModeUuid,
               UUID centralClientModeUuid) implements TransportConfig {}

    record WifiAware(String passphrase, Integer channelInfo, Integer bandInfo) implements TransportConfig {}

    record Nfc(android.nfc.tech.IsoDep isoDep, int maxCommandDataLength, int maxResponseDataLength) implements TransportConfig {}
}
