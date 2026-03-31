package org.omnione.did.sdk.mdoc.proximity.holder.core;

/**
 * Interface for Proximity-based transport (BLE, NFC, WiFi)
 */
public interface MdocProximityServer {
    /**
     * Start the proximity transport (e.g., Advertising for BLE, Listening for NFC)
     */
    void start();

    /**
     * Stop the proximity transport
     */
    void stop();

    /**
     * Send the final response to the reader
     * @param response The complete (usually encrypted) DeviceResponse
     */
    void sendResponse(byte[] response);

    /**
     * Set a listener for connection and data events
     */
    void setListener(MdocProximityListener listener);
}
