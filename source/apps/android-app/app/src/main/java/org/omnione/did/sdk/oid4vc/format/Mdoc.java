/*
 * Copyright 2025 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnione.did.sdk.oid4vc.format;

import android.content.Context;
import android.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.upokecenter.cbor.CBORObject;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.omnione.did.mdoc.core.oid4vp.OID4VPHandler;
import org.omnione.did.sdk.mdoc.util.LogUtil;
import org.omnione.did.sdk.oid4vc.util.CryptoUtil;

import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Security;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import COSE.AlgorithmID;
import COSE.Attribute;
import COSE.KeyKeys;
import COSE.OneKey;
import COSE.Sign1Message;

public class Mdoc {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    private static final String PRIVATE_KEY = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgmMOV8LmitIOKQCynSbCxsW0xmVMuQjdPtiJdjhwfx0agCgYIKoZIzj0DAQehRANCAAQv+cDbPA9aF/hQ0WIJyVJmfzr533/v+9xvCw+d/ptbZHTOhfDrj38GrJGQqxu4d1NswrAj+JlqA7Fhen34bWoT";

    /**
     * Checks if the given format is supported for mDoc.
     *
     * @param format The format string to check.
     * @return true if the format is supported, false otherwise.
     */
    public static boolean isSupported(String format) {
        return "mDL".equals(format) || "mDocPID".equals(format);
    }

    /**
     * Creates a selectively disclosed VP token for mDoc based on DCQL.
     *
     * @param mDoc The mDoc data in Base64 URL format.
     * @param selectedClaimsKeys The list of claim keys selected for disclosure.
     * @param selectedClaimsNamespaces The list of namespaces corresponding to the selected claim keys.
     * @param aud The audience for the VP token.
     * @param nonce The nonce for the VP token.
     * @param responseUri The response URI for the VP token.
     * @return A JSON string containing the generated VP token.
     * @throws Exception if an error occurs during token creation.
     */
    public static String createVpToken(String mDoc, List<String> selectedClaimsKeys, List<String> selectedClaimsNamespaces, String aud, String nonce, String responseUri) throws Exception {
        LogUtil.logLongString("sangjun", "mdoc: " + mDoc);
        PrivateKey holderPrivateKey = CryptoUtil.getPrivateKeyObject(java.util.Base64.getDecoder().decode(PRIVATE_KEY));

        byte[] mDocBytes = java.util.Base64.getUrlDecoder().decode(mDoc);
        CBORObject issuerSigned = CBORObject.DecodeFromBytes(mDocBytes);

        CBORObject nameSpaces = issuerSigned.get("nameSpaces");
        String namespaceStr = "";
        String docType = "";
        for (CBORObject keyObj : nameSpaces.getKeys()) {
            namespaceStr = keyObj.AsString();
        }
        if(namespaceStr.equals("eu.europa.ec.eudi.pid.1"))
            docType = "eu.europa.ec.eudi.pid.1";
        else if(namespaceStr.equals("org.iso.18013.5.1"))
            docType = "org.iso.18013.5.1.mDL";

        if (selectedClaimsKeys != null && !selectedClaimsKeys.isEmpty()) {
            CBORObject originalNameSpaces = issuerSigned.get("nameSpaces");
            if (originalNameSpaces != null) {
                CBORObject filteredNameSpaces = CBORObject.NewMap();
                for (CBORObject nsKey : originalNameSpaces.getKeys()) {
                    String currentNamespace = nsKey.AsString();
                    CBORObject items = originalNameSpaces.get(nsKey);
                    CBORObject filteredItems = CBORObject.NewArray();
                    
                    if (items != null && items.getType() == com.upokecenter.cbor.CBORType.Array) {
                        for (int i = 0; i < items.size(); i++) {
                            CBORObject itemBytesObj = items.get(i);
                            byte[] itemBytes = itemBytesObj.GetByteString();
                            CBORObject decodedItem = CBORObject.DecodeFromBytes(itemBytes);
                            
                            CBORObject elementIdObj = decodedItem.get("elementIdentifier");
                            if (elementIdObj != null) {
                                String elementIdentifier = elementIdObj.AsString();
                                
                                boolean isRequested = false;
                                for (int j = 0; j < selectedClaimsKeys.size(); j++) {
                                    String reqKey = selectedClaimsKeys.get(j);
                                    String reqNs = (selectedClaimsNamespaces != null && selectedClaimsNamespaces.size() > j) ? selectedClaimsNamespaces.get(j) : "";
                                    
                                    if (elementIdentifier.equals(reqKey) && currentNamespace.equals(reqNs)) {
                                        isRequested = true;
                                        break;
                                    }
                                }
                                
                                if (isRequested) {
                                    filteredItems.Add(itemBytesObj);
                                }
                            }
                        }
                    }
                    
                    if (filteredItems.size() > 0) {
                        filteredNameSpaces.Add(nsKey, filteredItems);
                    }
                }
                issuerSigned.set("nameSpaces", filteredNameSpaces);
            }
        }

        CBORObject document = CBORObject.NewMap();
        document.Add("docType", docType);
        document.Add("issuerSigned", issuerSigned);

        CBORObject deviceSigned = generateDeviceAuth(holderPrivateKey, docType, aud, nonce, responseUri);
        document.Add("deviceSigned", deviceSigned);

        CBORObject deviceResponse = CBORObject.NewMap();
        deviceResponse.Add("status", 0);
        deviceResponse.Add("version", "1.0");
        CBORObject documentsArray = CBORObject.NewArray();
        documentsArray.Add(document);
        deviceResponse.Add("documents", documentsArray);

        byte[] deviceResponseBytes = deviceResponse.EncodeToBytes();
        
        String vpToken = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(deviceResponseBytes);
        LogUtil.logLongString("sangjun", "vpToken: " + vpToken);
        String dcqlId = "query_0";
        return "{\"" + dcqlId + "\":[\"" + vpToken + "\"]}";
    }

    /**
     * Builds the session transcript for device authentication.
     *
     * @param aud The audience.
     * @param nonce The nonce.
     * @param responseUri The response URI.
     * @return The session transcript as a CBORObject.
     * @throws Exception if an error occurs during hashing or encoding.
     */
    private static CBORObject buildSessionTranscript(String aud, String nonce, String responseUri) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        CBORObject handoverInfo = CBORObject.NewArray();
        handoverInfo.Add(aud);
        handoverInfo.Add(nonce);
        handoverInfo.Add(CBORObject.Null);
        handoverInfo.Add(responseUri);

        byte[] handoverInfoByte = handoverInfo.EncodeToBytes();
        byte[] handoverInfoHash = sha256.digest(handoverInfoByte);

        CBORObject handover = CBORObject.NewArray();
        handover.Add("OpenID4VPHandover");
        handover.Add(handoverInfoHash);

        CBORObject sessionTranscript = CBORObject.NewArray();
        sessionTranscript.Add(CBORObject.Null);
        sessionTranscript.Add(CBORObject.Null);
        sessionTranscript.Add(handover);

        return sessionTranscript;
    }

    /**
     * Generates the device authentication CBOR object.
     *
     * @param privateKey The holder's private key.
     * @param docType The document type.
     * @param aud The audience.
     * @param nonce The nonce.
     * @param responseUri The response URI.
     * @return The device signed CBOR object.
     * @throws Exception if an error occurs during signing or encoding.
     */
    private static CBORObject generateDeviceAuth(PrivateKey privateKey, String docType, String aud, String nonce, String responseUri) throws Exception {
        CBORObject sessionTranscript = buildSessionTranscript(aud, nonce, responseUri);

        CBORObject emptyDeviceNameSpaces = CBORObject.NewMap();
        byte[] deviceNameSpacesBytes = emptyDeviceNameSpaces.EncodeToBytes();
        CBORObject deviceNameSpacesTagged = CBORObject.FromObject(deviceNameSpacesBytes).WithTag(24);

        CBORObject deviceAuth = CBORObject.NewArray();
        deviceAuth.Add("DeviceAuthentication");
        deviceAuth.Add(sessionTranscript);
        deviceAuth.Add(docType);
        deviceAuth.Add(deviceNameSpacesTagged);

        byte[] deviceAuthBytes = deviceAuth.EncodeToBytes();

        Sign1Message msg = new Sign1Message();
        msg.addAttribute(CBORObject.FromObject(1), CBORObject.FromObject(-7), Attribute.PROTECTED);
        OneKey devicePrivateKey = new OneKey(null, privateKey);
        msg.SetContent(CBORObject.FromObject(deviceAuthBytes).WithTag(24).EncodeToBytes());
        msg.sign(devicePrivateKey);

        CBORObject signatureObj = CBORObject.DecodeFromBytes(msg.EncodeToBytes());
        if (signatureObj.isTagged()) {
            signatureObj = signatureObj.Untag();
        }
        CBORObject deviceAuthMap = CBORObject.NewMap();
        deviceAuthMap.Add("deviceSignature", signatureObj);

        CBORObject deviceSigned = CBORObject.NewMap();
        deviceSigned.Add("deviceAuth", deviceAuthMap);

        return deviceSigned;
    }

    /**
     * Extracts claims from an mDL/mDoc Verifiable Credential.
     *
     * @param mDlData The mDoc data in Base64 format.
     * @return A map containing the extracted claims grouped by namespace.
     */
    public static Map<String, Object> getClaims(String mDlData) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            byte[] decodedBytes = Base64.decode(mDlData, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            ObjectMapper mapper = new ObjectMapper(new CBORFactory());
            Map<String, Object> map = mapper.readValue(decodedBytes, Map.class);
            
            if (map != null && map.containsKey("nameSpaces")) {
                Object nameSpacesObj = map.get("nameSpaces");
                if (nameSpacesObj instanceof Map) {
                    Map<String, Object> nameSpaces = (Map<String, Object>) nameSpacesObj;
                    for (Map.Entry<String, Object> entry : nameSpaces.entrySet()) {
                        String namespace = entry.getKey();
                        Map<String, Object> nsClaims = new LinkedHashMap<>();
                        Object itemsObj = entry.getValue();
                        if (itemsObj instanceof List) {
                            List<?> items = (List<?>) itemsObj;
                            for (Object item : items) {
                                if (item instanceof byte[]) {
                                    try {
                                        Map<String, Object> decodedItem = mapper.readValue((byte[]) item, Map.class);
                                        if (decodedItem.containsKey("elementIdentifier") && decodedItem.containsKey("elementValue")) {
                                            nsClaims.put((String) decodedItem.get("elementIdentifier"), decodedItem.get("elementValue"));
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                        result.put(namespace, nsClaims);
                    }
                }
            }
        } catch (Exception e) {
            // Error handling
        }
        return result;
    }


    /**
     * Creates a selectively disclosed VP token for mDoc using the SDK library.
     *
     * @param context The application context.
     * @param mDoc The mDoc data in Base64 URL format.
     * @param selectedClaimsKeys The list of claim keys selected for disclosure.
     * @param selectedClaimsNamespaces The list of namespaces corresponding to the selected claim keys.
     * @param aud The audience for the VP token.
     * @param nonce The nonce for the VP token.
     * @param responseUri The response URI for the VP token.
     * @return A JSON string containing the generated VP token.
     * @throws Exception if an error occurs during token creation.
     */
    public static String createVpToken2(Context context, String mDoc, List<String> selectedClaimsKeys, List<String> selectedClaimsNamespaces, String aud, String nonce, String responseUri) throws Exception {
        PrivateKey holderPrivateKey = CryptoUtil.getPrivateKeyObject(java.util.Base64.getDecoder().decode(PRIVATE_KEY));

        Map<String, Set<String>> requestedClaims = new java.util.HashMap<>();
        if (selectedClaimsKeys != null && selectedClaimsNamespaces != null) {
            for (int i = 0; i < selectedClaimsKeys.size(); i++) {
                String key = selectedClaimsKeys.get(i);
                String namespace = (selectedClaimsNamespaces.size() > i) ? selectedClaimsNamespaces.get(i) : "";

                if (namespace != null && !namespace.isEmpty()) {
                    if (!requestedClaims.containsKey(namespace)) {
                        requestedClaims.put(namespace, new java.util.HashSet<>());
                    }
                    requestedClaims.get(namespace).add(key);
                }
            }
        }
        String dcqlId = "query_0";
        String vpToken = OID4VPHandler.createVPTokenWithDcqlId(
                mDoc,
                requestedClaims,
                dcqlId,
                holderPrivateKey,
                aud,
                nonce,
                responseUri
        );

        LogUtil.logLongString("sangjun", "vpToken by mDoc SDK: " + vpToken);
        return "{\"" + dcqlId + "\":[\"" + vpToken + "\"]}";
    }
}
