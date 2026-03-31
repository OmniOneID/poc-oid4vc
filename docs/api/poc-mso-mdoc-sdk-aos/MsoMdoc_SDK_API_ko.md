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
---

MSO mDoc SDK API
==

- 주제: MSO mDoc SDK API
- 작성: 오픈소스개발팀
- 일자: 2026-03-31
- 버전: v1.0.0

| 버전   | 일자       | 변경 내용                 |
| ------ | ---------- | -------------------------|
| v1.0.0 | 2026-03-31 | 초기 작성                 |

<div style="page-break-after: always;"></div>

# 목차
- [개요](#개요)
- [1. Reader APIs](#1-reader-apis)
    - [1.1 TransferController](#11-transfercontroller)
        - [1.1.1 initializeVerifier](#111-initializeverifier)
        - [1.1.2 initializeTransferManager](#112-initializetransfermanager)
        - [1.1.3 startEngagement](#113-startengagement)
        - [1.1.4 sendRequest](#114-sendrequest)
        - [1.1.5 stopConnection](#115-stopconnection)
    - [1.2 DeviceEngagement](#12-deviceengagement)
        - [1.2.1 fromQrCode](#121-fromqrcode)
        - [1.2.2 fromBytes](#122-frombytes)
    - [1.3 TrustManager](#13-trustmanager)
        - [1.3.1 isDocumentTrusted](#131-isdocumenttrusted)
- [2. Holder APIs](#2-holder-apis)
    - [2.1 MdocSessionManager](#21-mdocsessionmanager)
        - [2.1.1 decryptSessionEstablishment](#211-decryptsessionestablishment)
        - [2.1.2 generateDeviceResponse](#212-generatedeviceresponse)
    - [2.2 MdocProximityServer](#22-mdocproximityserver)
- [3. OID4VP APIs](#3-oid4vp-apis)
    - [3.1 OID4VPHandler](#31-oid4vphandler)
        - [3.1.1 createVPToken](#311-createvptoken)
    - [3.2 MDocVerifier](#32-mdocverifier)
        - [3.2.1 verify](#321-verify)
        - [3.2.2 verifyWithX5c](#322-verifywithx5c)
- [4. Data Classes](#4-data-classes)
    - [4.1 TransportConfig](#41-transportconfig)
    - [4.2 EngagementSource](#42-engagementsource)
    - [4.3 RequestedDocument](#43-requesteddocument)
    - [4.4 TransferStatus](#44-transferstatus)
    - [4.5 ReceivedDocument](#45-receiveddocument)
    - [4.6 DocumentValidity](#46-documentvalidity)
- [5. Enums](#5-enums)
    - [5.1 AttestationType](#51-attestationtype)
    - [5.2 DocumentMode](#52-documentmode)

<div style="page-break-after: always;"></div>

## 개요

본 문서는 **ISO/IEC 18013-5** 기반 mDoc proximity 프레젠테이션 및 **OpenID4VP** 클라이언트 VP Token 생성/검증 등을 위한 SDK API를 정의합니다.

### 주요 기능
- **Reader**: BLE / Wi-Fi Aware / NFC 기반 mDoc 요청 및 수신
- **Holder**: BLE / NFC 기반 mDoc 응답 (Peripheral/Server 역할)
- **OID4VP**: VP Token 생성 및 검증 (선택적 공개, DeviceAuth 포함)

<br>

<div style="page-break-after: always;"></div>

# 1. Reader APIs

## 1.1 TransferController

### 1.1.1 initializeVerifier

### Class Name
`TransferController`

### Function Name
`initializeVerifier`

### Function Introduction
`Issuer 인증서 신뢰 검증을 초기화합니다. PEM 형식의 신뢰 루트 인증서 목록을 설정합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| certificates | List\<String\> | PEM 형식 신뢰 루트 인증서 목록 | M | |
| skipIssuerTrust | boolean | Issuer 신뢰 검증 건너뛰기 | M | 테스트용 |
| includeSystemRoots | boolean | 시스템 루트 인증서 포함 여부 | O | 기본값 false |

### Function Declaration

```java
void initializeVerifier(List<String> certificates, boolean skipIssuerTrust)

void initializeVerifier(List<String> certificates, boolean skipIssuerTrust, boolean includeSystemRoots)
```

### Function Usage
```java
TransferController controller = new TransferController(context);

List<String> certs = List.of("-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----");
controller.initializeVerifier(certs, false, true);
```

<br>

### 1.1.2 initializeTransferManager

### Class Name
`TransferController`

### Function Name
`initializeTransferManager`

### Function Introduction
`전송 방식(BLE, Wi-Fi Aware, NFC)에 따른 TransportManager를 초기화합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| config | TransportConfig | 전송 방식별 설정 | M | [Link](#41-transportconfig) |

### Function Declaration

```java
void initializeTransferManager(TransportConfig config)
```

### Function Usage
```java
// BLE
controller.initializeTransferManager(
    new TransportConfig.Ble(true, peripheralUuid, null));

// Wi-Fi Aware
controller.initializeTransferManager(
    new TransportConfig.WifiAware(passphrase, null, null));

// NFC
controller.initializeTransferManager(
    new TransportConfig.Nfc(isoDep, 255, 256));
```

<br>

### 1.1.3 startEngagement

### Class Name
`TransferController`

### Function Name
`startEngagement`

### Function Introduction
`DeviceEngagement를 파싱하고 전송 채널 연결을 시작합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| source | EngagementSource | Engagement 데이터 소스 | M | [Link](#42-engagementsource) |

### Function Declaration

```java
void startEngagement(EngagementSource source)
```

### Function Usage
```java
// QR 코드에서
controller.startEngagement(new EngagementSource.QrCode("mdoc:owBjMS..."));

// NFC에서
controller.startEngagement(new EngagementSource.Nfc(engagementBytes));
```

<br>

### 1.1.4 sendRequest

### Class Name
`TransferController`

### Function Name
`sendRequest`

### Function Introduction
`Holder에게 mDoc claim 요청을 전송하고, 콜백으로 결과를 수신합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| requestedDocs | List\<RequestedDocument\> | 요청 문서 목록 | M | [Link](#43-requesteddocument) |
| retainData | boolean | 데이터 보관 의사 (intentToRetain) | M | |
| callback | TransferCallback | 상태 변경 콜백 | M | |

### Function Declaration

```java
void sendRequest(List<RequestedDocument> requestedDocs, boolean retainData, TransferCallback callback)
```

### Function Usage
```java
List<String> claims = List.of("family_name", "given_name", "birth_date");
RequestedDocument doc = new RequestedDocument("mdl", AttestationType.MDL, DocumentMode.ISSUER_SIGNED_ONLY, claims);

controller.sendRequest(List.of(doc), false, status -> {
    switch (status.getType()) {
        case RESPONSE_RECEIVED:
            List<ReceivedDocument> docs = status.getReceivedDocuments();
            break;
        case ERROR:
            String error = status.getErrorMessage();
            break;
    }
});
```

<br>

### 1.1.5 stopConnection

### Class Name
`TransferController`

### Function Name
`stopConnection`

### Function Introduction
`전송 세션을 종료하고 리소스를 정리합니다.`

### Function Declaration

```java
void stopConnection()
```

<br>

## 1.2 DeviceEngagement

### 1.2.1 fromQrCode

### Class Name
`DeviceEngagement`

### Function Name
`fromQrCode`

### Function Introduction
`QR 코드 문자열에서 DeviceEngagement를 파싱합니다. "mdoc:" 접두사를 자동 처리합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| qrCode | String | QR 코드 문자열 | M | Base64URL 인코딩 |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| DeviceEngagement | 파싱된 Engagement 객체 | M | |

### Function Declaration

```java
static DeviceEngagement fromQrCode(String qrCode) throws Exception
```

<br>

### 1.2.2 fromBytes

### Class Name
`DeviceEngagement`

### Function Name
`fromBytes`

### Function Introduction
`NFC 등으로 수신한 바이트 배열에서 DeviceEngagement를 파싱합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| data | byte[] | CBOR 인코딩된 DeviceEngagement | M | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| DeviceEngagement | 파싱된 Engagement 객체 | M | |

### Function Declaration

```java
static DeviceEngagement fromBytes(byte[] data) throws Exception
```

<br>

## 1.3 TrustManager

### 1.3.1 isDocumentTrusted

### Class Name
`TrustManager`

### Function Name
`isDocumentTrusted`

### Function Introduction
`수신한 문서의 Issuer 인증서가 신뢰 루트 인증서로 체이닝되는지 PKIX로 검증합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| document | ParsedDocument | 파싱된 mDoc 문서 | M | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| boolean | 신뢰 여부 | M | |

### Function Declaration

```java
boolean isDocumentTrusted(DeviceResponseParser.ParsedDocument document)
```

<br>

<div style="page-break-after: always;"></div>

# 2. Holder APIs

## 2.1 MdocSessionManager

### 2.1.1 decryptSessionEstablishment

### Class Name
`MdocSessionManager`

### Function Name
`decryptSessionEstablishment`

### Function Introduction
`Reader로부터 수신한 SessionEstablishment 메시지를 복호화하여 DeviceRequest를 추출합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| message | byte[] | SessionEstablishment CBOR 바이트 | M | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| CBORObject | 복호화된 DeviceRequest | M | |

### Function Declaration

```java
CBORObject decryptSessionEstablishment(byte[] message)
```

<br>

### 2.1.2 generateDeviceResponse

### Class Name
`MdocSessionManager`

### Function Name
`generateDeviceResponse`

### Function Introduction
`선택된 claim으로 DeviceResponse를 생성하고 세션 암호화하여 반환합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| mDoc | String | Base64 인코딩된 mDoc 원본 | M | |
| selectedClaimsKeys | List\<String\> | 공개할 claim key 목록 | M | |
| selectedClaimsNamespaces | List\<String\> | 공개할 namespace 목록 | M | |
| holderPrivateKey | PrivateKey | Holder의 개인키 (DeviceAuth용) | M | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| byte[] | 암호화된 SessionData 바이트 | M | |

### Function Declaration

```java
byte[] generateDeviceResponse(String mDoc, List<String> selectedClaimsKeys, 
    List<String> selectedClaimsNamespaces, PrivateKey holderPrivateKey)
```

<br>

## 2.2 MdocProximityServer

### Interface Name
`MdocProximityServer`

### Description
`Holder 측 Proximity 서버 인터페이스입니다. BLE/NFC 구현체가 이를 구현합니다.`

### Declaration

```java
public interface MdocProximityServer {
    void start();
    void stop();
    void sendResponse(byte[] response);
    void setListener(MdocProximityListener listener);
}
```

### Listener Interface

```java
public interface MdocProximityListener {
    void onDeviceConnected();
    void onDeviceDisconnected();
    void onRequestReceived(byte[] request);
}
```

<br>

<div style="page-break-after: always;"></div>

# 3. OID4VP APIs

## 3.1 OID4VPHandler

### 3.1.1 createVPToken

### Class Name
`OID4VPHandler`

### Function Name
`createVPToken`

### Function Introduction
`mDoc에서 VP Token을 생성합니다. 선택적 공개와 DeviceAuth를 지원합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| issuerSignedBase64 | String | Base64 인코딩된 IssuerSigned | M | |
| docType | String | 문서 타입 | O | null이면 자동 감지 |
| requestedClaims | Map\<String, Set\<String\>\> | 네임스페이스별 요청 claim | O | null이면 전체 공개 |
| holderPrivateKey | PrivateKey | Holder 개인키 | O | DeviceAuth용 |
| clientId | String | Verifier Client ID | O | DeviceAuth용 |
| nonce | String | Nonce 값 | O | DeviceAuth용 |
| responseUri | String | Response URI | O | DeviceAuth용 |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| String | Base64 인코딩된 VP Token | M | |

### Function Declaration

```java
// 기본 (전체 claim, docType 자동 감지)
static String createVPToken(String issuerSignedBase64)

// 선택적 공개 (docType 자동 감지)
static String createVPToken(String issuerSignedBase64, Map<String, Set<String>> requestedClaims)

// DeviceAuth 포함 (docType 자동 감지)
static String createVPToken(String issuerSignedBase64, PrivateKey holderPrivateKey,
    String clientId, String nonce, String responseUri)

// 선택적 공개 + DeviceAuth (docType 자동 감지)
static String createVPToken(String issuerSignedBase64, Map<String, Set<String>> requestedClaims,
    PrivateKey holderPrivateKey, String clientId, String nonce, String responseUri)

// 기본 (docType 명시)
static String createVPToken(String issuerSignedBase64, String docType)

// 선택적 공개 (docType 명시)
static String createVPToken(String issuerSignedBase64, String docType,
    Map<String, Set<String>> requestedClaims)

// DeviceAuth 포함 (docType 명시)
static String createVPToken(String issuerSignedBase64, String docType,
    PrivateKey holderPrivateKey, String clientId, String nonce, String responseUri)

// 선택적 공개 + DeviceAuth (docType 명시, 전체 파라미터)
static String createVPToken(String issuerSignedBase64, String docType,
    Map<String, Set<String>> requestedClaims, PrivateKey holderPrivateKey,
    String clientId, String nonce, String responseUri)
```

### Function Usage
```java
// 선택적 공개 + DeviceAuth
Map<String, Set<String>> claims = Map.of(
    "org.iso.18013.5.1", Set.of("family_name", "given_name", "birth_date"));

String vpToken = OID4VPHandler.createVPToken(
    issuerSignedBase64, "org.iso.18013.5.1.mDL",
    claims, holderPrivateKey,
    "did:web:verifier.example.com", nonce, responseUri);
```

<br>

## 3.2 MDocVerifier

### 3.2.1 verify

### Class Name
`MDocVerifier`

### Function Name
`verify`

### Function Introduction
`VP Token을 검증하고 claim을 추출합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| mdocBase64 | String | Base64 인코딩된 VP Token | M | |
| clientId | String | Verifier Client ID | O | DeviceAuth 검증용 |
| nonce | String | Nonce 값 | O | DeviceAuth 검증용 |
| responseUri | String | Response URI | O | DeviceAuth 검증용 |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| MDocClaimsSet | 검증된 claim 집합 | M | |

### Function Declaration

```java
MDocClaimsSet verify(String mdocBase64)

MDocClaimsSet verify(String mdocBase64, String clientId, String nonce, String responseUri)
```

<br>

### 3.2.2 verifyWithX5c

### Class Name
`MDocVerifier`

### Function Name
`verifyWithX5c`

### Function Introduction
`IssuerAuth의 x5chain에서 공개키를 추출하여 서명을 검증합니다. 신뢰 루트 인증서를 지정하면 인증서 체인도 검증합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| mdocBase64 | String | Base64 인코딩된 VP Token | M | |
| trustedRootCerts | List\<X509Certificate\> | 신뢰 루트 인증서 | O | null이면 체인 검증 건너뜀 |
| clientId | String | Verifier Client ID | O | DeviceAuth 검증용 |
| nonce | String | Nonce 값 | O | DeviceAuth 검증용 |
| responseUri | String | Response URI | O | DeviceAuth 검증용 |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| MDocClaimsSet | 검증된 claim 집합 | M | |

### Function Declaration

```java
// x5chain 서명 검증만
static MDocClaimsSet verifyWithX5c(String mdocBase64)

// x5chain 서명 + 인증서 체인 검증
static MDocClaimsSet verifyWithX5c(String mdocBase64, List<X509Certificate> trustedRootCerts)

// x5chain 서명 + DeviceAuth 검증 (체인 검증 없음)
static MDocClaimsSet verifyWithX5c(String mdocBase64,
    String clientId, String nonce, String responseUri)

// x5chain 서명 + 인증서 체인 + DeviceAuth 검증 (전체)
static MDocClaimsSet verifyWithX5c(String mdocBase64, List<X509Certificate> trustedRootCerts,
    String clientId, String nonce, String responseUri)
```

<br>

<div style="page-break-after: always;"></div>

# 4. Data Classes

## 4.1 TransportConfig

### Declaration

```java
public sealed interface TransportConfig {
    record Ble(boolean usePeripheralServerMode, UUID peripheralServerModeUuid,
               UUID centralClientModeUuid) implements TransportConfig {}

    record WifiAware(String passphrase, Integer channelInfo,
                     Integer bandInfo) implements TransportConfig {}

    record Nfc(IsoDep isoDep, int maxCommandDataLength,
               int maxResponseDataLength) implements TransportConfig {}
}
```

### Property

| 타입 | Parameter | Type | Description | **M/O** |
|------|-----------|------|-------------|---------|
| Ble | usePeripheralServerMode | boolean | Peripheral 모드 사용 여부 | M |
| Ble | peripheralServerModeUuid | UUID | Peripheral 서비스 UUID | O |
| Ble | centralClientModeUuid | UUID | Central 클라이언트 UUID | O |
| WifiAware | passphrase | String | NAN Data Path 보안 passphrase | M |
| WifiAware | channelInfo | Integer | 채널 정보 | O |
| WifiAware | bandInfo | Integer | 대역 정보 (2.4/5GHz) | O |
| Nfc | isoDep | IsoDep | NFC IsoDep 인스턴스 | M |
| Nfc | maxCommandDataLength | int | 최대 커맨드 데이터 길이 | M |
| Nfc | maxResponseDataLength | int | 최대 응답 데이터 길이 | M |

<br>

## 4.2 EngagementSource

### Declaration

```java
public sealed interface EngagementSource {
    record QrCode(String qrCode) implements EngagementSource {}
    record Nfc(byte[] data) implements EngagementSource {}
    record NfcDataTransfer(byte[] data, IsoDep isoDep,
        int maxCommandDataLength, int maxResponseDataLength) implements EngagementSource {}
}
```

<br>

## 4.3 RequestedDocument

### Declaration

```java
public class RequestedDocument implements Parcelable {
    public RequestedDocument(String id, AttestationType documentType,
        DocumentMode mode, List<String> claims)
}
```

### Property

| Name | Type | Description | **M/O** |
|------|------|-------------|---------|
| id | String | 문서 식별자 | M |
| documentType | AttestationType | 문서 타입 (MDL, PID) | M |
| mode | DocumentMode | 서명 모드 | M |
| claims | List\<String\> | 요청 claim 목록 | M |

<br>

## 4.4 TransferStatus

### Declaration

```java
public class TransferStatus {
    public enum Type {
        CONNECTING, CONNECTED, DEVICE_ENGAGEMENT_COMPLETED,
        REQUEST_SENT, RESPONSE_RECEIVED, ERROR, DISCONNECTED
    }
}
```

### Property

| Name | Type | Description | **M/O** |
|------|------|-------------|---------|
| type | Type | 상태 타입 | M |
| errorMessage | String | 에러 메시지 (ERROR 타입) | O |
| receivedDocuments | List\<ReceivedDocument\> | 수신 문서 (RESPONSE_RECEIVED 타입) | O |

<br>

## 4.5 ReceivedDocument

### Declaration

```java
public class ReceivedDocument {
    public ReceivedDocument(boolean trusted, String docType,
        Map<String, Object> claims, DocumentValidity validity)
}
```

### Property

| Name | Type | Description | **M/O** |
|------|------|-------------|---------|
| trusted | boolean | 신뢰 여부 | M |
| docType | String | 문서 타입 | M |
| claims | Map\<String, Object\> | claim 맵 | M |
| validity | DocumentValidity | 유효성 정보 | M |

<br>

## 4.6 DocumentValidity

### Declaration

```java
public class DocumentValidity {
    public DocumentValidity(Boolean deviceSignatureValid, Boolean issuerSignatureValid,
        Boolean dataIntegrityIntact, String signed, String validFrom, String validUntil)
}
```

### Property

| Name | Type | Description | **M/O** |
|------|------|-------------|---------|
| deviceSignatureValid | Boolean | Device 서명 유효 여부 | O |
| issuerSignatureValid | Boolean | Issuer 서명 유효 여부 | O |
| dataIntegrityIntact | Boolean | 데이터 무결성 여부 | O |
| signed | String | 서명 시점 | O |
| validFrom | String | 유효 시작 | O |
| validUntil | String | 유효 만료 | O |

<br>

# 5. Enums

## 5.1 AttestationType

| Value | docType | namespace |
|-------|---------|-----------|
| MDL | org.iso.18013.5.1.mDL | org.iso.18013.5.1 |
| PID | eu.europa.ec.eudi.pid.1 | eu.europa.ec.eudi.pid.1 |

## 5.2 DocumentMode

| Value | Description |
|-------|-------------|
| FULL | 전체 claim 포함 |
| CUSTOM | 선택적 claim 지정 |

<br>
