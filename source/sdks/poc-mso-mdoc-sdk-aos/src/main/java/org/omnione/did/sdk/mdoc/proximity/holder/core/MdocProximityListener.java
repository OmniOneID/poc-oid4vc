package org.omnione.did.sdk.mdoc.proximity.holder.core;

/**
 * Listener for events in proximity transport (BLE, NFC, WiFi)
 */
public interface MdocProximityListener {
    /**
     * Called when a connection is established with a reader
     */
    void onDeviceConnected();

    /**
     * Called when the connection is lost
     */
    void onDeviceDisconnected();

    /**
     * Called when a full request message is received from the reader
     * @param request The complete (usually encrypted) DeviceRequest
     */
    void onRequestReceived(byte[] request);
}
