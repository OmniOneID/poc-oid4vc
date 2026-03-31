package org.omnione.did.sdk.mdoc.proximity.reader.core;

import android.util.Base64;
import android.util.Log;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import org.omnione.did.sdk.mdoc.proximity.reader.utility.ProtocolLogger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.*;

public class DeviceResponseParser {
    private static final String TAG = "MDR/ResponseParser";

    public static ParsedResponse parse(byte[] deviceResponseBytes) throws Exception {
        return parse(deviceResponseBytes, null);
    }

    public static ParsedResponse parse(byte[] deviceResponseBytes, SessionEncryption sessionEncryption) throws Exception {
        ProtocolLogger.logDeviceResponseParseStart(deviceResponseBytes);

        CBORObject response = CBORObject.DecodeFromBytes(deviceResponseBytes);

        List<ParsedDocument> documents = new ArrayList<>();

        CBORObject docsItem = response.get(CBORObject.FromObject("documents"));
        int totalDocs = 0;
        if (docsItem != null && docsItem.getType() == CBORType.Array) {
            totalDocs = docsItem.size();
            int docIndex = 0;
            for (int i = 0; i < docsItem.size(); i++) {
                CBORObject docItem = docsItem.get(i);
                if (docItem.getType() == CBORType.Map) {
                    documents.add(parseDocument(docItem, sessionEncryption, docIndex, totalDocs));
                }
                docIndex++;
            }
        }

        return new ParsedResponse(documents);
    }

    private static ParsedDocument parseDocument(CBORObject docMap, SessionEncryption sessionEncryption, int docIndex, int totalDocs) throws Exception {
        String docType = "";
        java.util.Map<String, java.util.Map<String, Object>> namespacedClaims = new LinkedHashMap<>();
        IssuerSignedInfo issuerSignedInfo = null;

        CBORObject docTypeItem = docMap.get(CBORObject.FromObject("docType"));
        if (docTypeItem != null && docTypeItem.getType() == CBORType.TextString) {
            docType = docTypeItem.AsString();
        }
        ProtocolLogger.logDocumentParsing(docType, docIndex, totalDocs);

        // issuerSigned 파싱
        Boolean dataIntegrityIntact = null;
        CBORObject issuerSignedItem = docMap.get(CBORObject.FromObject("issuerSigned"));
        if (issuerSignedItem != null && issuerSignedItem.getType() == CBORType.Map) {
            CBORObject issuerSigned = issuerSignedItem;

            // MSO digest를 얻기 위해 issuerAuth를 먼저 파싱
            CBORObject issuerAuthItem = issuerSigned.get(CBORObject.FromObject("issuerAuth"));
            if (issuerAuthItem != null) {
                issuerSignedInfo = parseIssuerAuth(issuerAuthItem);
            }

            // nameSpaces 파싱 및 MSO digest 대비 데이터 무결성 검증
            CBORObject nsItem = issuerSigned.get(CBORObject.FromObject("nameSpaces"));
            if (nsItem != null && nsItem.getType() == CBORType.Map) {
                CBORObject nsMap = nsItem;

                // MSO에서 valueDigests 추출
                java.util.Map<String, java.util.Map<Long, byte[]>> msoDigests = null;
                String digestAlgorithm = "SHA-256";
                if (issuerSignedInfo != null) {
                    try {
                        MsoDigestInfo mdi = extractMsoDigests(issuerSignedInfo.getCoseSign1Items());
                        if (mdi != null) {
                            msoDigests = mdi.digests;
                            digestAlgorithm = mdi.algorithm;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to extract MSO digests", e);
                    }
                }

                boolean allDigestsMatch = true;
                boolean hasDigests = false;

                for (CBORObject nsKey : nsMap.getKeys()) {
                    String namespace = nsKey.AsString();
                    java.util.Map<String, Object> claims = new LinkedHashMap<>();
                    CBORObject nsValue = nsMap.get(nsKey);

                    java.util.Map<Long, byte[]> nsDigests = (msoDigests != null) ? msoDigests.get(namespace) : null;

                    if (nsValue != null && nsValue.getType() == CBORType.Array) {
                        for (int i = 0; i < nsValue.size(); i++) {
                            CBORObject issuerSignedItemData = nsValue.get(i);
                            parseIssuerSignedItem(issuerSignedItemData, claims);

                            // MSO digest 사용 가능 시 digest 검증
                            if (nsDigests != null && issuerSignedItemData.getType() == CBORType.ByteString) {
                                hasDigests = true;
                                if (!verifyItemDigest(issuerSignedItemData, nsDigests, digestAlgorithm)) {
                                    allDigestsMatch = false;
                                }
                            }
                        }
                    }
                    namespacedClaims.put(namespace, claims);
                    ProtocolLogger.logNamespaceClaims(namespace, claims);
                }

                if (hasDigests) {
                    dataIntegrityIntact = allDigestsMatch;
                    int totalItems = 0;
                    for (java.util.Map<Long, byte[]> nd : msoDigests.values()) totalItems += nd.size();
                    ProtocolLogger.logDigestOverall(digestAlgorithm, totalItems, allDigestsMatch);
                }
            }
        }

        // deviceSignature 검증을 위해 MSO(issuerAuth 내)에서 deviceKey 추출
        ECPublicKey deviceKey = null;
        if (issuerSignedInfo != null) {
            try {
                deviceKey = extractDeviceKeyFromMso(issuerSignedInfo.getCoseSign1Items());
            } catch (Exception e) {
                Log.w(TAG, "Failed to extract deviceKey from MSO", e);
            }
        }

        // deviceSigned 파싱 및 DeviceAuth 검증
        Boolean deviceSignatureValid = null;
        CBORObject deviceSignedItem = docMap.get(CBORObject.FromObject("deviceSigned"));
        if (deviceSignedItem != null && deviceSignedItem.getType() == CBORType.Map && sessionEncryption != null) {
            try {
                deviceSignatureValid = verifyDeviceAuth(
                    deviceSignedItem,
                    docType,
                    sessionEncryption,
                    deviceKey);
            } catch (Exception e) {
                Log.w(TAG, "DeviceAuth verification failed", e);
                deviceSignatureValid = false;
            }
        }

        return new ParsedDocument(docType, namespacedClaims, issuerSignedInfo, deviceSignatureValid, dataIntegrityIntact);
    }

    // ISO 18013-5 섹션 9.1.3에 따라 DeviceAuth 검증
    // EMacKey를 사용한 COSE_Mac0 (deviceMac) 검증 지원
    private static boolean verifyDeviceAuth(
            CBORObject deviceSigned,
            String docType,
            SessionEncryption sessionEncryption,
            ECPublicKey deviceKey) throws Exception {

        CBORObject deviceAuthItem = deviceSigned.get(CBORObject.FromObject("deviceAuth"));
        if (deviceAuthItem == null || deviceAuthItem.getType() != CBORType.Map) return false;
        CBORObject deviceAuth = deviceAuthItem;

        // CBOR 원본 바이트를 직접 결합하여 DeviceAuthenticationBytes 구성
        // CBOR 라이브러리의 인코딩 차이를 방지하기 위해 디코드→재인코딩을 피함
        // DeviceAuthentication = ["DeviceAuthentication", SessionTranscript, DocType, DeviceNameSpacesBytes]
        // DeviceAuthenticationBytes = #6.24(bstr .cbor DeviceAuthentication)

        // 1. "DeviceAuthentication" 문자열 인코딩
        byte[] labelBytes = CBORObject.FromObject("DeviceAuthentication").EncodeToBytes();

        // 2. SessionTranscript - 원본 인코딩 바이트 직접 사용 (디코드→재인코딩 없이)
        byte[] sessionTranscriptBytes = sessionEncryption.getEncodedSessionTranscript();

        // 3. DocType 문자열 인코딩
        byte[] docTypeBytes = CBORObject.FromObject(docType).EncodeToBytes();

        // 4. DeviceNameSpacesBytes - deviceSigned에서 태그된 바이트 문자열 재인코딩
        CBORObject nameSpacesItem = deviceSigned.get(CBORObject.FromObject("nameSpaces"));
        byte[] nameSpacesBytesEncoded;
        if (nameSpacesItem != null) {
            nameSpacesBytesEncoded = nameSpacesItem.EncodeToBytes();
        } else {
            // Tag(24, bstr(0xa0)), 0xa0 = CBOR 빈 맵
            CBORObject emptyTagged = CBORObject.FromObjectAndTag(CBORObject.FromObject(new byte[]{(byte) 0xa0}), 24);
            nameSpacesBytesEncoded = emptyTagged.EncodeToBytes();
        }

        // 5. DeviceAuthentication CBOR 배열 수동 구성: 헤더 + 연결된 요소들
        ByteArrayOutputStream daOs = new ByteArrayOutputStream();
        daOs.write(0x84); // CBOR array of 4 items
        daOs.write(labelBytes);
        daOs.write(sessionTranscriptBytes);
        daOs.write(docTypeBytes);
        daOs.write(nameSpacesBytesEncoded);
        byte[] encodedDeviceAuth = daOs.toByteArray();

        // 6. Tag(24, bstr(...))로 래핑 → DeviceAuthenticationBytes
        CBORObject taggedDa = CBORObject.FromObjectAndTag(CBORObject.FromObject(encodedDeviceAuth), 24);
        byte[] deviceAuthenticationBytes = taggedDa.EncodeToBytes();

        // deviceSignature (COSE_Sign1) 먼저 확인
        CBORObject deviceSigItem = deviceAuth.get(CBORObject.FromObject("deviceSignature"));
        if (deviceSigItem != null && deviceSigItem.getType() == CBORType.Array && deviceKey != null) {
            ProtocolLogger.logDeviceAuth("COSE_Sign1 (deviceSignature)", deviceAuthenticationBytes);
            return verifyDeviceSignature(deviceSigItem, deviceAuthenticationBytes, deviceKey);
        }

        // deviceMac (COSE_Mac0) 확인
        CBORObject deviceMacItem = deviceAuth.get(CBORObject.FromObject("deviceMac"));
        if (deviceMacItem != null && deviceMacItem.getType() == CBORType.Array) {
            ProtocolLogger.logDeviceAuth("COSE_Mac0 (deviceMac)", deviceAuthenticationBytes);
            return verifyDeviceMac(deviceMacItem, deviceAuthenticationBytes, sessionEncryption.getEMacKey());
        }

        Log.w(TAG, "Neither deviceSignature nor deviceMac found in deviceAuth");
        return false;
    }

    // COSE_Sign1 검증: [protected, unprotected, payload, signature]
    // Sig_structure: ["Signature1", protected, external_aad, payload]
    // external_aad = deviceAuthenticationBytes, payload = 비어있음(분리)
    private static boolean verifyDeviceSignature(CBORObject coseSign1, byte[] deviceAuthenticationBytes, ECPublicKey deviceKey) throws Exception {
        if (coseSign1.size() < 4) return false;

        byte[] protectedHeaders = (coseSign1.get(0).getType() == CBORType.ByteString)
            ? coseSign1.get(0).GetByteString() : new byte[0];
        byte[] signature = (coseSign1.get(3).getType() == CBORType.ByteString)
            ? coseSign1.get(3).GetByteString() : null;
        if (signature == null) return false;

        // Sig_structure 구성: ["Signature1", body_protected, external_aad, payload]
        // external_aad = 빈값, payload = deviceAuthenticationBytes (분리된 콘텐츠)
        CBORObject sigStructure = CBORObject.NewArray();
        sigStructure.Add(CBORObject.FromObject("Signature1"));
        sigStructure.Add(CBORObject.FromObject(protectedHeaders));
        sigStructure.Add(CBORObject.FromObject(new byte[0]));           // external_aad = empty
        sigStructure.Add(CBORObject.FromObject(deviceAuthenticationBytes)); // payload = detached content

        byte[] sigStructureBytes = sigStructure.EncodeToBytes();

        // protected header에서 알고리즘 결정
        String algorithm = resolveCoseAlgorithm(protectedHeaders);

        // Java 검증을 위해 원시 R||S 서명을 DER 형식으로 변환
        byte[] derSignature = rawToDer(signature);

        try {
            Signature sig = Signature.getInstance(algorithm);
            sig.initVerify(deviceKey);
            sig.update(sigStructureBytes);
            boolean valid = sig.verify(derSignature);
            ProtocolLogger.logSignatureVerification("DeviceAuth COSE_Sign1", algorithm, valid);
            return valid;
        } catch (Exception e) {
            Log.e(TAG, "Signature verify exception", e);
            return false;
        }
    }

    // 원시 ECDSA 서명 (R||S)을 DER 인코딩으로 변환
    private static byte[] rawToDer(byte[] raw) {
        int len = raw.length / 2;
        BigInteger r = new BigInteger(1, Arrays.copyOfRange(raw, 0, len));
        BigInteger s = new BigInteger(1, Arrays.copyOfRange(raw, len, raw.length));
        byte[] rBytes = r.toByteArray();
        byte[] sBytes = s.toByteArray();
        int totalLen = rBytes.length + sBytes.length + 4;
        byte[] der = new byte[totalLen + 2];
        der[0] = 0x30;
        der[1] = (byte) totalLen;
        der[2] = 0x02;
        der[3] = (byte) rBytes.length;
        System.arraycopy(rBytes, 0, der, 4, rBytes.length);
        der[4 + rBytes.length] = 0x02;
        der[5 + rBytes.length] = (byte) sBytes.length;
        System.arraycopy(sBytes, 0, der, 6 + rBytes.length, sBytes.length);
        return der;
    }

    // COSE_Mac0 검증: [protected, unprotected, payload, tag]
    private static boolean verifyDeviceMac(CBORObject coseMac0, byte[] deviceAuthenticationBytes, byte[] eMacKey) throws Exception {
        if (coseMac0.size() < 4) return false;

        byte[] protectedHeaders;
        if (coseMac0.get(0).getType() == CBORType.ByteString) {
            protectedHeaders = coseMac0.get(0).GetByteString();
        } else {
            protectedHeaders = new byte[0];
        }

        byte[] tagInResponse;
        if (coseMac0.get(3).getType() == CBORType.ByteString) {
            tagInResponse = coseMac0.get(3).GetByteString();
        } else {
            return false;
        }

        // MAC_structure 구성: ["MAC0", protected, external_aad, payload]
        // RFC 9052 섹션 6.3 및 ISO 18013-5 섹션 9.1.3.5 기반:
        //   external_aad = 빈값, payload = DeviceAuthenticationBytes (분리된 콘텐츠)
        CBORObject macStructure = CBORObject.NewArray();
        macStructure.Add(CBORObject.FromObject("MAC0"));
        macStructure.Add(CBORObject.FromObject(protectedHeaders));
        macStructure.Add(CBORObject.FromObject(new byte[0]));              // external_aad = empty
        macStructure.Add(CBORObject.FromObject(deviceAuthenticationBytes)); // payload = DeviceAuthenticationBytes

        byte[] macStructureBytes = macStructure.EncodeToBytes();

        // 기대 MAC 태그 계산
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(eMacKey, "HmacSHA256"));
        byte[] expectedTag = mac.doFinal(macStructureBytes);

        boolean valid = Arrays.equals(expectedTag, tagInResponse);
        ProtocolLogger.logMacVerification(expectedTag, tagInResponse, valid);
        return valid;
    }

    private static void parseIssuerSignedItem(CBORObject item, java.util.Map<String, Object> claims) throws Exception {
        byte[] encodedItem;
        if (item.getType() == CBORType.ByteString) {
            encodedItem = item.GetByteString();
        } else {
            return;
        }

        CBORObject claimMap = CBORObject.DecodeFromBytes(encodedItem);
        CBORObject idItem = claimMap.get(CBORObject.FromObject("elementIdentifier"));
        CBORObject valItem = claimMap.get(CBORObject.FromObject("elementValue"));

        if (idItem != null && idItem.getType() == CBORType.TextString) {
            String key = idItem.AsString();
            Object value = cborToJava(valItem);
            claims.put(key, value);
        }
    }

    private static IssuerSignedInfo parseIssuerAuth(CBORObject issuerAuth) {
        try {
            // COSE_Sign1 구조: [protected, unprotected, payload, signature]
            CBORObject coseSign1;
            if (issuerAuth.getType() == CBORType.Array) {
                coseSign1 = issuerAuth;
            } else {
                return null;
            }

            if (coseSign1.size() < 4) return null;

            byte[] protectedHeaderBytes = (coseSign1.get(0).getType() == CBORType.ByteString)
                ? coseSign1.get(0).GetByteString() : new byte[0];

            byte[] payload;
            if (coseSign1.get(2).getType() == CBORType.ByteString) {
                payload = coseSign1.get(2).GetByteString();
            } else {
                return null;
            }

            byte[] signature = (coseSign1.get(3).getType() == CBORType.ByteString)
                ? coseSign1.get(3).GetByteString() : null;

            ProtocolLogger.logIssuerAuth(protectedHeaderBytes, payload, signature != null ? signature : new byte[0]);

            // payload에서 MSO 파싱
            CBORObject mso = decodeMsoFromPayload(payload);
            if (mso == null) return null;

            String signed = null, validFrom = null, validUntil = null;
            CBORObject validityInfo = mso.get(CBORObject.FromObject("validityInfo"));
            if (validityInfo != null && validityInfo.getType() == CBORType.Map) {
                CBORObject vi = validityInfo;
                CBORObject s = vi.get(CBORObject.FromObject("signed"));
                CBORObject vf = vi.get(CBORObject.FromObject("validFrom"));
                CBORObject vu = vi.get(CBORObject.FromObject("validUntil"));

                if (s != null && s.getType() == CBORType.TextString) signed = s.AsString();
                if (vf != null && vf.getType() == CBORType.TextString) validFrom = vf.AsString();
                if (vu != null && vu.getType() == CBORType.TextString) validUntil = vu.AsString();
            }
            ProtocolLogger.logMsoValidity(signed, validFrom, validUntil);

            // 발급자 서명 (COSE_Sign1) 검증
            Boolean issuerSignatureValid = null;
            if (signature != null) {
                issuerSignatureValid = verifyIssuerSignature(
                    protectedHeaderBytes, payload, signature, coseSign1);
            }

            return new IssuerSignedInfo(signed, validFrom, validUntil, coseSign1, issuerSignatureValid);
        } catch (Exception e) {
            Log.w(TAG, "parseIssuerAuth failed", e);
            return null;
        }
    }

    // issuerAuth COSE_Sign1 서명 검증
    // unprotected header의 x5chain에서 서명 키 추출
    private static boolean verifyIssuerSignature(
            byte[] protectedHeaderBytes, byte[] payload, byte[] signature,
            CBORObject coseSign1) {
        try {
            // unprotected header에서 x5chain 추출 (COSE 라벨 33)
            CBORObject unprotectedItem = coseSign1.get(1);
            ECPublicKey signingKey = null;

            if (unprotectedItem != null && unprotectedItem.getType() == CBORType.Map) {
                CBORObject unprotected = unprotectedItem;
                // x5chain은 라벨 33 (정수 키)에 위치
                CBORObject x5chainItem = unprotected.get(CBORObject.FromObject(33));
                if (x5chainItem == null) {
                    // 문자열 키 "x5chain" 시도
                    x5chainItem = unprotected.get(CBORObject.FromObject("x5chain"));
                }
                if (x5chainItem != null) {
                    byte[] certBytes = null;
                    if (x5chainItem.getType() == CBORType.ByteString) {
                        certBytes = x5chainItem.GetByteString();
                    } else if (x5chainItem.getType() == CBORType.Array) {
                        // 인증서 배열에서 첫 번째 사용
                        if (x5chainItem.size() > 0 && x5chainItem.get(0).getType() == CBORType.ByteString) {
                            certBytes = x5chainItem.get(0).GetByteString();
                        }
                    }
                    if (certBytes != null) {
                        java.security.cert.CertificateFactory cf =
                            java.security.cert.CertificateFactory.getInstance("X.509");
                        java.security.cert.X509Certificate cert =
                            (java.security.cert.X509Certificate) cf.generateCertificate(
                                new ByteArrayInputStream(certBytes));
                        signingKey = (ECPublicKey) cert.getPublicKey();
                        ProtocolLogger.logIssuerCertificate(
                            cert.getSubjectX500Principal().toString(),
                            cert.getIssuerX500Principal().toString());
                    }
                }
            }

            if (signingKey == null) {
                Log.w(TAG, "No signing key found in issuerAuth unprotected headers");
                return false;
            }

            // protected header에서 알고리즘 결정
            String algorithm = resolveCoseAlgorithm(protectedHeaderBytes);

            // Sig_structure 구성: ["Signature1", protected, external_aad(빈값), payload]
            // issuerAuth의 경우: payload 포함 (분리되지 않음)
            CBORObject sigStructure = CBORObject.NewArray();
            sigStructure.Add(CBORObject.FromObject("Signature1"));
            sigStructure.Add(CBORObject.FromObject(protectedHeaderBytes));
            sigStructure.Add(CBORObject.FromObject(new byte[0])); // external_aad = empty
            sigStructure.Add(CBORObject.FromObject(payload));       // payload included

            byte[] sigStructureBytes = sigStructure.EncodeToBytes();

            byte[] derSignature = rawToDer(signature);

            Signature sig = Signature.getInstance(algorithm);
            sig.initVerify(signingKey);
            sig.update(sigStructureBytes);
            boolean valid = sig.verify(derSignature);
            ProtocolLogger.logSignatureVerification("IssuerAuth COSE_Sign1", algorithm, valid);
            return valid;
        } catch (Exception e) {
            Log.w(TAG, "Issuer signature verification failed", e);
            return false;
        }
    }

    // protected header에서 COSE 알고리즘 ID를 파싱하여 JCA 알고리즘명 반환
    private static String resolveCoseAlgorithm(byte[] protectedHeaders) {
        if (protectedHeaders == null || protectedHeaders.length == 0) return "SHA256withECDSA";
        try {
            CBORObject ph = CBORObject.DecodeFromBytes(protectedHeaders);
            if (ph.getType() == CBORType.Map) {
                CBORObject algItem = ph.get(CBORObject.FromObject(1));
                if (algItem != null && algItem.getType() == CBORType.Integer) {
                    long algId = algItem.AsInt64();
                    if (algId == -7) return "SHA256withECDSA";
                    else if (algId == -35) return "SHA384withECDSA";
                    else if (algId == -36) return "SHA512withECDSA";
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse protected headers for algorithm", e);
        }
        return "SHA256withECDSA";
    }

    // COSE_Sign1 payload 바이트에서 MSO Map 디코딩 (Tag 24 존재 시 언래핑)
    private static CBORObject decodeMsoFromPayload(byte[] payload) throws Exception {
        if (payload == null) return null;
        CBORObject msoItem = CBORObject.DecodeFromBytes(payload);
        if (msoItem.HasMostOuterTag(24) && msoItem.getType() == CBORType.ByteString) {
            msoItem = CBORObject.DecodeFromBytes(msoItem.GetByteString());
        }
        if (msoItem.getType() != CBORType.Map) return null;
        return msoItem;
    }

    private static class MsoDigestInfo {
        java.util.Map<String, java.util.Map<Long, byte[]>> digests;
        String algorithm;
        MsoDigestInfo(java.util.Map<String, java.util.Map<Long, byte[]>> digests, String algorithm) {
            this.digests = digests;
            this.algorithm = algorithm;
        }
    }

    // MSO에서 valueDigests 및 digestAlgorithm 추출
    private static MsoDigestInfo extractMsoDigests(CBORObject coseSign1) throws Exception {
        if (coseSign1 == null || coseSign1.size() < 3) return null;
        byte[] payload = (coseSign1.get(2).getType() == CBORType.ByteString)
            ? coseSign1.get(2).GetByteString() : null;
        if (payload == null) return null;

        CBORObject mso = decodeMsoFromPayload(payload);
        if (mso == null) return null;

        // 다이제스트 알고리즘
        String algorithm = "SHA-256";
        CBORObject algItem = mso.get(CBORObject.FromObject("digestAlgorithm"));
        if (algItem != null && algItem.getType() == CBORType.TextString) {
            String algStr = algItem.AsString();
            if ("SHA-256".equals(algStr)) algorithm = "SHA-256";
            else if ("SHA-384".equals(algStr)) algorithm = "SHA-384";
            else if ("SHA-512".equals(algStr)) algorithm = "SHA-512";
        }

        // valueDigests: 네임스페이스별 다이제스트 맵
        java.util.Map<String, java.util.Map<Long, byte[]>> digests = new LinkedHashMap<>();
        CBORObject vdItem = mso.get(CBORObject.FromObject("valueDigests"));
        if (vdItem != null && vdItem.getType() == CBORType.Map) {
            CBORObject vdMap = vdItem;
            for (CBORObject nsKey : vdMap.getKeys()) {
                String ns = nsKey.AsString();
                java.util.Map<Long, byte[]> nsDigests = new LinkedHashMap<>();
                CBORObject nsVal = vdMap.get(nsKey);
                if (nsVal != null && nsVal.getType() == CBORType.Map) {
                    CBORObject digestMap = nsVal;
                    for (CBORObject dKey : digestMap.getKeys()) {
                        long digestId = 0;
                        if (dKey.getType() == CBORType.Integer) digestId = dKey.AsInt64();
                        CBORObject dVal = digestMap.get(dKey);
                        if (dVal != null && dVal.getType() == CBORType.ByteString) {
                            nsDigests.put(digestId, dVal.GetByteString());
                        }
                    }
                }
                digests.put(ns, nsDigests);
            }
        }

        return new MsoDigestInfo(digests, algorithm);
    }

    // MSO valueDigests 대비 개별 issuerSignedItem의 digest 검증
    private static boolean verifyItemDigest(CBORObject item, java.util.Map<Long, byte[]> nsDigests, String algorithm) {
        try {
            // 항목을 그대로 인코딩 (태그 있으면 포함)하여 해시
            byte[] encodedItemBytes = item.EncodeToBytes();

            byte[] computedDigest = MessageDigest.getInstance(algorithm).digest(encodedItemBytes);

            // 디코딩된 항목에서 digestID 가져오기
            byte[] innerBytes = item.GetByteString();
            CBORObject claimMap = CBORObject.DecodeFromBytes(innerBytes);
            CBORObject digestIdItem = claimMap.get(CBORObject.FromObject("digestID"));
            long digestId = 0;
            if (digestIdItem != null && digestIdItem.getType() == CBORType.Integer) {
                digestId = digestIdItem.AsInt64();
            }

            byte[] expectedDigest = nsDigests.get(digestId);
            if (expectedDigest == null) {
                Log.w(TAG, "No MSO digest for digestID " + digestId);
                return false;
            }

            boolean match = Arrays.equals(computedDigest, expectedDigest);
            ProtocolLogger.logDigestVerification(digestId, computedDigest, expectedDigest, match);
            return match;
        } catch (Exception e) {
            Log.w(TAG, "Failed to verify item digest", e);
            return false;
        }
    }

    // issuerAuth COSE_Sign1 내 MSO에서 deviceKey (EC 공개키) 추출
    private static ECPublicKey extractDeviceKeyFromMso(CBORObject coseSign1) throws Exception {
        if (coseSign1 == null || coseSign1.size() < 3) return null;
        byte[] payload;
        if (coseSign1.get(2).getType() == CBORType.ByteString) {
            payload = coseSign1.get(2).GetByteString();
        } else {
            return null;
        }

        // MSO payload 디코딩
        CBORObject mso = decodeMsoFromPayload(payload);
        if (mso == null) return null;

        // deviceKeyInfo.deviceKey (COSE_Key) 가져오기
        CBORObject dkInfoItem = mso.get(CBORObject.FromObject("deviceKeyInfo"));
        if (dkInfoItem == null || dkInfoItem.getType() != CBORType.Map) return null;
        CBORObject dkItem = dkInfoItem.get(CBORObject.FromObject("deviceKey"));
        if (dkItem == null || dkItem.getType() != CBORType.Map) return null;

        CBORObject coseKey = dkItem;
        byte[] x = null, y = null;
        for (CBORObject key : coseKey.getKeys()) {
            if (key.getType() != CBORType.Integer) continue;
            long keyVal = key.AsInt64();
            CBORObject val = coseKey.get(key);
            if (keyVal == -2 && val.getType() == CBORType.ByteString) x = val.GetByteString();
            else if (keyVal == -3 && val.getType() == CBORType.ByteString) y = val.GetByteString();
        }
        if (x == null || y == null) return null;

        ECPoint point = new ECPoint(new BigInteger(1, x), new BigInteger(1, y));
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecSpec = params.getParameterSpec(ECParameterSpec.class);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return (ECPublicKey) kf.generatePublic(new ECPublicKeySpec(point, ecSpec));
    }

    public static Object cborToJava(CBORObject item) {
        if (item == null) return null;
        if (item.isNull()) return null;
        if (item.isTrue()) return true;
        if (item.isFalse()) return false;
        if (item.getType() == CBORType.TextString) return item.AsString();
        if (item.getType() == CBORType.ByteString) return Base64.encodeToString(item.GetByteString(), Base64.NO_WRAP);
        if (item.getType() == CBORType.Integer) return item.AsInt64();
        if (item.getType() == CBORType.FloatingPoint) return item.AsDouble();
        if (item.getType() == CBORType.Array) {
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < item.size(); i++) {
                list.add(cborToJava(item.get(i)));
            }
            return list;
        }
        if (item.getType() == CBORType.Map) {
            java.util.Map<String, Object> map = new LinkedHashMap<>();
            for (CBORObject key : item.getKeys()) {
                String keyStr = (key.getType() == CBORType.TextString) ? key.AsString() : key.toString();
                map.put(keyStr, cborToJava(item.get(key)));
            }
            return map;
        }
        return item.toString();
    }

    // 내부 클래스

    public static class ParsedResponse {
        private final List<ParsedDocument> documents;
        public ParsedResponse(List<ParsedDocument> documents) { this.documents = documents; }
        public List<ParsedDocument> getDocuments() { return documents; }
    }

    public static class ParsedDocument {
        private final String docType;
        private final java.util.Map<String, java.util.Map<String, Object>> namespacedClaims;
        private final IssuerSignedInfo issuerSignedInfo;
        private final Boolean deviceSignatureValid;
        private final Boolean dataIntegrityIntact;

        public ParsedDocument(String docType,
                              java.util.Map<String, java.util.Map<String, Object>> namespacedClaims,
                              IssuerSignedInfo issuerSignedInfo,
                              Boolean deviceSignatureValid,
                              Boolean dataIntegrityIntact) {
            this.docType = docType;
            this.namespacedClaims = namespacedClaims;
            this.issuerSignedInfo = issuerSignedInfo;
            this.deviceSignatureValid = deviceSignatureValid;
            this.dataIntegrityIntact = dataIntegrityIntact;
        }

        public String getDocType() { return docType; }
        public java.util.Map<String, java.util.Map<String, Object>> getNamespacedClaims() { return namespacedClaims; }
        public IssuerSignedInfo getIssuerSignedInfo() { return issuerSignedInfo; }
        public Boolean isDeviceSignatureValid() { return deviceSignatureValid; }
        public Boolean isDataIntegrityIntact() { return dataIntegrityIntact; }

        public java.util.Map<String, Object> getFlattenedClaims() {
            java.util.Map<String, Object> flat = new LinkedHashMap<>();
            for (java.util.Map<String, Object> ns : namespacedClaims.values()) {
                flat.putAll(ns);
            }
            return flat;
        }
    }

    public static class IssuerSignedInfo {
        private final String signed;
        private final String validFrom;
        private final String validUntil;
        private final CBORObject coseSign1Items;
        private final Boolean issuerSignatureValid;

        public IssuerSignedInfo(String signed, String validFrom, String validUntil,
                                CBORObject coseSign1Items, Boolean issuerSignatureValid) {
            this.signed = signed;
            this.validFrom = validFrom;
            this.validUntil = validUntil;
            this.coseSign1Items = coseSign1Items;
            this.issuerSignatureValid = issuerSignatureValid;
        }

        public String getSigned() { return signed; }
        public String getValidFrom() { return validFrom; }
        public String getValidUntil() { return validUntil; }
        public CBORObject getCoseSign1Items() { return coseSign1Items; }
        public Boolean isIssuerSignatureValid() { return issuerSignatureValid; }
    }
}
