package org.omnione.did.sdk.mdoc.proximity.reader.utility;

import android.util.Log;

import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// ISO/IEC 18013-5 mDOC proximity 프로토콜 로거
// 프로토콜 각 단계에서 오고 가는 데이터를 상세히 기록합니다.
// 다른 mDOC Reader 구현 시 참조할 수 있도록, 프로토콜 흐름 순서대로 로그를 출력합니다.
public class ProtocolLogger {
    private static final String TAG = "MDR/Protocol";
    private static boolean enabled = true;

    public static void setEnabled(boolean enable) {
        enabled = enable;
    }


    // Phase 1: 디바이스 인게이지먼트 (QR 코드 파싱)


    // QR 코드 수신 로깅
    public static void logQrCodeReceived(String qrCode) {
        if (!enabled) return;
        log("══════════════════════════════════════════════════════════════");
        log("Phase 1: Device Engagement 시작 / Device Engagement Start");
        log("══════════════════════════════════════════════════════════════");
        log("QR 코드 원본 / QR code raw: " + qrCode);
        if (qrCode.startsWith("mdoc:")) {
            log("  → 'mdoc:' prefix 제거 후 Base64URL 디코딩 수행");
            log("  → Strip 'mdoc:' prefix, then Base64URL decode");
        }
    }

    // DeviceEngagement CBOR 파싱 결과 로깅
    public static void logDeviceEngagementParsed(byte[] rawBytes, String structureType, boolean hasTag24) {
        if (!enabled) return;
        log("DeviceEngagement CBOR 디코딩 / CBOR decoded:");
        log("  raw bytes 길이 / length: " + rawBytes.length);
        log("  raw hex: " + toHex(rawBytes));
        log("  CBOR 구조 타입 / structure type: " + structureType);
        log("  Tag 24 래핑 여부 / Tag 24 wrapped: " + hasTag24);
    }

    // EDeviceKey (디바이스 임시 공개키) 파싱 결과 로깅
    public static void logEDeviceKey(ECPublicKey key) {
        if (!enabled) return;
        ECPoint w = key.getW();
        log("EDeviceKey (디바이스 임시 공개키 / device ephemeral public key):");
        log("  curve: P-256 (secp256r1)");
        log("  x: " + toHex(bigIntToFixedBytes(w.getAffineX(), 32)));
        log("  y: " + toHex(bigIntToFixedBytes(w.getAffineY(), 32)));
    }

    // BLE 연결 방식 로깅
    public static void logBleConnectionMethod(UUID peripheralUuid, UUID centralUuid) {
        if (!enabled) return;
        log("BLE 연결 방식 / BLE connection methods:");
        log("  Peripheral Server Mode UUID: " + (peripheralUuid != null ? peripheralUuid : "(없음/none)"));
        log("  Central Client Mode UUID: " + (centralUuid != null ? centralUuid : "(없음/none)"));
    }


    // Phase 2: 세션 수립 및 키 교환


    // eReaderKey (리더 임시 키 쌍) 생성 로깅
    public static void logEReaderKeyGenerated(ECPublicKey readerPublicKey) {
        if (!enabled) return;
        log("══════════════════════════════════════════════════════════════");
        log("Phase 2: Session Establishment 시작 / Session Establishment Start");
        log("══════════════════════════════════════════════════════════════");
        ECPoint w = readerPublicKey.getW();
        log("eReaderKey 생성 완료 / eReaderKey generated:");
        log("  curve: P-256 (secp256r1)");
        log("  x: " + toHex(bigIntToFixedBytes(w.getAffineX(), 32)));
        log("  y: " + toHex(bigIntToFixedBytes(w.getAffineY(), 32)));
    }

    // ECDH 키 교환 및 세션 키 도출 로깅
    public static void logSessionKeysDerivation(byte[] sharedSecret, byte[] salt,
                                                 byte[] skReader, byte[] skDevice, byte[] eMacKey) {
        if (!enabled) return;
        log("ECDH 키 합의 / ECDH key agreement:");
        log("  sharedSecret: " + toHex(sharedSecret));
        log("세션 키 도출 / Session key derivation (HKDF-SHA256):");
        log("  salt (= SHA-256(CBOR(Tag(24, SessionTranscript)))): " + toHex(salt));
        log("  SKReader (리더→디바이스 암호화 키 / reader→device encryption key): " + toHex(skReader));
        log("  SKDevice (디바이스→리더 암호화 키 / device→reader encryption key): " + toHex(skDevice));
        log("  EMacKey  (DeviceAuth MAC 검증 키 / DeviceAuth MAC verification key): " + toHex(eMacKey));
    }

    // SessionTranscript 구조 로깅
    public static void logSessionTranscript(byte[] encodedSessionTranscript,
                                             byte[] deviceEngagementBytes,
                                             byte[] eReaderKeyBytes) {
        if (!enabled) return;
        log("SessionTranscript 구성 / SessionTranscript structure:");
        log("  [0] DeviceEngagementBytes (Tag 24): " + toHex(deviceEngagementBytes));
        log("  [1] EReaderKeyBytes (Tag 24): " + toHex(eReaderKeyBytes));
        log("  [2] Handover: null (QR engagement)");
        log("  전체 인코딩 / full encoded: " + toHex(encodedSessionTranscript));
    }

    // 암호화/복호화 수행 로깅
    public static void logCrypto(String direction, int counter, byte[] iv,
                                  int inputLen, int outputLen) {
        if (!enabled) return;
        String dirKo = direction.equals("encrypt") ? "암호화" : "복호화";
        String role = direction.equals("encrypt") ? "Reader→Device" : "Device→Reader";
        log(dirKo + " 수행 / " + direction + " (" + role + "):");
        log("  counter: " + counter);
        log("  IV: " + toHex(iv));
        log("  입력 크기 / input size: " + inputLen + " bytes");
        log("  출력 크기 / output size: " + outputLen + " bytes");
    }

    // SessionEstablishment 메시지 로깅
    public static void logSessionEstablishment(byte[] eReaderKeyCbor, byte[] encryptedData,
                                                byte[] fullMessage) {
        if (!enabled) return;
        log("SessionEstablishment 메시지 구성 / SessionEstablishment message:");
        log("  eReaderKey (Tag 24 CBOR): " + toHex(eReaderKeyCbor));
        log("  data (암호화된 DeviceRequest / encrypted DeviceRequest): "
            + encryptedData.length + " bytes");
        log("  전체 메시지 / full message: " + toHex(fullMessage));
    }

    // SessionData 수신 로깅
    public static void logSessionDataReceived(byte[] sessionData, byte[] extractedData,
                                               boolean isTermination) {
        if (!enabled) return;
        log("SessionData 수신 / SessionData received:");
        log("  전체 CBOR / full CBOR: " + toHex(sessionData));
        if (isTermination) {
            log("  → 세션 종료 메시지 / session termination message");
        } else {
            log("  → data 필드 추출 / data field extracted: " + extractedData.length + " bytes");
        }
    }


    // Phase 3: DeviceRequest 구성


    // DeviceRequest 구성 로깅
    public static void logDeviceRequestBuilding(String docType, String namespace,
                                                 Map<String, Boolean> claims) {
        if (!enabled) return;
        log("──────────────────────────────────────────────────────────────");
        log("Phase 3: DeviceRequest 구성 / Building DeviceRequest");
        log("──────────────────────────────────────────────────────────────");
        log("DocRequest 항목 / DocRequest entry:");
        log("  docType: " + docType);
        log("  namespace: " + namespace);
        log("  요청 claim 목록 / requested claims:");
        for (Map.Entry<String, Boolean> entry : claims.entrySet()) {
            log("    - " + entry.getKey() + " (intentToRetain: " + entry.getValue() + ")");
        }
    }

    // ItemsRequest CBOR 로깅
    public static void logItemsRequestCbor(String docType, byte[] itemsRequestBytes) {
        if (!enabled) return;
        log("ItemsRequest CBOR (docType=" + docType + "):");
        log("  hex: " + toHex(itemsRequestBytes));
    }

    // 최종 DeviceRequest CBOR 로깅
    public static void logDeviceRequestComplete(byte[] deviceRequestBytes) {
        if (!enabled) return;
        log("최종 DeviceRequest CBOR / Final DeviceRequest CBOR:");
        log("  크기 / size: " + deviceRequestBytes.length + " bytes");
        log("  hex: " + toHex(deviceRequestBytes));
    }


    // Phase 4: BLE 전송 및 수신


    // BLE 연결 시작 로깅
    public static void logBleConnecting(String mode, UUID serviceUuid) {
        if (!enabled) return;
        log("══════════════════════════════════════════════════════════════");
        log("Phase 4: BLE Transport 시작 / BLE Transport Start");
        log("══════════════════════════════════════════════════════════════");
        log("BLE 연결 시작 / BLE connecting:");
        log("  모드 / mode: " + mode);
        log("  service UUID: " + serviceUuid);
    }

    // MTU 협상 결과 로깅
    public static void logMtuNegotiated(int mtu) {
        if (!enabled) return;
        log("MTU 협상 완료 / MTU negotiated: " + mtu);
        log("  최대 chunk 크기 / max chunk payload: " + (mtu - 3 - 1) + " bytes (MTU - ATT overhead - control byte)");
    }

    // GATT 서비스 발견 로깅
    public static void logGattServicesDiscovered(String serviceUuid, boolean hasState,
                                                  boolean hasC2S, boolean hasS2C) {
        if (!enabled) return;
        log("GATT 서비스 발견 / GATT service discovered:");
        log("  service: " + serviceUuid);
        log("  State characteristic: " + (hasState ? "있음/found" : "없음/not found"));
        log("  Client2Server characteristic: " + (hasC2S ? "있음/found" : "없음/not found"));
        log("  Server2Client characteristic: " + (hasS2C ? "있음/found" : "없음/not found"));
    }

    // 데이터 전송 (chunking) 로깅
    public static void logBleSendStart(int totalBytes, int chunkCount, int maxChunkSize) {
        if (!enabled) return;
        log("BLE 데이터 전송 시작 / BLE sending data:");
        log("  전체 크기 / total size: " + totalBytes + " bytes");
        log("  chunk 수 / chunk count: " + chunkCount);
        log("  chunk 최대 크기 / max chunk size: " + maxChunkSize + " bytes");
    }

    // 개별 chunk 전송 로깅
    public static void logBleChunkSent(int chunkIndex, int totalChunks, byte[] chunk) {
        if (!enabled) return;
        byte controlByte = chunk[0];
        String control = (controlByte == 0x01) ? "0x01 (더 있음/more)" : "0x00 (마지막/last)";
        log("  chunk [" + (chunkIndex + 1) + "/" + totalChunks + "]: "
            + chunk.length + " bytes, control=" + control);
        logHexDump("    ", chunk, 1, chunk.length - 1);
    }

    // 수신 chunk 로깅
    public static void logBleChunkReceived(byte[] chunk, boolean isLast, int accumulatedSize) {
        if (!enabled) return;
        byte controlByte = chunk[0];
        String control = isLast ? "0x00 (마지막/last)" : "0x01 (더 있음/more)";
        log("BLE chunk 수신 / BLE chunk received:");
        log("  크기 / size: " + chunk.length + " bytes, control=" + control);
        log("  누적 크기 / accumulated: " + accumulatedSize + " bytes");
        logHexDump("  ", chunk, 1, chunk.length - 1);
    }

    // 전체 수신 데이터 조립 완료 로깅
    public static void logBleDataAssembled(byte[] completeData) {
        if (!enabled) return;
        log("BLE 데이터 조립 완료 / BLE data assembly complete:");
        log("  전체 크기 / total size: " + completeData.length + " bytes");
        log("  hex: " + toHex(completeData));
    }

    // BLE State characteristic 변경 로깅
    public static void logBleStateUpdate(byte stateValue) {
        if (!enabled) return;
        String desc;
        if (stateValue == 0x01) desc = "START (연결 시작)";
        else if (stateValue == 0x02) desc = "END (세션 종료)";
        else desc = "UNKNOWN (0x" + String.format("%02x", stateValue) + ")";
        log("BLE State 변경 / BLE state update: " + desc);
    }


    // Phase 5: DeviceResponse 파싱 및 검증


    // DeviceResponse 파싱 시작 로깅
    public static void logDeviceResponseParseStart(byte[] responseBytes) {
        if (!enabled) return;
        log("══════════════════════════════════════════════════════════════");
        log("Phase 5: DeviceResponse 파싱 / Parsing DeviceResponse");
        log("══════════════════════════════════════════════════════════════");
        log("DeviceResponse CBOR 수신 / DeviceResponse CBOR received:");
        log("  크기 / size: " + responseBytes.length + " bytes");
        log("  hex: " + toHex(responseBytes));
    }

    // 개별 Document 파싱 시작 로깅
    public static void logDocumentParsing(String docType, int docIndex, int totalDocs) {
        if (!enabled) return;
        log("──────────────────────────────────────────────────────────────");
        log("Document [" + (docIndex + 1) + "/" + totalDocs + "] 파싱 / parsing:");
        log("  docType: " + docType);
    }

    // IssuerSigned namespace 파싱 로깅
    public static void logNamespaceClaims(String namespace, Map<String, Object> claims) {
        if (!enabled) return;
        log("  namespace: " + namespace);
        log("    claim 수 / claim count: " + claims.size());
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            Object val = entry.getValue();
            String valStr;
            if (val instanceof byte[]) {
                valStr = "[bytes:" + ((byte[]) val).length + "]";
            } else if (val instanceof String && ((String) val).length() > 100) {
                valStr = ((String) val).substring(0, 100) + "... (" + ((String) val).length() + " chars)";
            } else {
                valStr = String.valueOf(val);
            }
            log("    - " + entry.getKey() + " = " + valStr);
        }
    }

    // IssuerAuth (COSE_Sign1) 파싱 로깅
    public static void logIssuerAuth(byte[] protectedHeaders, byte[] payload, byte[] signature) {
        if (!enabled) return;
        log("  IssuerAuth (COSE_Sign1) 파싱 / parsing:");
        log("    protectedHeaders: " + toHex(protectedHeaders));
        log("    payload (MSO): " + payload.length + " bytes");
        log("    payload hex: " + toHex(payload));
        log("    signature: " + toHex(signature));
    }

    // MSO validityInfo 로깅
    public static void logMsoValidity(String signed, String validFrom, String validUntil) {
        if (!enabled) return;
        log("  MSO validityInfo:");
        log("    signed (서명 시점 / signing time): " + signed);
        log("    validFrom (유효 시작 / valid from): " + validFrom);
        log("    validUntil (유효 만료 / valid until): " + validUntil);
    }

    // 서명 검증 결과 로깅
    public static void logSignatureVerification(String type, String algorithm, boolean result) {
        if (!enabled) return;
        String resultKo = result ? "성공/PASS" : "실패/FAIL";
        log("  " + type + " 서명 검증 / signature verification:");
        log("    알고리즘 / algorithm: " + algorithm);
        log("    결과 / result: " + resultKo);
    }

    // Issuer 인증서 정보 로깅 (x5chain)
    public static void logIssuerCertificate(String subject, String issuer) {
        if (!enabled) return;
        log("  Issuer 인증서 / Issuer certificate (from x5chain):");
        log("    Subject: " + subject);
        log("    Issuer: " + issuer);
    }

    // DeviceAuth 검증 로깅
    public static void logDeviceAuth(String authType, byte[] deviceAuthenticationBytes) {
        if (!enabled) return;
        log("  DeviceAuth 검증 / DeviceAuth verification:");
        log("    인증 방식 / auth type: " + authType);
        log("    DeviceAuthenticationBytes 크기 / size: " + deviceAuthenticationBytes.length + " bytes");
        log("    hex: " + toHex(deviceAuthenticationBytes));
    }

    // MAC 검증 로깅
    public static void logMacVerification(byte[] computedTag, byte[] receivedTag, boolean result) {
        if (!enabled) return;
        String resultKo = result ? "일치/MATCH" : "불일치/MISMATCH";
        log("  COSE_Mac0 검증 / verification:");
        log("    계산된 tag / computed tag: " + toHex(computedTag));
        log("    수신된 tag / received tag: " + toHex(receivedTag));
        log("    결과 / result: " + resultKo);
    }

    // 개별 항목 Digest 검증 로깅
    public static void logDigestVerification(long digestId, byte[] computed, byte[] expected,
                                              boolean match) {
        if (!enabled) return;
        String resultKo = match ? "일치/MATCH" : "불일치/MISMATCH";
        log("    digestID=" + digestId + ": " + resultKo);
        if (!match) {
            log("      계산값 / computed: " + toHex(computed));
            log("      기대값 / expected: " + toHex(expected));
        }
    }

    // MSO digest 알고리즘 및 전체 결과 로깅
    public static void logDigestOverall(String algorithm, int totalItems, boolean allMatch) {
        if (!enabled) return;
        String resultKo = allMatch ? "모두 일치/ALL MATCH" : "불일치 있음/MISMATCH FOUND";
        log("  데이터 무결성 검증 / Data integrity verification:");
        log("    digest 알고리즘 / algorithm: " + algorithm);
        log("    검증 항목 수 / items verified: " + totalItems);
        log("    결과 / result: " + resultKo);
    }


    // Wi-Fi Aware 연결 방식 로깅
    public static void logWifiAwareConnectionMethod(String passphrase, Integer channel, Integer band) {
        if (!enabled) return;
        log("Wi-Fi Aware 연결 방식 / Wi-Fi Aware connection method:");
        log("  passphrase: " + (passphrase != null ? "***(" + passphrase.length() + " chars)" : "(없음/none)"));
        log("  channel info: " + (channel != null ? channel : "(없음/none)"));
        log("  band info: " + (band != null ? band : "(없음/none)"));
    }

    // NFC 연결 방식 로깅
    public static void logNfcConnectionMethod(Integer maxCommandLen, Integer maxResponseLen) {
        if (!enabled) return;
        log("NFC 연결 방식 / NFC connection method:");
        log("  max command data length: " + (maxCommandLen != null ? maxCommandLen : "(없음/none)"));
        log("  max response data length: " + (maxResponseLen != null ? maxResponseLen : "(없음/none)"));
    }

    // Wi-Fi Aware 전송 시작 로깅
    public static void logWifiAwareConnecting() {
        if (!enabled) return;
        log("══════════════════════════════════════════════════════════════");
        log("Phase 4: Wi-Fi Aware Transport 시작 / Wi-Fi Aware Transport Start");
        log("══════════════════════════════════════════════════════════════");
    }

    // Wi-Fi Aware 피어 발견 로깅
    public static void logWifiAwarePeerDiscovered() {
        if (!enabled) return;
        log("Wi-Fi Aware 피어 발견 / Wi-Fi Aware peer discovered");
    }

    // Wi-Fi Aware TCP 연결 로깅
    public static void logWifiAwareTcpConnected(String address, int port) {
        if (!enabled) return;
        log("Wi-Fi Aware TCP 연결 완료 / TCP connected:");
        log("  address: " + address);
        log("  port: " + port);
    }

    // Wi-Fi Aware 데이터 전송 로깅
    public static void logWifiAwareSendStart(int totalBytes) {
        if (!enabled) return;
        log("Wi-Fi Aware 데이터 전송 / sending data: " + totalBytes + " bytes");
    }

    // Wi-Fi Aware 데이터 수신 로깅
    public static void logWifiAwareDataReceived(int totalBytes) {
        if (!enabled) return;
        log("Wi-Fi Aware 데이터 수신 / data received: " + totalBytes + " bytes");
    }

    // NFC 전송 시작 로깅
    public static void logNfcConnecting() {
        if (!enabled) return;
        log("══════════════════════════════════════════════════════════════");
        log("Phase 4: NFC Transport 시작 / NFC Transport Start");
        log("══════════════════════════════════════════════════════════════");
    }

    // NFC SELECT 결과 로깅
    public static void logNfcSelectResult(boolean success) {
        if (!enabled) return;
        String resultStr = success ? "성공/SUCCESS" : "실패/FAILED";
        log("NFC SELECT (mDOC AID): " + resultStr);
    }

    // NFC ENVELOPE 전송 로깅
    public static void logNfcEnvelopeSent(int chunkIndex, int totalChunks, int chunkSize) {
        if (!enabled) return;
        log("NFC ENVELOPE [" + (chunkIndex + 1) + "/" + totalChunks + "]: " + chunkSize + " bytes");
    }

    // NFC GET RESPONSE 수신 로깅
    public static void logNfcGetResponseReceived(int chunkSize, boolean hasMore) {
        if (!enabled) return;
        String more = hasMore ? "(추가 데이터 있음/more data)" : "(마지막/last)";
        log("NFC GET RESPONSE: " + chunkSize + " bytes " + more);
    }


    // Phase 6: 최종 결과


    // 최종 결과 요약 로깅
    public static void logTransferResult(int documentCount,
                                          List<String> docTypes,
                                          List<Integer> claimCounts,
                                          List<Boolean> trusted,
                                          List<Boolean> issuerSigValid,
                                          List<Boolean> deviceSigValid,
                                          List<Boolean> dataIntegrity) {
        if (!enabled) return;
        log("══════════════════════════════════════════════════════════════");
        log("Phase 6: 전송 결과 요약 / Transfer Result Summary");
        log("══════════════════════════════════════════════════════════════");
        log("수신 문서 수 / documents received: " + documentCount);
        for (int i = 0; i < documentCount; i++) {
            log("  Document [" + (i + 1) + "/" + documentCount + "]:");
            log("    docType: " + docTypes.get(i));
            log("    claim 수 / claims: " + claimCounts.get(i));
            log("    신뢰 여부 / trusted: " + formatBool(trusted.get(i)));
            log("    Issuer 서명 / issuer signature: " + formatBool(issuerSigValid.get(i)));
            log("    Device 서명 / device signature: " + formatBool(deviceSigValid.get(i)));
            log("    데이터 무결성 / data integrity: " + formatBool(dataIntegrity.get(i)));
        }
        log("══════════════════════════════════════════════════════════════");
    }


    // 헬퍼 메서드


    private static void log(String message) {
        Log.d(TAG, message);
    }

    // byte[] → hex 문자열 변환 (최대 512바이트까지 출력, 초과 시 truncate)
    public static String toHex(byte[] bytes) {
        if (bytes == null) return "(null)";
        if (bytes.length == 0) return "(empty)";
        int limit = Math.min(bytes.length, 512);
        StringBuilder sb = new StringBuilder(limit * 2 + 10);
        for (int i = 0; i < limit; i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        if (bytes.length > limit) {
            sb.append("...(").append(bytes.length).append(" bytes total)");
        }
        return sb.toString();
    }

    // hex dump (offset, length 지정 가능)
    private static void logHexDump(String prefix, byte[] data, int offset, int length) {
        if (length <= 0) return;
        int limit = Math.min(length, 128);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            sb.append(String.format("%02x", data[offset + i]));
        }
        if (length > limit) {
            sb.append("...(").append(length).append(" bytes total)");
        }
        log(prefix + "payload hex: " + sb);
    }

    private static String formatBool(Boolean value) {
        if (value == null) return "확인 불가/N/A";
        return value ? "유효/VALID" : "무효/INVALID";
    }

    private static byte[] bigIntToFixedBytes(java.math.BigInteger val, int length) {
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
}
