package org.omnione.did.sdk.mdoc.proximity.reader.core;

import android.util.Base64;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import org.omnione.did.sdk.mdoc.proximity.reader.exception.MdocReaderErrorCode;
import org.omnione.did.sdk.mdoc.proximity.reader.exception.MdocReaderException;
import org.omnione.did.sdk.mdoc.proximity.reader.utility.ProtocolLogger;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.UUID;

public class DeviceEngagement {
    // CBOR Tag 24: 인코딩된 CBOR 데이터 아이템
    private static final long CBOR_TAG_ENCODED = 24;

    // DeviceEngagement CBOR 구조 키 (ISO 18013-5 8.2.2.1)
    private static final int DE_KEY_SECURITY = 1;
    private static final int DE_KEY_METHODS = 2;

    // COSE_Key 파라미터 (RFC 8152)
    private static final int COSE_KEY_KTY = 1;      // 키 타입
    private static final int COSE_KEY_CRV = -1;     // 곡선
    private static final int COSE_KEY_X = -2;       // x 좌표
    private static final int COSE_KEY_Y = -3;       // y 좌표
    private static final int COSE_KTY_EC2 = 2;      // EC2 키 타입
    private static final int COSE_CRV_P256 = 1;     // P-256 곡선

    // 연결 방법 타입 (ISO 18013-5 Table A.1)
    private static final int CONNECTION_TYPE_NFC = 1;
    private static final int CONNECTION_TYPE_BLE = 2;
    private static final int CONNECTION_TYPE_WIFI_AWARE = 3;

    // BLE 연결 방법 옵션 키 (ISO 18013-5 Table A.2)
    private static final int BLE_OPT_PERIPHERAL_UUID = 10;
    private static final int BLE_OPT_CENTRAL_UUID = 11;

    // Wi-Fi Aware 연결 방법 옵션 키 (ISO 18013-5 Table A.3)
    private static final int WIFI_AWARE_OPT_PASSPHRASE = 0;
    private static final int WIFI_AWARE_OPT_CHANNEL_INFO = 1;
    private static final int WIFI_AWARE_OPT_BAND_INFO = 2;

    // NFC 연결 방법 옵션 키 (ISO 18013-5 Table A.4)
    private static final int NFC_OPT_MAX_COMMAND_DATA_LENGTH = 0;
    private static final int NFC_OPT_MAX_RESPONSE_DATA_LENGTH = 1;

    private ECPublicKey eDeviceKey;
    private byte[] encodedEngagement;
    private UUID peripheralServerModeUuid;
    private UUID centralClientModeUuid;

    // Wi-Fi Aware 필드
    private String wifiAwarePassphrase;
    private Integer wifiAwareChannelInfo;
    private Integer wifiAwareBandInfo;

    // NFC 필드
    private Integer nfcMaxCommandDataLength;
    private Integer nfcMaxResponseDataLength;

    // QR 코드로부터 DeviceEngagement 생성
    public static DeviceEngagement fromQrCode(String qrCode) throws Exception {
        ProtocolLogger.logQrCodeReceived(qrCode);
        DeviceEngagement de = new DeviceEngagement();
        String base64Data = qrCode.startsWith("mdoc:") ? qrCode.substring(5) : qrCode;
        byte[] decoded = Base64.decode(base64Data, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        de.encodedEngagement = decoded;
        de.parse(decoded);
        ProtocolLogger.logBleConnectionMethod(de.peripheralServerModeUuid, de.centralClientModeUuid);
        return de;
    }

    // NFC 바이트로부터 DeviceEngagement 생성
    public static DeviceEngagement fromBytes(byte[] data) throws Exception {
        ProtocolLogger.logDeviceEngagementParsed(data, "NfcBytes", false);
        DeviceEngagement de = new DeviceEngagement();
        de.encodedEngagement = data;
        de.parse(data);
        ProtocolLogger.logBleConnectionMethod(de.peripheralServerModeUuid, de.centralClientModeUuid);
        return de;
    }

    private void parse(byte[] data) throws Exception {
        CBORObject root = decodeCborRoot(data);
        ProtocolLogger.logDeviceEngagementParsed(data, root.getType().toString(), false);
        parseSecurityAndMethods(root);
    }

    // CBOR 루트 아이템 디코딩 (Tag 24 래핑 및 raw ByteString 처리 포함)
    private CBORObject decodeCborRoot(byte[] data) throws Exception {
        CBORObject root = decodeCbor(data);
        if (root == null) {
            throw new MdocReaderException(MdocReaderErrorCode.INVALID_CBOR_DATA, "Empty CBOR data");
        }

        // Tag 24로 래핑된 경우 내부 바이트 추출
        if (root.getType() == CBORType.ByteString && root.HasMostOuterTag(24)) {
            root = unwrapByteString(root);
        }

        // 태그 없는 raw ByteString인 경우 내부 CBOR 디코딩
        if (root.getType() == CBORType.ByteString && root.getType() != CBORType.Array && root.getType() != CBORType.Map) {
            CBORObject inner = tryUnwrapByteString(root);
            if (inner != null) root = inner;
        }

        return root;
    }

    // ByteString 내부의 CBOR 디코딩
    private CBORObject unwrapByteString(CBORObject bs) throws Exception {
        CBORObject inner = decodeCbor(bs.GetByteString());
        if (inner == null) {
            throw new MdocReaderException(MdocReaderErrorCode.INVALID_CBOR_DATA, "Empty inner CBOR data");
        }
        return inner;
    }

    // ByteString 내부 CBOR 디코딩 시도 (실패 시 null 반환)
    private CBORObject tryUnwrapByteString(CBORObject bs) {
        try {
            CBORObject inner = decodeCbor(bs.GetByteString());
            return inner;
        } catch (Exception e) {
            return null;
        }
    }

    // security 배열과 연결 방법 파싱
    private void parseSecurityAndMethods(CBORObject root) throws Exception {
        CBORObject securityItem = null;
        CBORObject methodsItem = null;

        if (root.getType() == CBORType.Array) {
            if (root.size() >= 2) securityItem = root.get(DE_KEY_SECURITY);
            if (root.size() >= 3) methodsItem = root.get(DE_KEY_METHODS);
        } else if (root.getType() == CBORType.Map) {
            securityItem = root.get(CBORObject.FromObject(DE_KEY_SECURITY));
            methodsItem = root.get(CBORObject.FromObject(DE_KEY_METHODS));
        } else {
            throw new MdocReaderException(MdocReaderErrorCode.INVALID_DEVICE_ENGAGEMENT,
                "Expected CBOR array or map, got " + root.getType().toString());
        }

        parseSecurityArray(securityItem);
        parseConnectionMethods(methodsItem);
    }

    // security 배열에서 EDeviceKey 추출: [cipherSuiteId, EDeviceKeyBytes]
    private void parseSecurityArray(CBORObject securityItem) throws Exception {
        if (securityItem == null || securityItem.getType() != CBORType.Array) return;

        if (securityItem.size() < 2) return;

        CBORObject eDeviceKeyItem = securityItem.get(1);
        if (eDeviceKeyItem.getType() != CBORType.ByteString) {
            throw new MdocReaderException(MdocReaderErrorCode.INVALID_COSE_KEY, "Unexpected EDeviceKey format");
        }

        eDeviceKey = parseCoseKey(eDeviceKeyItem.GetByteString());
        ProtocolLogger.logEDeviceKey(eDeviceKey);
    }

    // 연결 방법 배열 파싱
    private void parseConnectionMethods(CBORObject methodsItem) {
        if (methodsItem == null || methodsItem.getType() != CBORType.Array) return;

        for (int i = 0; i < methodsItem.size(); i++) {
            CBORObject method = methodsItem.get(i);
            if (method.getType() == CBORType.Array && method.size() >= 1) {
                long type = getCborIntegerValue(method.get(0));
                if (type == CONNECTION_TYPE_BLE) {
                    parseBleConnectionMethod(method);
                } else if (type == CONNECTION_TYPE_WIFI_AWARE) {
                    parseWifiAwareConnectionMethod(method);
                } else if (type == CONNECTION_TYPE_NFC) {
                    parseNfcConnectionMethod(method);
                }
            }
        }
    }

    // COSE_Key에서 EC 공개키 추출
    private ECPublicKey parseCoseKey(byte[] coseKeyBytes) throws Exception {
        CBORObject item = decodeCbor(coseKeyBytes);
        if (item.getType() != CBORType.Map) {
            throw new MdocReaderException(MdocReaderErrorCode.INVALID_COSE_KEY, "COSE_Key must be a map");
        }

        byte[] xCoord = null;
        byte[] yCoord = null;
        CBORObject coseKey = item;

        for (CBORObject key : coseKey.getKeys()) {
            long keyVal = getCborIntegerValue(key);
            CBORObject val = coseKey.get(key);
            if (keyVal == COSE_KEY_X && val.getType() == CBORType.ByteString) {
                xCoord = val.GetByteString();
            } else if (keyVal == COSE_KEY_Y && val.getType() == CBORType.ByteString) {
                yCoord = val.GetByteString();
            }
        }

        if (xCoord == null || yCoord == null) {
            throw new MdocReaderException(MdocReaderErrorCode.MISSING_EC_COORDINATES);
        }

        return buildEcPublicKey(xCoord, yCoord);
    }

    // x, y 좌표로 EC 공개키 생성
    private ECPublicKey buildEcPublicKey(byte[] x, byte[] y) throws Exception {
        ECPoint point = new ECPoint(new BigInteger(1, x), new BigInteger(1, y));
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecSpec = params.getParameterSpec(ECParameterSpec.class);
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(point, ecSpec));
    }

    // BLE 연결 방법 파싱: [type, version, options]
    private void parseBleConnectionMethod(CBORObject method) {
        if (method.size() < 3) return;

        long type = getCborIntegerValue(method.get(0));
        if (type != CONNECTION_TYPE_BLE) return;

        if (method.get(2).getType() != CBORType.Map) return;
        CBORObject opts = method.get(2);

        for (CBORObject key : opts.getKeys()) {
            long optKey = getCborIntegerValue(key);
            CBORObject val = opts.get(key);

            if (optKey == BLE_OPT_PERIPHERAL_UUID && val.getType() == CBORType.ByteString) {
                peripheralServerModeUuid = bytesToUuid(val.GetByteString());
            } else if (optKey == BLE_OPT_CENTRAL_UUID && val.getType() == CBORType.ByteString) {
                centralClientModeUuid = bytesToUuid(val.GetByteString());
            }
        }
    }

    // Wi-Fi Aware 연결 방법 파싱: [type, version, options]
    private void parseWifiAwareConnectionMethod(CBORObject method) {
        if (method.size() < 3) return;
        if (method.get(2).getType() != CBORType.Map) return;
        CBORObject opts = method.get(2);
        for (CBORObject key : opts.getKeys()) {
            long optKey = getCborIntegerValue(key);
            CBORObject val = opts.get(key);
            if (optKey == WIFI_AWARE_OPT_PASSPHRASE && val.getType() == CBORType.TextString) {
                wifiAwarePassphrase = val.AsString();
            } else if (optKey == WIFI_AWARE_OPT_CHANNEL_INFO && val.getType() == CBORType.Integer) {
                wifiAwareChannelInfo = val.AsInt32();
            } else if (optKey == WIFI_AWARE_OPT_BAND_INFO && val.getType() == CBORType.Integer) {
                wifiAwareBandInfo = val.AsInt32();
            }
        }
    }

    // NFC 연결 방법 파싱: [type, version, options]
    private void parseNfcConnectionMethod(CBORObject method) {
        if (method.size() < 3) return;
        if (method.get(2).getType() != CBORType.Map) return;
        CBORObject opts = method.get(2);
        for (CBORObject key : opts.getKeys()) {
            long optKey = getCborIntegerValue(key);
            CBORObject val = opts.get(key);
            if (optKey == NFC_OPT_MAX_COMMAND_DATA_LENGTH && val.getType() == CBORType.Integer) {
                nfcMaxCommandDataLength = val.AsInt32();
            } else if (optKey == NFC_OPT_MAX_RESPONSE_DATA_LENGTH && val.getType() == CBORType.Integer) {
                nfcMaxResponseDataLength = val.AsInt32();
            }
        }
    }

    // CBOR 정수 아이템에서 long 값 추출
    private long getCborIntegerValue(CBORObject item) {
        if (item.getType() == CBORType.Integer) return item.AsInt64();
        return Long.MIN_VALUE;
    }

    // CBOR 바이트 디코딩 헬퍼
    private CBORObject decodeCbor(byte[] data) throws Exception {
        return CBORObject.DecodeFromBytes(data);
    }

    // 16바이트를 UUID로 변환
    private UUID bytesToUuid(byte[] bytes) {
        if (bytes.length < 16) return null;
        long msb = 0, lsb = 0;
        for (int i = 0; i < 8; i++) msb = (msb << 8) | (bytes[i] & 0xff);
        for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (bytes[i] & 0xff);
        return new UUID(msb, lsb);
    }

    public ECPublicKey getEDeviceKey() { return eDeviceKey; }
    public byte[] getEncodedEngagement() { return encodedEngagement; }
    public UUID getPeripheralServerModeUuid() { return peripheralServerModeUuid; }
    public UUID getCentralClientModeUuid() { return centralClientModeUuid; }

    // Wi-Fi Aware getters
    public String getWifiAwarePassphrase() { return wifiAwarePassphrase; }
    public Integer getWifiAwareChannelInfo() { return wifiAwareChannelInfo; }
    public Integer getWifiAwareBandInfo() { return wifiAwareBandInfo; }
    public boolean hasWifiAwareConnectionMethod() { return wifiAwarePassphrase != null || wifiAwareChannelInfo != null || wifiAwareBandInfo != null; }

    // NFC getters
    public Integer getNfcMaxCommandDataLength() { return nfcMaxCommandDataLength; }
    public Integer getNfcMaxResponseDataLength() { return nfcMaxResponseDataLength; }
    public boolean hasNfcConnectionMethod() { return nfcMaxCommandDataLength != null || nfcMaxResponseDataLength != null; }

    // BLE getter
    public boolean hasBleConnectionMethod() { return peripheralServerModeUuid != null || centralClientModeUuid != null; }
}
