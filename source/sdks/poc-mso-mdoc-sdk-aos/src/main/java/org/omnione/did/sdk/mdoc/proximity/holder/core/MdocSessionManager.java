package org.omnione.did.sdk.mdoc.proximity.holder.core;

import android.util.Log;
import com.upokecenter.cbor.CBORObject;
import org.omnione.did.sdk.mdoc.util.LogUtil;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import COSE.Attribute;
import COSE.OneKey;
import COSE.Sign1Message;

/**
 * Handles ISO 18013-5 Session security, including encryption, decryption, and transcript management.
 * This class is transport-agnostic and manages the secure channel between the mDoc and the Reader.
 */
public class MdocSessionManager {
    private static final String TAG = "MdocSession";

    private final PrivateKey eDevicePrivateKey;
    private final byte[] deviceEngagementBytes;
    private CBORObject eReaderKey;
    private CBORObject handover = CBORObject.Null;

    /**
     * @param eDevicePrivateKey The ephemeral private key generated for this session.
     * @param deviceEngagementBytes The raw CBOR bytes of the DeviceEngagement (used for salt).
     */
    public MdocSessionManager(PrivateKey eDevicePrivateKey, byte[] deviceEngagementBytes) {
        this.eDevicePrivateKey = eDevicePrivateKey;
        this.deviceEngagementBytes = deviceEngagementBytes;
        Log.d(TAG, "Session Manager initialized with ephemeral key.");
    }

    public void setHandover(CBORObject handover) {
        this.handover = handover != null ? handover : CBORObject.Null;
    }

    /**
     * Decrypts the SessionEstablishment message from the reader and derives session keys.
     * @param message The encrypted SessionEstablishment message.
     * @return The decrypted DeviceRequest CBOR object.
     */
    public CBORObject decryptSessionEstablishment(byte[] message) {
        try {
            CBORObject sessionEstablishment = CBORObject.DecodeFromBytes(message);
            this.eReaderKey = sessionEstablishment.get("eReaderKey");
            CBORObject data = sessionEstablishment.get("data");

            if (eReaderKey == null || data == null) {
                Log.e(TAG, "Invalid SessionEstablishment: missing eReaderKey or data.");
                return null;
            }
            byte[] encryptedDeviceRequest = data.GetByteString();

            Log.i(TAG, "Deriving Session Keys (ECDH)");

            // 1. ECDH Shared Secret (Z)
            PublicKey eReaderPublicKey = getPublicKeyFromCoseKey(eReaderKey);
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(eDevicePrivateKey);
            ka.doPhase(eReaderPublicKey, true);
            byte[] rawZ = ka.generateSecret();
            byte[] sharedSecretZ = normalizeSharedSecret(rawZ);

            // 2. Session Transcript & Salt
            byte[] salt = generateSalt(deviceEngagementBytes, eReaderKey, handover);

            // 3. HKDF for SKReader
            byte[] skReader = deriveKey(sharedSecretZ, salt, "SKReader");

            // 4. AES-GCM Decrypt
            byte[] iv = new byte[12];
            iv[11] = 1; // Reader identifier=0, counter=1

            byte[] decryptedBytes = aesGcmDecrypt(skReader, iv, encryptedDeviceRequest);
            CBORObject deviceRequest = CBORObject.DecodeFromBytes(decryptedBytes);

            Log.i(TAG, "Session established. Reader request decrypted successfully.");
            return deviceRequest;

        } catch (Exception e) {
            Log.e(TAG, "CRITICAL: Decryption failed during session establishment: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generates and encrypts the DeviceResponse based on the requested claims.
     */
    public byte[] generateDeviceResponse(String mDoc, List<String> selectedClaimsKeys, List<String> selectedClaimsNamespaces, PrivateKey holderPrivateKey) throws Exception {
        if (eReaderKey == null) {
            throw new IllegalStateException("Session not established. Decrypt SessionEstablishment first.");
        }

        Log.i(TAG, "Generating Secure DeviceResponse");

        // 1. Parse and Filter mDoc
        byte[] mDocBytes = java.util.Base64.getUrlDecoder().decode(mDoc);
        CBORObject issuerSigned = CBORObject.DecodeFromBytes(mDocBytes);

        CBORObject nameSpaces = issuerSigned.get("nameSpaces");
        String namespaceStr = "";
        String docType = "";
        for (CBORObject keyObj : nameSpaces.getKeys()) {
            namespaceStr = keyObj.AsString();
        }
        
        if (namespaceStr.equals("eu.europa.ec.eudi.pid.1")) docType = "eu.europa.ec.eudi.pid.1";
        else if (namespaceStr.equals("org.iso.18013.5.1")) docType = "org.iso.18013.5.1.mDL";

        // Filtering logic
        if (selectedClaimsKeys != null && !selectedClaimsKeys.isEmpty()) {
            Log.d(TAG, "Filtering claims. Requested count: " + selectedClaimsKeys.size());
            CBORObject filteredNameSpaces = CBORObject.NewMap();
            for (CBORObject nsKey : nameSpaces.getKeys()) {
                String currentNamespace = nsKey.AsString();
                CBORObject items = nameSpaces.get(nsKey);
                CBORObject filteredItems = CBORObject.NewArray();

                if (items != null && items.getType() == com.upokecenter.cbor.CBORType.Array) {
                    for (int i = 0; i < items.size(); i++) {
                        CBORObject itemBytesObj = items.get(i);
                        byte[] itemBytes = itemBytesObj.GetByteString();
                        CBORObject decodedItem = CBORObject.DecodeFromBytes(itemBytes);
                        String elementIdentifier = decodedItem.get("elementIdentifier").AsString();

                        boolean isRequested = false;
                        for (int j = 0; j < selectedClaimsKeys.size(); j++) {
                            if (elementIdentifier.equals(selectedClaimsKeys.get(j)) && 
                                currentNamespace.equals(selectedClaimsNamespaces.get(j))) {
                                isRequested = true;
                                break;
                            }
                        }
                        if (isRequested) filteredItems.Add(itemBytesObj);
                    }
                }
                if (filteredItems.size() > 0) filteredNameSpaces.Add(nsKey, filteredItems);
            }
            issuerSigned.set("nameSpaces", filteredNameSpaces);
        }

        // 2. Generate DeviceAuth (Signature)
        CBORObject sessionTranscript = CBORObject.NewArray();
        sessionTranscript.Add(CBORObject.FromObject(deviceEngagementBytes).WithTag(24));
        sessionTranscript.Add(eReaderKey);
        sessionTranscript.Add(handover);

        CBORObject deviceAuth = generateDeviceAuth(holderPrivateKey, docType, sessionTranscript);

        CBORObject document = CBORObject.NewMap();
        document.Add("docType", docType);
        document.Add("issuerSigned", issuerSigned);
        document.Add("deviceSigned", deviceAuth);

        CBORObject deviceResponse = CBORObject.NewMap();
        deviceResponse.Add("version", "1.0");
        CBORObject documentsArray = CBORObject.NewArray();
        documentsArray.Add(document);
        deviceResponse.Add("documents", documentsArray);
        deviceResponse.Add("status", 0);

        byte[] deviceResponseBytes = deviceResponse.EncodeToBytes();

        // 3. Encrypt DeviceResponse
        Log.d(TAG, "Encrypting response with SKDevice...");
        PublicKey eReaderPublicKey = getPublicKeyFromCoseKey(eReaderKey);
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(eDevicePrivateKey);
        ka.doPhase(eReaderPublicKey, true);
        byte[] rawZ = ka.generateSecret();
        byte[] sharedSecretZ = normalizeSharedSecret(rawZ);

        byte[] salt = generateSalt(deviceEngagementBytes, eReaderKey, handover);
        byte[] skDevice = deriveKey(sharedSecretZ, salt, "SKDevice");

        byte[] iv = new byte[12];
        iv[7] = 1; iv[11] = 1; // Device identifier=1, counter=1
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(skDevice, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] encryptedResponse = cipher.doFinal(deviceResponseBytes);

        // 4. Wrap in SessionData
        CBORObject sessionData = CBORObject.NewMap();
        sessionData.Add("data", encryptedResponse);
        sessionData.Add("status", 20); // Session Termination

        Log.i(TAG, "DeviceResponse generated and encrypted (" + encryptedResponse.length + " bytes).");
        return sessionData.EncodeToBytes();
    }

    private CBORObject generateDeviceAuth(PrivateKey holderPrivateKey, String docType, CBORObject sessionTranscript) throws Exception {
        CBORObject emptyNameSpaces = CBORObject.FromObject(CBORObject.NewMap().EncodeToBytes()).WithTag(24);

        CBORObject deviceAuthentication = CBORObject.NewArray();
        deviceAuthentication.Add("DeviceAuthentication");
        deviceAuthentication.Add(sessionTranscript);
        deviceAuthentication.Add(docType);
        deviceAuthentication.Add(emptyNameSpaces);

        byte[] deviceAuthBytes = CBORObject.FromObjectAndTag(deviceAuthentication.EncodeToBytes(), 24).EncodeToBytes();

        Sign1Message msg = new Sign1Message();
        msg.addAttribute(CBORObject.FromObject(1), CBORObject.FromObject(-7), Attribute.PROTECTED); // ES256
        OneKey devicePrivateKey = new OneKey(null, holderPrivateKey);
        msg.SetContent(deviceAuthBytes);
        msg.sign(devicePrivateKey);

        CBORObject signatureObj = CBORObject.DecodeFromBytes(msg.EncodeToBytes());
        if (signatureObj.isTagged()) signatureObj = signatureObj.Untag();
        signatureObj.set(2, CBORObject.Null);

        CBORObject deviceSigned = CBORObject.NewMap();
        CBORObject deviceAuthMap = CBORObject.NewMap();
        deviceAuthMap.Add("deviceSignature", signatureObj);
        deviceSigned.Add("nameSpaces", emptyNameSpaces);
        deviceSigned.Add("deviceAuth", deviceAuthMap);

        return deviceSigned;
    }

    public static byte[] normalizeSharedSecret(byte[] rawZ) {
        byte[] sharedSecretZ = new byte[32];
        if (rawZ.length > 32) System.arraycopy(rawZ, rawZ.length - 32, sharedSecretZ, 0, 32);
        else if (rawZ.length < 32) System.arraycopy(rawZ, 0, sharedSecretZ, 32 - rawZ.length, rawZ.length);
        else sharedSecretZ = rawZ;
        return sharedSecretZ;
    }

    public static byte[] generateSalt(byte[] engagement, CBORObject eReaderKey, CBORObject handover) throws Exception {
        CBORObject sessionTranscript = CBORObject.NewArray();
        sessionTranscript.Add(CBORObject.FromObject(engagement).WithTag(24));
        sessionTranscript.Add(eReaderKey);
        sessionTranscript.Add(handover != null ? handover : CBORObject.Null);

        byte[] encodedTranscriptArray = sessionTranscript.EncodeToBytes();
        CBORObject taggedTranscript = CBORObject.FromObjectAndTag(CBORObject.FromObject(encodedTranscriptArray), 24);
        byte[] finalTranscriptBytes = taggedTranscript.EncodeToBytes();

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(finalTranscriptBytes);
    }

    public static byte[] deriveKey(byte[] z, byte[] salt, String infoStr) {
        byte[] info = infoStr.getBytes(StandardCharsets.UTF_8);
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(z, salt, info));
        byte[] key = new byte[32];
        hkdf.generateBytes(key, 0, 32);
        return key;
    }

    public static byte[] aesGcmDecrypt(byte[] key, byte[] iv, byte[] encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        return cipher.doFinal(encrypted);
    }

    public static PublicKey getPublicKeyFromCoseKey(CBORObject coseKeyObj) throws Exception {
        CBORObject coseKeyMap = coseKeyObj.isTagged() ? CBORObject.DecodeFromBytes(coseKeyObj.Untag().GetByteString()) : coseKeyObj;
        byte[] xBytes = coseKeyMap.get(CBORObject.FromObject(-2)).GetByteString();
        byte[] yBytes = coseKeyMap.get(CBORObject.FromObject(-3)).GetByteString();

        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);

        ECPublicKeySpec pubSpec = new ECPublicKeySpec(new ECPoint(new BigInteger(1, xBytes), new BigInteger(1, yBytes)), ecParameters);
        return KeyFactory.getInstance("EC").generatePublic(pubSpec);
    }

    public CBORObject getEReaderKey() {
        return eReaderKey;
    }
}
