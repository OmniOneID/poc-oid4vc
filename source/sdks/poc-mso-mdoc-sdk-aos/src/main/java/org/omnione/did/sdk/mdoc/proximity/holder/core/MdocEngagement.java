package org.omnione.did.sdk.mdoc.proximity.holder.core;

import android.util.Base64;
import android.util.Log;

import com.upokecenter.cbor.CBORObject;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.PrivateKey;
import java.security.Security;

import COSE.AlgorithmID;
import COSE.KeyKeys;
import COSE.OneKey;

import org.omnione.did.sdk.mdoc.proximity.holder.nfc.NfcConstants;
import org.omnione.did.sdk.mdoc.proximity.holder.wifi.WifiAwareConstants;

/**
 * Handles ISO 18013-5 Device Engagement.
 * Generates the payload used in QR codes or NFC NDEF records to initiate the proximity transaction.
 */
public class MdocEngagement {
    private static final String TAG = "MdocEngagement";
    private static String lastWifiAwarePassphrase;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Creates a DeviceEngagement CBOR object and returns it as a Base64URL encoded string with "mdoc:" prefix.
     * Includes multiple retrieval methods (NFC, BLE, WiFi Aware).
     *
     * @param eDeviceKeyBytes The ephemeral public key in COSE_Key format (wrapped in tag 24).
     * @param bleUuidBytes 16-byte UUID for BLE advertising.
     * @return The formatted QR payload string.
     */
    public static String createDeviceEngagementPayload(byte[] eDeviceKeyBytes, byte[] bleUuidBytes) {
        if (eDeviceKeyBytes == null) throw new IllegalArgumentException("eDeviceKeyBytes is null");

        Log.d(TAG, "Generating DeviceEngagement with multiple retrieval methods.");

        CBORObject deviceEngagement = CBORObject.NewMap();
        deviceEngagement.Add(0, "1.0"); // Version

        CBORObject security = CBORObject.NewArray();
        security.Add(1); // Cipher Suite: 1 (ECDH with P-256)
        security.Add(CBORObject.DecodeFromBytes(eDeviceKeyBytes));
        deviceEngagement.Add(1, security);

        CBORObject retrievalMethods = CBORObject.NewArray();

        // 1. NFC (Type: 1)
        CBORObject nfcMethod = CBORObject.NewArray();
        nfcMethod.Add(1);
        nfcMethod.Add(1);
        CBORObject nfcOptions = CBORObject.NewMap();
        nfcOptions.Add(0, 4096);  
        nfcOptions.Add(1, 32768); 
        nfcMethod.Add(nfcOptions);
        retrievalMethods.Add(nfcMethod);

        // 2. BLE (Type: 2)
        CBORObject bleMethod = CBORObject.NewArray();
        bleMethod.Add(2);
        bleMethod.Add(1);
        CBORObject bleOptions = CBORObject.NewMap();
        bleOptions.Add(0, true);  // supportsPeripheralServerMode
        bleOptions.Add(1, false); // supportsCentralClientMode
        bleOptions.Add(10, bleUuidBytes);
        bleMethod.Add(bleOptions);
        retrievalMethods.Add(bleMethod);

        // 3. Wi-Fi Aware (Type: 3)
        CBORObject wifiMethod = CBORObject.NewArray();
        wifiMethod.Add(3);
        wifiMethod.Add(1);
        CBORObject wifiOptions = CBORObject.NewMap();
        lastWifiAwarePassphrase = java.util.UUID.randomUUID().toString();
        wifiOptions.Add(0, lastWifiAwarePassphrase); // Passphrase
        wifiOptions.Add(1, WifiAwareConstants.DEFAULT_OPERATING_CLASS);
        wifiOptions.Add(2, WifiAwareConstants.DEFAULT_CHANNEL);
        wifiOptions.Add(3, CBORObject.FromObject(new byte[]{(byte)0x01})); 
        wifiMethod.Add(wifiOptions);
        retrievalMethods.Add(wifiMethod);

        deviceEngagement.Add(2, retrievalMethods);

        byte[] cborData = deviceEngagement.EncodeToBytes();
        String base64UrlEncoded = Base64.encodeToString(cborData, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        
        Log.i(TAG, "DeviceEngagement payload created successfully.");
        return "mdoc:" + base64UrlEncoded;
    }

    public static String getLastWifiAwarePassphrase() {
        return lastWifiAwarePassphrase;
    }

    /**
     * Creates a DeviceEngagement payload containing only BLE retrieval method for EUDI Wallet compatibility.
     *
     * @param eDeviceKeyBytes The ephemeral public key in COSE_Key format.
     * @param bleUuidBytes 16-byte UUID for BLE advertising.
     * @return The formatted QR payload string.
     */
    public static String createDeviceEngagementPayloadForEudi(byte[] eDeviceKeyBytes, byte[] bleUuidBytes) {
        if (eDeviceKeyBytes == null) throw new IllegalArgumentException("eDeviceKeyBytes is null");

        Log.d(TAG, "Generating EUDI-compatible DeviceEngagement (BLE only).");

        CBORObject deviceEngagement = CBORObject.NewMap();
        deviceEngagement.Add(0, "1.0"); 

        CBORObject security = CBORObject.NewArray();
        security.Add(1);
        security.Add(CBORObject.DecodeFromBytes(eDeviceKeyBytes));
        deviceEngagement.Add(1, security);

        CBORObject retrievalMethods = CBORObject.NewArray();

        // BLE (Type: 2)
        CBORObject bleMethod = CBORObject.NewArray();
        bleMethod.Add(2);
        bleMethod.Add(1);
        CBORObject bleOptions = CBORObject.NewMap();
        bleOptions.Add(0, true);
        bleOptions.Add(1, false);
        bleOptions.Add(10, bleUuidBytes);
        bleMethod.Add(bleOptions);
        retrievalMethods.Add(bleMethod);

        deviceEngagement.Add(2, retrievalMethods);

        byte[] cborData = deviceEngagement.EncodeToBytes();
        String base64UrlEncoded = Base64.encodeToString(cborData, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        
        Log.i(TAG, "EUDI-compatible DeviceEngagement created successfully.");
        return "mdoc:" + base64UrlEncoded;
    }

    /**
     * Creates an ephemeral key pair for the mdoc session.
     * @return EphemeralKeyHolder containing both private key and encoded public key bytes.
     */
    public static EphemeralKeyHolder generateEDeviceKeyBytes() throws Exception {
        Log.d(TAG, "Generating ephemeral key pair for session...");
        EphemeralKeyHolder holder = new EphemeralKeyHolder();
        OneKey ephemeralKey = OneKey.generateKey(AlgorithmID.ECDSA_256);
        holder.privateKey = ephemeralKey.AsPrivateKey();

        CBORObject publicCoseKey = CBORObject.NewMap();
        publicCoseKey.Add(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_EC2);
        publicCoseKey.Add(KeyKeys.EC2_Curve.AsCBOR(), KeyKeys.EC2_P256);
        publicCoseKey.Add(KeyKeys.EC2_X.AsCBOR(), ephemeralKey.get(KeyKeys.EC2_X));
        publicCoseKey.Add(KeyKeys.EC2_Y.AsCBOR(), ephemeralKey.get(KeyKeys.EC2_Y));

        byte[] encodedCoseKey = publicCoseKey.EncodeToBytes();
        holder.eDeviceKeyBytes = CBORObject.FromObjectAndTag(encodedCoseKey, 24).EncodeToBytes();

        return holder;
    }
}
