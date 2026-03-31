---
puppeteer:
  pdf:
    format: A4
    displayHeaderFooter: true
    landscape: false
    scale: 0.8
    margin:
      top: 1.2cm
      right: 1cm
      bottom: 1cm
      left: 1cm
  image:
    quality: 100
    fullPage: false
---

MSO mDoc SDK Error
==

- 주제: MsoMdocSDKError
- 작성: 오픈소스개발팀
- 일자: 2026-03-31
- 버전: v1.0.0

| 버전   | 일자       | 변경 내용                 |
| ------ | ---------- | -------------------------|
| v1.0.0 | 2026-03-31 | 초기 작성                 |

<div style="page-break-after: always;"></div>

# 목차
- [Model](#model)
    - [Error Response (Reader)](#error-response-reader)
    - [Error Response (OID4VC)](#error-response-oid4vc)
- [Reader Error Code](#reader-error-code)
    - [1. BLE (00xxx)](#1-ble-00xxx)
    - [2. 세션 및 암호화 (01xxx)](#2-세션-및-암호화-01xxx)
    - [3. 인증서 및 데이터 (02xxx)](#3-인증서-및-데이터-02xxx)
    - [4. Wi-Fi Aware (03xxx)](#4-wi-fi-aware-03xxx)
    - [5. NFC (04xxx)](#5-nfc-04xxx)
    - [6. 공통 (99xxx)](#6-공통-99xxx)
- [OID4VC Error Code](#oid4vc-error-code)
    - [1. General (00xxx)](#1-general-00xxx)
    - [2. Crypto (01xxx)](#2-crypto-01xxx)
    - [3. JWT (02xxx)](#3-jwt-02xxx)
    - [4. mDoc (03xxx)](#4-mdoc-03xxx)
    - [5. Disclosure (04xxx)](#5-disclosure-04xxx)
    - [6. Frame (05xxx)](#6-frame-05xxx)
    - [7. Key Binding (06xxx)](#7-key-binding-06xxx)
    - [8. JSON (07xxx)](#8-json-07xxx)
    - [9. Encoding (08xxx)](#9-encoding-08xxx)
    - [10. Hash (09xxx)](#10-hash-09xxx)
    - [11. Salt (10xxx)](#11-salt-10xxx)
    - [12. Validation (11xxx)](#12-validation-11xxx)
    - [13. OID4VP (13xxx)](#13-oid4vp-13xxx)

# Model

## Error Response (Reader)

### Description
```
mDoc Reader SDK의 에러 구조체입니다. feature + code 조합으로 에러를 식별합니다.
코드는 MSDKMDC로 시작합니다.
```

### Declaration
```java
public class MdocReaderException extends Exception {
    protected final MdocReaderErrorCode errorCode;
    protected final String message;
}
```

### Property

| Name      | Type               | Description                      | **M/O** | **Note** |
|-----------|--------------------|----------------------------------|---------|----------|
| errorCode | MdocReaderErrorCode | 에러 코드 enum                   | M       |          |
| message   | String             | 추가 에러 메시지                  | O       |          |

<br>

## Error Response (OID4VC)

### Description
```
mDoc OID4VC SDK의 에러 구조체입니다.
코드는 MSDKMDC로 시작합니다.
```

### Declaration
```java
public class MdocException extends Exception {
    protected final String message;
}
```

<br>

# Reader Error Code

## 1. BLE (00xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC00000  | Bluetooth not available            | -           | 기기의 Bluetooth 지원 여부 확인           |
| MSDKMDC00001  | BLE scanner not available          | -           | BLE 스캐너 가용성 확인                    |
| MSDKMDC00100  | BLE connection failed              | -           | BLE 연결 재시도                           |
| MSDKMDC00101  | BLE scan failed                    | -           | BLE 스캔 권한 및 상태 확인                |
| MSDKMDC00200  | mDOC BLE service not found         | -           | Holder의 BLE 서비스 UUID 확인             |
| MSDKMDC00201  | BLE advertising failed             | -           | BLE 광고 설정 확인                        |
| MSDKMDC00300  | No BLE connection method available | -           | DeviceEngagement에 BLE 연결 정보 확인     |
| MSDKMDC00301  | Could not open GATT server         | -           | GATT 서버 초기화 확인                     |
| MSDKMDC00302  | Service discovery failed           | -           | GATT 서비스 탐색 재시도                   |
| MSDKMDC00400  | Failed to send data                | -           | BLE 연결 상태 확인 후 재시도              |
| MSDKMDC00401  | Failed to receive data             | -           | BLE 연결 상태 확인                        |
| MSDKMDC00500  | Invalid session data               | -           | SessionData CBOR 형식 확인                |
| MSDKMDC00501  | Failed to send session termination | -           | 세션 종료 메시지 전송 실패                |
| MSDKMDC00600  | Bluetooth permission denied        | -           | Bluetooth 권한 요청                       |

<br>

## 2. 세션 및 암호화 (01xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC01000  | Session encryption failed          | -           | 암호화 키 및 카운터 상태 확인             |
| MSDKMDC01001  | Session decryption failed          | -           | 세션 키 일치 여부 확인                    |
| MSDKMDC01100  | Invalid device engagement          | -           | DeviceEngagement CBOR 구조 확인           |
| MSDKMDC01101  | Invalid CBOR data                  | -           | CBOR 데이터 형식 확인                     |
| MSDKMDC01102  | Invalid COSE_Key format            | -           | COSE_Key 구조 확인                        |
| MSDKMDC01103  | Missing EC coordinates in key      | -           | EC 공개키의 x, y 좌표 확인               |
| MSDKMDC01200  | Failed to build device request     | -           | DeviceRequest 구성 파라미터 확인          |
| MSDKMDC01201  | Failed to parse device response    | -           | DeviceResponse CBOR 구조 확인             |
| MSDKMDC01300  | Trust validation failed            | -           | 신뢰 인증서 체인 확인                     |
| MSDKMDC01400  | Transfer manager not initialized   | -           | TransferManager 초기화 후 사용            |

<br>

## 3. 인증서 및 데이터 (02xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC02000  | Failed to load certificate         | -           | 인증서 파일 경로 및 형식 확인             |
| MSDKMDC02001  | Failed to parse certificate        | -           | PEM 인증서 형식 확인                      |
| MSDKMDC02100  | Failed to parse claim value        | -           | Claim 값의 CBOR 타입 확인                 |
| MSDKMDC02200  | Resource not found                 | -           | 리소스 경로 확인                          |

<br>

## 4. Wi-Fi Aware (03xxx)

| Error Code    | Error Message                              | Description | Action Required                          |
|---------------|--------------------------------------------|-------------|------------------------------------------|
| MSDKMDC03000  | Wi-Fi Aware not available                  | -           | 기기의 Wi-Fi Aware 지원 여부 확인         |
| MSDKMDC03001  | Wi-Fi Aware requires Android Q+            | -           | Android 10 (API 29) 이상 필요             |
| MSDKMDC03100  | Wi-Fi Aware attach failed                  | -           | Wi-Fi Aware 세션 연결 재시도              |
| MSDKMDC03101  | Wi-Fi Aware subscribe failed               | -           | Subscribe 설정 확인                       |
| MSDKMDC03102  | Wi-Fi Aware peer not found                 | -           | Holder의 Publish 상태 확인                |
| MSDKMDC03200  | Wi-Fi Aware network request failed         | -           | 네트워크 요청 설정 확인                   |
| MSDKMDC03201  | Wi-Fi Aware TCP connection failed          | -           | TCP 소켓 연결 확인                        |
| MSDKMDC03300  | No Wi-Fi Aware connection method available | -           | DeviceEngagement에 Wi-Fi Aware 정보 확인  |

<br>

## 5. NFC (04xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC04000  | NFC not available                  | -           | 기기의 NFC 지원 여부 확인                 |
| MSDKMDC04100  | NFC tag lost during transfer       | -           | NFC 태그 근접 유지                        |
| MSDKMDC04101  | NFC SELECT command failed          | -           | mDOC AID SELECT 결과 확인                 |
| MSDKMDC04200  | NFC APDU error                     | -           | APDU 응답 SW 코드 확인                    |
| MSDKMDC04300  | No NFC connection method available | -           | DeviceEngagement에 NFC 정보 확인          |

<br>

## 6. 공통 (99xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC99999  | Unknown error                      | -           | 로그 확인                                 |

<br>

# OID4VC Error Code

## 1. General (00xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC00000  | mso-MDOC General Error             | -           | 로그 확인                                 |
| MSDKMDC00001  | Invalid parameter                  | -           | 파라미터 값 확인                          |
| MSDKMDC00002  | Parameter cannot be null           | -           | null이 아닌 값 제공                       |
| MSDKMDC00003  | Parameter cannot be empty          | -           | 비어있지 않은 값 제공                     |
| MSDKMDC00004  | Unsupported operation              | -           | 지원되는 연산인지 확인                    |

<br>

## 2. Crypto (01xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC01000  | Crypto Error                       | -           | 암호화 설정 확인                          |
| MSDKMDC01001  | Invalid key                        | -           | 키 형식 및 유효성 확인                    |
| MSDKMDC01002  | Signature operation failed         | -           | 서명 키 및 알고리즘 확인                  |
| MSDKMDC01003  | Signature verification failed      | -           | 서명 및 공개키 확인                       |
| MSDKMDC01004  | Unsupported algorithm              | -           | 지원 알고리즘 사용 (ES256 등)             |
| MSDKMDC01005  | Unsupported key type               | -           | 지원 키 타입 사용                         |
| MSDKMDC01006  | Algorithm not available            | -           | 암호화 프로바이더 설치 확인               |
| MSDKMDC01007  | Invalid signature format           | -           | 서명 인코딩 형식 확인                     |

<br>

## 3. JWT (02xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC02000  | JWT Error                          | -           | JWT 구조 확인                             |
| MSDKMDC02001  | Invalid JWT format                 | -           | JWT 형식 확인                             |
| MSDKMDC02002  | JWT parse failed                   | -           | JWT 파싱 데이터 확인                      |
| MSDKMDC02003  | JWT header missing                 | -           | JWT 헤더 포함 확인                        |
| MSDKMDC02004  | JWT payload missing                | -           | JWT 페이로드 포함 확인                    |
| MSDKMDC02005  | JWT sign failed                    | -           | JWT 서명 키 확인                          |

<br>

## 4. mDoc (03xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC03000  | mso-MDOC Error                     | -           | mDoc 데이터 확인                          |
| MSDKMDC03001  | Invalid mso-MDOC format            | -           | mDoc CBOR 형식 확인                       |
| MSDKMDC03002  | mso-MDOC parse failed              | -           | mDoc 구조 확인                            |
| MSDKMDC03003  | mso-MDOC build failed              | -           | mDoc 구성 파라미터 확인                   |

<br>

## 5. Disclosure (04xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC04000  | Disclosure Error                   | -           | 선택적 공개 설정 확인                     |
| MSDKMDC04001  | Invalid disclosure format          | -           | Disclosure 형식 확인                      |
| MSDKMDC04002  | Disclosure parse failed            | -           | Disclosure 데이터 확인                    |
| MSDKMDC04003  | Disclosure create failed           | -           | Disclosure 생성 파라미터 확인             |
| MSDKMDC04004  | Disclosure hash mismatch           | -           | 해시값 불일치, 데이터 무결성 확인          |
| MSDKMDC04005  | Invalid disclosure salt            | -           | Disclosure salt 값 확인                   |
| MSDKMDC04006  | Invalid disclosure claim name      | -           | Disclosure claim 이름 확인                |
| MSDKMDC04007  | Array element disclosure not supported in this context | - | 배열 요소 Disclosure 미지원          |

<br>

## 6. Frame (05xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC05000  | Disclosure Frame Error             | -           | 프레임 구조 확인                          |
| MSDKMDC05001  | Invalid disclosure frame format    | -           | 프레임 형식 확인                          |
| MSDKMDC05002  | Disclosure frame parse failed      | -           | 프레임 파싱 데이터 확인                   |
| MSDKMDC05003  | Invalid field in disclosure frame  | -           | 프레임 필드 확인                          |
| MSDKMDC05004  | Reserved field name in disclosure frame | -      | 예약된 필드명 사용 불가                   |
| MSDKMDC05005  | Disclosure frame references non-existent claim | - | 존재하지 않는 claim 참조                |
| MSDKMDC05006  | Invalid nested type in disclosure frame | -      | 중첩 타입 확인                            |

<br>

## 7. Key Binding (06xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC06000  | Key Binding Error                  | -           | 키 바인딩 설정 확인                       |
| MSDKMDC06001  | Invalid key binding JWT format     | -           | 키 바인딩 JWT 형식 확인                   |
| MSDKMDC06002  | Key binding JWT build failed       | -           | 키 바인딩 JWT 구성 확인                   |
| MSDKMDC06003  | Invalid holder key                 | -           | Holder 키 확인                            |

<br>

## 8. JSON (07xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC07000  | JSON Processing Error              | -           | JSON 처리 설정 확인                       |
| MSDKMDC07001  | JSON processing failed             | -           | JSON 데이터 확인                          |
| MSDKMDC07002  | JSON mapping failed                | -           | JSON 매핑 대상 확인                       |
| MSDKMDC07003  | JSON serialization failed          | -           | 직렬화 대상 객체 확인                     |
| MSDKMDC07004  | JSON deserialization failed        | -           | JSON 형식 및 대상 타입 확인               |

<br>

## 9. Encoding (08xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC08000  | Encoding Error                     | -           | 인코딩 설정 확인                          |
| MSDKMDC08001  | Base64 encoding/decoding failed    | -           | Base64 데이터 형식 확인                   |
| MSDKMDC08002  | Unsupported encoding               | -           | 지원 인코딩 사용                          |
| MSDKMDC08003  | Invalid encoding format            | -           | 인코딩 형식 확인                          |

<br>

## 10. Hash (09xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC09000  | Hash Error                         | -           | 해시 설정 확인                            |
| MSDKMDC09001  | Hash algorithm not available       | -           | 지원 해시 알고리즘 사용 (SHA-256 등)      |
| MSDKMDC09002  | Unsupported hash algorithm         | -           | 지원 알고리즘 확인                        |
| MSDKMDC09003  | Hash computation failed            | -           | 입력 데이터 확인                          |

<br>

## 11. Salt (10xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC10000  | Salt Error                         | -           | Salt 설정 확인                            |
| MSDKMDC10001  | Invalid salt length                | -           | Salt 길이 확인                            |
| MSDKMDC10002  | Salt generation failed             | -           | Salt 생성 로직 확인                       |

<br>

## 12. Validation (11xxx)

| Error Code    | Error Message                      | Description | Action Required                          |
|---------------|------------------------------------|-------------|------------------------------------------|
| MSDKMDC11000  | Validation Error                   | -           | 검증 설정 확인                            |
| MSDKMDC11001  | Validation failed                  | -           | 검증 대상 데이터 확인                     |
| MSDKMDC11002  | Required claim not found           | -           | 요청한 claim 존재 여부 확인               |

<br>

## 13. OID4VP (13xxx)

| Error Code    | Error Message                              | Description | Action Required                          |
|---------------|--------------------------------------------|-------------|------------------------------------------|
| MSDKMDC13000  | OID4VP Error                               | -           | OID4VP 처리 설정 확인                     |
| MSDKMDC13001  | OID4VP mdoc parse failed                   | -           | VP Token 형식 확인                        |
| MSDKMDC13002  | OID4VP invalid mdoc credential             | -           | Credential 구조 확인                      |
| MSDKMDC13003  | OID4VP no x5chain found in IssuerAuth      | -           | IssuerAuth에 x5chain 포함 확인            |
| MSDKMDC13004  | OID4VP IssuerAuth signature verification failed | -      | 서명 및 인증서 확인                       |
| MSDKMDC13005  | OID4VP IssuerSignedItem digest mismatch    | -           | MSO digest 값 확인                        |
| MSDKMDC13006  | OID4VP DeviceAuth verification failed      | -           | DeviceAuth MAC/서명 확인                  |
| MSDKMDC13007  | OID4VP unsupported key type or curve       | -           | 지원 키 타입 사용                         |

<br>
