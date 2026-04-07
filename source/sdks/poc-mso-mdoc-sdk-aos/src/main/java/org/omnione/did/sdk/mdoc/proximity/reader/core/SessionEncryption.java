package org.omnione.did.sdk.mdoc.proximity.reader.core;

import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import org.omnione.did.sdk.mdoc.proximity.reader.exception.MdocReaderErrorCode;
import org.omnione.did.sdk.mdoc.proximity.reader.exception.MdocReaderException;
import org.omnione.did.sdk.mdoc.proximity.reader.utility.ProtocolLogger;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;

// ISO/IEC 18013-5:2021 섹션 9.1.1 기반 세션 암호화
// 1. SessionTranscript salt = SHA-256(CBOR(Tag(24, bstr(encodedSessionTranscript))))
// 2. HKDF로 32바이트 키 도출, AES-256-GCM 사용
// 3. IV = [0x00000000][identifier:4bytes][counter:4bytes] (identifier: 0=리더, 1=디바이스)
// 4. SessionEstablishment의 eReaderKey는 Tag 24로 래핑
public class SessionEncryption {
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    // HKDF 키 도출 라벨 (ISO 18013-5 9.1.1.5)
    private static final byte[] LABEL_SK_READER = "SKReader".getBytes();
    private static final byte[] LABEL_SK_DEVICE = "SKDevice".getBytes();
    private static final byte[] LABEL_EMAC_KEY = "EMacKey".getBytes();
    private static final int SESSION_KEY_LENGTH = 32;

    // COSE_Key 파라미터 (RFC 8152)
    private static final int COSE_KEY_KTY = 1;
    private static final int COSE_KTY_EC2 = 2;
    private static final int COSE_CRV_P256 = 1;
    private static final int COSE_KEY_CRV = -1;
    private static final int COSE_KEY_X = -2;
    private static final int COSE_KEY_Y = -3;

    // 세션 종료 상태 코드 (ISO 18013-5 8.3.3.1.1.5)
    private static final int SESSION_TERMINATION_STATUS = 20;

    private final KeyPair readerKeyPair;
    private final ECPublicKey devicePublicKey;
    private final byte[] encodedEngagement;
    private byte[] skReader;
    private byte[] skDevice;
    private byte[] eMacKey;
    private byte[] encodedSessionTranscript;
    private int readerMessageCounter = 1;
    private int deviceMessageCounter = 1;

    public SessionEncryption(ECPublicKey devicePublicKey, byte[] encodedEngagement) throws Exception {
        this.devicePublicKey = devicePublicKey;
        this.encodedEngagement = encodedEngagement;
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        this.readerKeyPair = kpg.generateKeyPair();
        ProtocolLogger.logEReaderKeyGenerated((ECPublicKey) readerKeyPair.getPublic());
        deriveSessionKeys();
    }

    private void deriveSessionKeys() throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(readerKeyPair.getPrivate());
        ka.doPhase(devicePublicKey, true);
        byte[] sharedSecret = ka.generateSecret();

        // ISO 18013-5 9.1.1.5에 따라 SessionTranscript 구성 및 salt 계산
        // salt = SHA-256( CBOR(Tag(24, bstr(encodedSessionTranscript))) )
        byte[] encodedSessionTranscript = buildSessionTranscriptBytes();
        byte[] taggedSessionTranscript = cborEncodeTagged24(encodedSessionTranscript);
        byte[] salt = sha256(taggedSessionTranscript);

        byte[] prk = hkdfExtract(salt, sharedSecret);
        skReader = hkdfExpand(prk, LABEL_SK_READER, SESSION_KEY_LENGTH);
        skDevice = hkdfExpand(prk, LABEL_SK_DEVICE, SESSION_KEY_LENGTH);

        this.encodedSessionTranscript = encodedSessionTranscript;

        ProtocolLogger.logSessionTranscript(encodedSessionTranscript, this.encodedEngagement, getEReaderKeyBytes());
        ProtocolLogger.logSessionKeysDerivation(sharedSecret, salt, skReader, skDevice, eMacKey);
    }

    // CBOR Tag 24로 인코딩
    private byte[] cborEncodeTagged24(byte[] data) throws Exception {
        CBORObject tagged = CBORObject.FromObjectAndTag(CBORObject.FromObject(data), 24);
        return tagged.EncodeToBytes();
    }

    private byte[] buildSessionTranscriptBytes() throws Exception {
        // SessionTranscript = [DeviceEngagementBytes, EReaderKeyBytes, Handover]
        // QR engagement의 경우 Handover = null
        CBORObject arr = CBORObject.NewArray();
        CBORObject deBs = CBORObject.FromObjectAndTag(CBORObject.FromObject(encodedEngagement), 24);
        arr.Add(deBs);
        CBORObject erBs = CBORObject.FromObjectAndTag(CBORObject.FromObject(getEReaderKeyBytes()), 24);
        arr.Add(erBs);
        arr.Add(CBORObject.Null);

        return arr.EncodeToBytes();
    }

    public byte[] getEReaderKeyBytes() throws Exception {
        ECPublicKey pub = (ECPublicKey) readerKeyPair.getPublic();
        ECPoint w = pub.getW();
        byte[] x = bigIntToFixedBytes(w.getAffineX(), 32);
        byte[] y = bigIntToFixedBytes(w.getAffineY(), 32);

        CBORObject coseKey = CBORObject.NewMap();
        coseKey.set(CBORObject.FromObject(COSE_KEY_KTY), CBORObject.FromObject(COSE_KTY_EC2));
        coseKey.set(CBORObject.FromObject(COSE_KEY_CRV), CBORObject.FromObject(COSE_CRV_P256));
        coseKey.set(CBORObject.FromObject(COSE_KEY_X), CBORObject.FromObject(x));
        coseKey.set(CBORObject.FromObject(COSE_KEY_Y), CBORObject.FromObject(y));

        return coseKey.EncodeToBytes();
    }

    // EDeviceKeyBytes = #6.24(bstr .cbor EDeviceKey)
    // EDeviceKey는 DeviceEngagement Security 배열에서 추출한 COSE_Key
    public byte[] getEDeviceKeyBytes() throws Exception {
        ECPoint w = devicePublicKey.getW();
        byte[] x = bigIntToFixedBytes(w.getAffineX(), 32);
        byte[] y = bigIntToFixedBytes(w.getAffineY(), 32);

        CBORObject coseKey = CBORObject.NewMap();
        coseKey.set(CBORObject.FromObject(COSE_KEY_KTY), CBORObject.FromObject(COSE_KTY_EC2));
        coseKey.set(CBORObject.FromObject(COSE_KEY_CRV), CBORObject.FromObject(COSE_CRV_P256));
        coseKey.set(CBORObject.FromObject(COSE_KEY_X), CBORObject.FromObject(x));
        coseKey.set(CBORObject.FromObject(COSE_KEY_Y), CBORObject.FromObject(y));

        return cborEncodeTagged24(coseKey.EncodeToBytes());
    }

    public byte[] encryptRequest(byte[] plaintext) throws Exception {
        return encrypt(plaintext, skReader, readerMessageCounter++, false);
    }

    public byte[] decryptResponse(byte[] ciphertext) throws Exception {
        return decrypt(ciphertext, skDevice, deviceMessageCounter++, true);
    }

    // ISO 18013-5 9.1.1.5 기반 IV 구성
    // [0x00000000] [identifier:4bytes] [counter:4bytes]
    // identifier: 0x00000000=리더, 0x00000001=디바이스
    private byte[] encrypt(byte[] plaintext, byte[] key, int counter, boolean isDevice) throws Exception {
        byte[] iv = buildIv(counter, isDevice);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] result = cipher.doFinal(plaintext);
        ProtocolLogger.logCrypto("encrypt", counter, iv, plaintext.length, result.length);
        return result;
    }

    private byte[] decrypt(byte[] ciphertext, byte[] key, int counter, boolean isDevice) throws Exception {
        byte[] iv = buildIv(counter, isDevice);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] result = cipher.doFinal(ciphertext);
        ProtocolLogger.logCrypto("decrypt", counter, iv, ciphertext.length, result.length);
        return result;
    }

    private byte[] buildIv(int counter, boolean isDevice) {
        ByteBuffer bb = ByteBuffer.allocate(GCM_IV_LENGTH);
        bb.putInt(0);                             // 4 bytes zero
        bb.putInt(isDevice ? 1 : 0);              // 4 bytes identifier
        bb.putInt(counter);                        // 4 bytes counter
        return bb.array();
    }

    // SessionEstablishment 메시지 구성
    // eReaderKey는 ISO 18013-5 9.1.1.4에 따라 Tag 24로 인코딩
    public byte[] buildSessionEstablishment(byte[] encryptedRequest) throws Exception {
        CBORObject map = CBORObject.NewMap();

        // eReaderKey: Tag(24, bstr(CBOR 인코딩된 COSE_Key))
        CBORObject eReaderKeyTagged = CBORObject.FromObjectAndTag(CBORObject.FromObject(getEReaderKeyBytes()), 24);
        map.set(CBORObject.FromObject("eReaderKey"), eReaderKeyTagged);

        map.set(CBORObject.FromObject("data"), CBORObject.FromObject(encryptedRequest));

        byte[] result = map.EncodeToBytes();
        ProtocolLogger.logSessionEstablishment(getEReaderKeyBytes(), encryptedRequest, result);
        return result;
    }

    public byte[] buildSessionData(byte[] encryptedData) throws Exception {
        CBORObject map = CBORObject.NewMap();
        map.set(CBORObject.FromObject("data"), CBORObject.FromObject(encryptedData));
        return map.EncodeToBytes();
    }

    // ISO 18013-5 8.3.3.1.1.5: 세션 종료 메시지
    // SessionData = { "status": 20 } ("data" 필드 없음)
    public byte[] buildSessionTermination() throws Exception {
        CBORObject map = CBORObject.NewMap();
        map.set(CBORObject.FromObject("status"), CBORObject.FromObject(SESSION_TERMINATION_STATUS));
        return map.EncodeToBytes();
    }

    public byte[] parseSessionData(byte[] sessionData) throws Exception {
        // Read()를 사용하여 trailing bytes 허용 (S25 BLE 이슈 대응)
        CBORObject map = CBORObject.Read(new ByteArrayInputStream(sessionData));
        CBORObject dataItem = map.get(CBORObject.FromObject("data"));
        if (dataItem != null && dataItem.getType() == CBORType.ByteString) {
            byte[] extracted = dataItem.GetByteString();
            ProtocolLogger.logSessionDataReceived(sessionData, extracted, false);
            return extracted;
        }
        CBORObject statusItem = map.get(CBORObject.FromObject("status"));
        if (statusItem != null) {
            ProtocolLogger.logSessionDataReceived(sessionData, null, true);
            return null; // 세션 종료
        }
        throw new MdocReaderException(MdocReaderErrorCode.SESSION_DATA_INVALID);
    }

    private byte[] sha256(byte[] data) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(data);
    }

    private byte[] hkdfExtract(byte[] salt, byte[] ikm) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        return mac.doFinal(ikm);
    }

    private byte[] hkdfExpand(byte[] prk, byte[] info, int length) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        byte[] t = new byte[0];
        byte[] okm = new byte[length];
        int offset = 0;
        int i = 1;
        while (offset < length) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(t);
            baos.write(info);
            baos.write(i++);
            t = mac.doFinal(baos.toByteArray());
            int toCopy = Math.min(t.length, length - offset);
            System.arraycopy(t, 0, okm, offset, toCopy);
            offset += toCopy;
        }
        return okm;
    }

    private byte[] bigIntToFixedBytes(BigInteger val, int length) {
        byte[] bytes = val.toByteArray();
        if (bytes.length == length) return bytes;
        byte[] result = new byte[length];
        if (bytes.length > length) {
            System.arraycopy(bytes, bytes.length - length, result, 0, length);
        } else {
            System.arraycopy(bytes, 0, result, length - bytes.length, bytes.length);
        }
        return result;
    }

    public ECPublicKey getReaderPublicKey() {
        return (ECPublicKey) readerKeyPair.getPublic();
    }

    // ISO 18013-5 9.1.3.5에 따라 EMacKey 도출
    // EMacKey는 SDeviceKey(MSO의 static device key) × EReaderKey로 도출해야 함
    // (세션 암호화에 사용하는 EDeviceKey × EReaderKey 와는 다름)
    public void deriveEMacKey(ECPublicKey deviceKeyFromMso) throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(readerKeyPair.getPrivate());
        ka.doPhase(deviceKeyFromMso, true);
        byte[] macSharedSecret = ka.generateSecret();

        byte[] salt = sha256(cborEncodeTagged24(encodedSessionTranscript));
        byte[] prk = hkdfExtract(salt, macSharedSecret);
        this.eMacKey = hkdfExpand(prk, LABEL_EMAC_KEY, SESSION_KEY_LENGTH);
    }

    public byte[] getEMacKey() { return eMacKey; }
    public byte[] getEncodedSessionTranscript() { return encodedSessionTranscript; }
}
