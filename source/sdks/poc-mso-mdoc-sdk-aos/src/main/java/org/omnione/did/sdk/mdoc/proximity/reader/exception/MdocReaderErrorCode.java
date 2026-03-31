package org.omnione.did.sdk.mdoc.proximity.reader.exception;

public enum MdocReaderErrorCode {
    // 00: BLE 가용성 및 연결
    BLE_NOT_AVAILABLE          ("MSDKMDC", "00000", "Bluetooth not available"),
    BLE_SCANNER_NOT_AVAILABLE  ("MSDKMDC", "00001", "BLE scanner not available"),
    BLE_CONNECTION_FAILED      ("MSDKMDC", "00100", "BLE connection failed"),
    BLE_SCAN_FAILED            ("MSDKMDC", "00101", "BLE scan failed"),
    BLE_SERVICE_NOT_FOUND      ("MSDKMDC", "00200", "mDOC BLE service not found"),
    BLE_ADVERTISING_FAILED     ("MSDKMDC", "00201", "BLE advertising failed"),
    BLE_NO_CONNECTION_METHOD   ("MSDKMDC", "00300", "No BLE connection method available"),
    BLE_GATT_SERVER_FAILED     ("MSDKMDC", "00301", "Could not open GATT server"),
    BLE_SERVICE_DISCOVERY_FAILED("MSDKMDC", "00302", "Service discovery failed"),
    // 00: 데이터 전송
    TRANSFER_SEND_FAILED       ("MSDKMDC", "00400", "Failed to send data"),
    TRANSFER_RECEIVE_FAILED    ("MSDKMDC", "00401", "Failed to receive data"),
    SESSION_DATA_INVALID       ("MSDKMDC", "00500", "Invalid session data"),
    SESSION_TERMINATION_FAILED ("MSDKMDC", "00501", "Failed to send session termination"),
    PERMISSION_DENIED          ("MSDKMDC", "00600", "Bluetooth permission denied"),

    // 01: 세션 및 암호화
    SESSION_ENCRYPTION_FAILED  ("MSDKMDC", "01000", "Session encryption failed"),
    SESSION_DECRYPTION_FAILED  ("MSDKMDC", "01001", "Session decryption failed"),
    // 01: 디바이스 인게이지먼트
    INVALID_DEVICE_ENGAGEMENT  ("MSDKMDC", "01100", "Invalid device engagement"),
    INVALID_CBOR_DATA          ("MSDKMDC", "01101", "Invalid CBOR data"),
    INVALID_COSE_KEY           ("MSDKMDC", "01102", "Invalid COSE_Key format"),
    MISSING_EC_COORDINATES     ("MSDKMDC", "01103", "Missing EC coordinates in key"),
    // 01: 요청 및 응답
    DEVICE_REQUEST_BUILD_FAILED("MSDKMDC", "01200", "Failed to build device request"),
    DEVICE_RESPONSE_PARSE_FAILED("MSDKMDC", "01201", "Failed to parse device response"),
    // 01: 신뢰 및 검증
    TRUST_VALIDATION_FAILED    ("MSDKMDC", "01300", "Trust validation failed"),
    // 01: 초기화
    TRANSFER_NOT_INITIALIZED   ("MSDKMDC", "01400", "Transfer manager not initialized"),

    // 02: 인증서
    CERTIFICATE_LOAD_FAILED    ("MSDKMDC", "02000", "Failed to load certificate"),
    CERTIFICATE_PARSE_FAILED   ("MSDKMDC", "02001", "Failed to parse certificate"),
    // 02: 데이터 파싱
    CLAIM_VALUE_PARSE_FAILED   ("MSDKMDC", "02100", "Failed to parse claim value"),
    // 02: 리소스
    RESOURCE_NOT_FOUND         ("MSDKMDC", "02200", "Resource not found"),

    // 03: Wi-Fi Aware
    WIFI_AWARE_NOT_AVAILABLE       ("MSDKMDC", "03000", "Wi-Fi Aware not available"),
    WIFI_AWARE_NOT_SUPPORTED       ("MSDKMDC", "03001", "Wi-Fi Aware requires Android Q+"),
    WIFI_AWARE_ATTACH_FAILED       ("MSDKMDC", "03100", "Wi-Fi Aware attach failed"),
    WIFI_AWARE_SUBSCRIBE_FAILED    ("MSDKMDC", "03101", "Wi-Fi Aware subscribe failed"),
    WIFI_AWARE_PEER_NOT_FOUND      ("MSDKMDC", "03102", "Wi-Fi Aware peer not found"),
    WIFI_AWARE_NETWORK_FAILED      ("MSDKMDC", "03200", "Wi-Fi Aware network request failed"),
    WIFI_AWARE_TCP_CONNECT_FAILED  ("MSDKMDC", "03201", "Wi-Fi Aware TCP connection failed"),
    WIFI_AWARE_NO_CONNECTION_METHOD("MSDKMDC", "03300", "No Wi-Fi Aware connection method available"),

    // 04: NFC
    NFC_NOT_AVAILABLE              ("MSDKMDC", "04000", "NFC not available"),
    NFC_TAG_LOST                   ("MSDKMDC", "04100", "NFC tag lost during transfer"),
    NFC_SELECT_FAILED              ("MSDKMDC", "04101", "NFC SELECT command failed"),
    NFC_APDU_ERROR                 ("MSDKMDC", "04200", "NFC APDU error"),
    NFC_NO_CONNECTION_METHOD       ("MSDKMDC", "04300", "No NFC connection method available"),

    // 공통
    UNKNOWN                    ("MSDKMDC", "99999", "Unknown error");

    private final String feature;
    private final String code;
    private final String msg;

    MdocReaderErrorCode(String feature, String code, String msg) {
        this.feature = feature;
        this.code = code;
        this.msg = msg;
    }

    public String getFeature() { return feature; }
    public String getCode() { return code; }
    public String getMsg() { return msg; }
    public String getErrorCodeString() { return feature + code; }

    public static MdocReaderErrorCode getEnumByCode(String code) {
        for (MdocReaderErrorCode e : values()) {
            if (e.code.equals(code)) return e;
        }
        return UNKNOWN;
    }
}
