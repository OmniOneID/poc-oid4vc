package org.omnione.did.sdk.mdoc.proximity.holder.wifi;

/**
 * Constants for WiFi Aware (Neighbor Awareness Networking)
 */
public class WifiAwareConstants {
    public static final String SERVICE_NAME = "mdoc";
    
    // ISO 18013-5 WiFi Aware Configuration Defaults
    public static final int DEFAULT_OPERATING_CLASS = 81; // 2.4GHz
    public static final int DEFAULT_CHANNEL = 6;
    
    // Message Chunking
    public static final int MAX_WIFI_AWARE_MESSAGE_SIZE = 255;
    public static final int MAX_CHUNK_PAYLOAD_SIZE = MAX_WIFI_AWARE_MESSAGE_SIZE - 1; // 254 bytes

    // Timing
    public static final int SEND_DELAY_MS = 50;
}
