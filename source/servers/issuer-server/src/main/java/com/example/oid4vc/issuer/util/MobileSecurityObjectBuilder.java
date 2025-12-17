package com.example.oid4vc.issuer.util;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * MSO (Mobile Security Object) 생성
 * Section 9.1.2.4 (Signing method and structure for MSO) 참고
 * 
 * IA가 MSO를 생성하고, 이를 COSE Sign1로 서명하여 IssuerAuth를 생성합니다.
 */
public class MobileSecurityObjectBuilder {
    
    private static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String NAMESPACE = "org.iso.18013.5.1";
    
    /**
     * MSO (Mobile Security Object) 생성
     * 
     * Section 9.1.2.4 명시 구조:
     * MobileSecurityObject = {
     *   "version" : tstr, ; "1.0"
     *   "digestAlgorithm" : tstr, ; "SHA-256"
     *   "valueDigests" : ValueDigests, ; Digest 맵
     *   "deviceKeyInfo" : DeviceKeyInfo,
     *   "docType" : tstr, ; "org.iso.18013.5.1.mDL"
     *   "validityInfo" : ValidityInfo
     * }
     */
    public static Map<String, Object> buildMso(
            Map<Integer, byte[]> valueDigests,
            PublicKey devicePublicKey,
            LocalDateTime validFrom,
            LocalDateTime validUntil) throws Exception {
        
        Map<String, Object> mso = new LinkedHashMap<>();
        
        // 1. version
        // Section 9.1.2.4: "The version for the MobileSecurityObject structure 
        // shall be "1.0" in the current version of this document"
        mso.put("version", "1.0");
        
        // 2. digestAlgorithm
        // Section 9.1.2.5: SHA-256, SHA-384, SHA-512 중 하나
        mso.put("digestAlgorithm", "SHA-256");
        
        // 3. valueDigests
        // DigestID → digest 바이트 맵
        Map<String, Object> digestsByNamespace = new LinkedHashMap<>();
        digestsByNamespace.put(NAMESPACE, valueDigests);
        mso.put("valueDigests", digestsByNamespace);
        
        // 4. deviceKeyInfo
        // Section 9.1.2.4: Device의 공개키를 포함
        // ⚠️ Device의 개인키는 포함하면 안 됨 (공개키만)
        Map<String, Object> deviceKeyInfo = buildDeviceKeyInfo(devicePublicKey);
        mso.put("deviceKeyInfo", deviceKeyInfo);
        
        // 5. docType
        // Section 9.1.2.4: "DocType is the document type of the document and shall 
        // be identical to the DocType element in the mdoc response"
        mso.put("docType", "org.iso.18013.5.1.mDL");
        
        // 6. validityInfo
        // Section 9.1.2.4: signed, validFrom, validUntil, expectedUpdate (optional)
        Map<String, Object> validityInfo = buildValidityInfo(validFrom, validUntil);
        mso.put("validityInfo", validityInfo);
        
        return mso;
    }
    
    /**
     * DeviceKeyInfo 구조 생성
     * 
     * DeviceKeyInfo = {
     *   "deviceKey" : DeviceKey (COSE_Key),
     *   ? "keyAuthorizations" : KeyAuthorizations
     * }
     */
    private static Map<String, Object> buildDeviceKeyInfo(PublicKey devicePublicKey) {
        Map<String, Object> deviceKeyInfo = new LinkedHashMap<>();
        
        // deviceKey: COSE_Key 형식으로 Device 공개키 포함
        // Section 9.1.2.4: "The deviceKey element is encoded as an untagged COSE_Key"
        Map<String, Object> deviceKey = convertToCoseKey(devicePublicKey);
        deviceKeyInfo.put("deviceKey", deviceKey);
        
        // keyAuthorizations: 이 키가 서명/MAC할 수 있는 데이터 지정
        Map<String, Object> keyAuthorizations = new LinkedHashMap<>();
        // 임시: 모든 namespace 승인
        List<String> authorizedNamespaces = Arrays.asList(NAMESPACE);
        keyAuthorizations.put("nameSpaces", authorizedNamespaces);
        deviceKeyInfo.put("keyAuthorizations", keyAuthorizations);
        
        return deviceKeyInfo;
    }
    
    /**
     * ValidityInfo 구조 생성
     * 
     * ValidityInfo = {
     *   "signed" : tdate,
     *   "validFrom" : tdate,
     *   "validUntil" : tdate,
     *   ? "expectedUpdate" : tdate
     * }
     * 
     * Section 9.1.2.4: "The timestamps in the ValidityInfo structure shall not use 
     * fractions of seconds and shall use a UTC offset of 00:00, as indicated by 
     * the character "Z"."
     */
    private static Map<String, Object> buildValidityInfo(
            LocalDateTime validFrom,
            LocalDateTime validUntil) {
        
        Map<String, Object> validityInfo = new LinkedHashMap<>();
        
        // signed: MSO 서명 생성 시간 (현재 시각)
        String signed = LocalDateTime.now().format(ISO_DATETIME) + "Z";
        validityInfo.put("signed", signed);
        
        // validFrom: MSO 유효 시작 시간
        String validFromStr = validFrom.format(ISO_DATETIME) + "Z";
        validityInfo.put("validFrom", validFromStr);
        
        // validUntil: MSO 유효 종료 시간
        String validUntilStr = validUntil.format(ISO_DATETIME) + "Z";
        validityInfo.put("validUntil", validUntilStr);
        
        return validityInfo;
    }
    
    /**
     * PublicKey를 COSE_Key 형식으로 변환
     * 
     * Section 9.1.2.4: "The deviceKey element is encoded as an untagged COSE_Key"
     * RFC 8152 Section 13.1.1 참고
     * 
     * 중요: P-256의 경우 x, y 좌표는 반드시 32 바이트여야 함
     * (RFC 8152에서 정의한 고정 크기 좌표)
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> convertToCoseKey(PublicKey publicKey) {
        // Object 키를 지원하는 LinkedHashMap 사용
        Map<Object, Object> keyMap = new LinkedHashMap<>();
        
        // COSE_Key 필드들
        // kty: Key Type (1 = OKP, 2 = EC, 3 = RSA, 4 = Symmetric)
        keyMap.put(1, 2);  // kty = EC (2)
        
        // crv: Curve
        // 1 = P-256, 2 = P-384, 3 = P-521, 4 = X25519, 5 = X448
        keyMap.put(-1, 1);  // crv = P-256 (1)
        
        // EC 공개키에서 좌표 추출
        if (publicKey instanceof ECPublicKey) {
            ECPublicKey ecPub = (ECPublicKey) publicKey;
            byte[] xCoord = bigIntegerToFixedLengthBytes(ecPub.getW().getAffineX(), 32);
            byte[] yCoord = bigIntegerToFixedLengthBytes(ecPub.getW().getAffineY(), 32);
            
            // x, y 좌표 (음수 CBOR 레이블)
            keyMap.put(-2, xCoord);  // x coordinate
            keyMap.put(-3, yCoord);  // y coordinate
        }
        
        // Object를 String으로 캐스트하여 반환
        return (Map<String, Object>) (Map<?, ?>) keyMap;
    }
    
    /**
     * BigInteger를 고정 길이 바이트 배열로 변환
     * 
     * BigInteger.toByteArray()는 부호 비트를 고려하여 예상과 다른 길이를 반환할 수 있습니다.
     * 이 메서드는 항상 지정된 길이의 바이트 배열을 반환합니다.
     * 
     * @param value BigInteger 값
     * @param length 목표 바이트 길이
     * @return 고정 길이 바이트 배열 (Zero-padded if needed, 또는 truncated if too large)
     */
    private static byte[] bigIntegerToFixedLengthBytes(java.math.BigInteger value, int length) {
        byte[] input = value.toByteArray();
        byte[] output = new byte[length];
        
        if (input.length <= length) {
            // Zero-pad: 원본을 끝에 복사
            System.arraycopy(input, 0, output, length - input.length, input.length);
        } else {
            // Truncate: 뒤쪽 바이트만 가져오기 (선행 0x00 제거)
            System.arraycopy(input, input.length - length, output, 0, length);
        }
        
        return output;
    }
    
    /**
     * 좌표를 지정된 길이로 Zero-pad (ISO/IEC 18013-5 준수)
     * 
     * RFC 8152 Section 13.1.1:
     * "Coordinates are encoded as fixed-size big-endian integers"
     * 
     * BigInteger.toByteArray()는 가변 길이를 반환하므로 (선행 0 제거),
     * 고정 크기로 패딩이 필요합니다.
     * 
     * @param coord 원본 좌표 바이트
     * @param expectedLength 목표 길이 (P-256 = 32, P-384 = 48, P-521 = 66)
     * @return 목표 길이로 패딩된 좌표
     * @throws IllegalArgumentException 좌표가 목표 길이보다 크면
     */
    private static byte[] padCoordinate(byte[] coord, int expectedLength) {
        if (coord.length == expectedLength) {
            // 이미 올바른 길이
            return coord;
        }
        
        if (coord.length > expectedLength) {
            // 좌표가 너무 길면 에러
            throw new IllegalArgumentException(
                "Coordinate length (" + coord.length + 
                ") exceeds expected length (" + expectedLength + ")");
        }
        
        // Zero-pad: 시작 부분에 0x00 추가
        byte[] padded = new byte[expectedLength];
        // 원본 좌표를 끝 부분에 복사
        System.arraycopy(coord, 0, padded, expectedLength - coord.length, coord.length);
        
        return padded;
    }
}
