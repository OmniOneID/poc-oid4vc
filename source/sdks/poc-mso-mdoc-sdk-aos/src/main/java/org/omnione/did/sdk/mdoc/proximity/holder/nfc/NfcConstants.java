package org.omnione.did.sdk.mdoc.proximity.holder.nfc;

/**
 * Constants for NFC (ISO 7816-4 & ISO 18013-5)
 */
public class NfcConstants {
    // AIDs
    public static final byte[] AID_ISO_18013_5 = {(byte)0xA0, 0x00, 0x00, 0x02, (byte)0x48, 0x04, 0x00};
    public static final byte[] AID_NDEF = {(byte)0xD2, 0x76, 0x00, 0x00, (byte)0x85, 0x01, 0x01};

    // File IDs
    public static final byte[] FILE_ID_CC = {(byte)0xE1, 0x03};
    public static final byte[] FILE_ID_NDEF = {(byte)0xE1, 0x04};

    // APDU Instructions
    public static final byte INS_SELECT = (byte)0xA4;
    public static final byte INS_READ_BINARY = (byte)0xB0;
    public static final byte INS_ENVELOPE = (byte)0xC2;
    public static final byte INS_GET_RESPONSE = (byte)0xC0;

    // Status Words (SW)
    public static final byte[] SW_SUCCESS = {(byte)0x90, 0x00};
    public static final byte SW_MORE_DATA = (byte)0x61;
    public static final byte SW_END_OF_DATA = (byte)0x90;
    public static final byte[] SW_FILE_NOT_FOUND = {(byte)0x6A, (byte)0x82};
    public static final byte[] SW_WRONG_LENGTH = {(byte)0x67, 0x00};
    public static final byte[] SW_UNKNOWN = {(byte)0x6F, 0x00};
    public static final byte[] SW_NO_DATA = {(byte)0x6A, (byte)0x80};
    public static final byte[] SW_CLA_NOT_SUPPORTED = {(byte)0x6E, 0x00};

    // Limits
    public static final int MAX_APDU_RESPONSE_SIZE = 0xFD; // ~253 bytes

    // NDEF Engagement
    public static final String NDEF_RECORD_TYPE = "iso.org:18013:deviceengagement";

    // Common CC File
    public static final byte[] CC_FILE = {
            0x00, 0x0F,       // CC File Length (15 bytes)
            0x20,             // Mapping Version 2.0
            0x00, (byte)0xFF, // MLe (255 bytes)
            0x00, (byte)0xFF, // MLc (255 bytes)
            0x04, 0x06,       // NDEF TLV
            (byte)0xE1, 0x04, // NDEF File ID
            0x08, 0x00,       // Max NDEF Size (2048 bytes)
            0x00,             // Read Access
            (byte)0xFF        // Write Access
    };
}
