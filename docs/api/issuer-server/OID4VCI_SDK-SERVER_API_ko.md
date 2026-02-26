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

OID4VCI SDK Server API
==

- 주제: OID4VCI SDK Server API
- 작성: 김상준
- 일자: 2026-02-02
- 버전: v1.0.0

| 버전   | 일자       | 변경 내용                 |
| ------ | ---------- | -------------------------|
| v1.0.0 | 2026-02-02 | 초기 작성                 |


<div style="page-break-after: always;"></div>

# 목차
- [1. APIs](#1-apis)
    - [1.1 CredentialService](#11-credentialservice)
        - [1.1.1 processCredentialOffer](#111-processcredentialoffer)
        - [1.1.2 createTestCredentialOffer](#112-createtestcredentialoffer)
        - [1.1.3 issueCredential](#113-issuecredential)
        - [1.1.4 getDeferredCredential](#114-getdeferredcredential)
        - [1.1.5 getIssuerMetadata](#115-getissuermetadata)
        - [1.1.6 handleNonce](#116-handlenonce)
        - [1.1.7 handleNotification](#117-handlenotification)
    - [1.2 IssuanceGatewayService](#12-issuancegatewayservice)
        - [1.2.1 generateCredentialOfferUri](#121-generatecredentialofferuri)
- [2. Interfaces (SPI)](#2-interfaces-spi)
    - [2.1 UserDataProvider](#21-userdataprovider)
    - [2.2 KeyDataProvider](#22-keydataprovider)
    - [2.3 ProtocolIssuer](#23-protocolissuer)
    - [2.4 Store Interfaces](#24-store-interfaces)
- [3. Data Classes](#3-data-classes)
    - [3.1 CredentialRequest](#31-credentialrequest)
    - [3.2 Proofs](#32-proofs)
    - [3.3 CredentialResponse](#33-credentialresponse)
    - [3.4 IssuerMetadataResponse](#34-issuermetadataresponse)
    - [3.5 PreAuthorizeResponse](#35-preauthorizeresponse)
    - [3.6 DeferredIssuanceResponse](#36-deferredissuanceresponse)
    - [3.7 NotificationRequest](#37-notificationrequest)

<div style="page-break-after: always;"></div>

## 개요

본 문서는 **OpenID for Verifiable Credential Issuance (OID4VCI)** 프로토콜을 구현하기 위한 Server SDK API를 정의합니다.
발급자(Issuer) 서버 개발자가 VC(Verifiable Credential) 발급 기능을 효율적으로 구현할 수 있도록 지원합니다.

### 주요 기능
- Credential Offer 생성 및 관리 (Pre-Authorized & Authorization Code Flow)
- 자격증명 발급 요청 처리 및 Proof 검증
- 지연 발급(Deferred Issuance) 지원
- Issuer Metadata 제공 및 동적 엔드포인트 관리
- 다양한 VC 포맷(SD-JWT, OpenDID VC 등) 발급 지원

<br>

<div style="page-break-after: always;"></div>

# 1. APIs

## 1.1 CredentialService

### 1.1.1 processCredentialOffer

### Class Name
`CredentialService`

### Function Name
`processCredentialOffer`

### Function Introduction
`클라이언트의 Credential Offer 요청을 처리합니다. 요청 ID에 따라 적절한 Grant 타입을 판별하여 Offer 정보를 반환합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| requestId | String | 요청 식별자 ('p' 시작: Pre-Auth, 'a' 시작: Auth Code) | M | |
| request | CredentialOfferRequest | Offer 요청 정보 | M | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| CredentialOfferResponse | 생성된 Credential Offer 정보 | M | |

### Function Declaration

```java
public CredentialOfferResponse processCredentialOffer(String requestId, CredentialOfferRequest request) throws OID4VCIException
```

<br>

### 1.1.2 createTestCredentialOffer

### Class Name
`CredentialService`

### Function Name
`createTestCredentialOffer`

### Function Introduction
`테스트용 Credential Offer를 생성합니다. 인가 서버로부터 Pre-Authorized Code를 즉시 발급받아 테스트 환경을 구성합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| userId | String | 사용자 식별자 | M | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| TestCredentialOfferResponse | 테스트용 Offer 정보 및 PIN 코드 | M | |

<br>

### 1.1.3 issueCredential

### Class Name
`CredentialService`

### Function Name
`issueCredential`

### Function Introduction
`자격증명 발급 요청을 처리합니다. Access Token의 유효성 및 Proof를 검증한 후 실제 VC를 생성하여 반환합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| request | CredentialRequest | 발급 요청 정보 (포맷, Proof 등) | M | |
| accessToken | Jwt | 인가 서버에서 발급한 Access Token | M | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| Object | 발급된 VC 또는 DeferredIssuanceResponse | M | 즉시 발급 또는 지연 발급 결과 |

### Function Declaration

```java
public Object issueCredential(CredentialRequest request, Jwt accessToken) throws OID4VCIException
```

<br>

### 1.1.4 getDeferredCredential

### Class Name
`CredentialService`

### Function Name
`getDeferredCredential`

### Function Introduction
`지연된 자격증명 발급 요청에 대해 실제 VC를 반환합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| transactionId | String | 지연 발급 트랜잭션 ID | M | |
| accessToken | Jwt | 유효한 Access Token | M | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| CredentialResponse | 발급된 VC 정보 | M | |

<br>

### 1.1.5 getIssuerMetadata

### Class Name
`CredentialService`

### Function Name
`getIssuerMetadata`

### Function Introduction
`발급자 메타데이터 정보를 반환합니다.`

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| IssuerMetadataResponse | 발급자 메타데이터 객체 | M | |

<br>

### 1.1.6 handleNonce

### Class Name
`CredentialService`

### Function Name
`handleNonce`

### Function Introduction
`Proof 검증에 사용될 새로운 c_nonce를 생성하여 반환합니다.`

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| NonceResponse | 생성된 Nonce 정보 | M | |

<br>

### 1.1.7 handleNotification

### Class Name
`CredentialService`

### Function Name
`handleNotification`

### Function Introduction
`월렛으로부터의 알림(예: VC 저장 성공 등)을 처리합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| request | NotificationRequest | 알림 상세 정보 | M | |

### Function Declaration

```java
public void handleNotification(NotificationRequest request)
```

<br>

## 1.2 IssuanceGatewayService

### 1.2.1 generateCredentialOfferUri

### Class Name
`IssuanceGatewayService`

### Function Name
`generateCredentialOfferUri`

### Function Introduction
`사용자 식별자와 Grant 타입을 기반으로 Credential Offer URI와 QR 코드 데이터를 생성합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| userId | String | 사용자 식별자 | M | |
| grantType | String | 권한 부여 방식 (pre-authorized_code 등) | M | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| Map\<String, Object\> | 생성된 QR 이미지 및 URL 데이터 | M | |

### Function Declaration

```java
public Map<String, Object> generateCredentialOfferUri(String userId, String grantType)
    throws IOException, WriterException, NoSuchAlgorithmException, OID4VCIException
```

<br>

# 2. Interfaces (SPI)

## 2.1 UserDataProvider
`자격증명에 포함될 사용자의 클레임 데이터를 제공하는 인터페이스입니다.`

```java
public interface UserDataProvider {
    Map<String, Object> getUserClaims(String userId, String credentialType);
}
```

## 2.2 KeyDataProvider
`VC 서명에 필요한 키 정보(개인키, 공개키 등)를 제공하는 인터페이스입니다.`

```java
public interface KeyDataProvider {
    Map<String, Object> getKeyInfo(String userId, String credentialType);
}
```

## 2.3 ProtocolIssuer
`특정 프로토콜 흐름에 따라 VC를 실제로 생성하는 역할을 하는 인터페이스입니다.`

```java
public interface ProtocolIssuer {
    boolean supports(String format);
    Object issueCredential(Map<String, Object> claims, Map<String, Object> keyInfo) throws OID4VCIException;
}
```

## 2.4 Store Interfaces
- `CredentialOfferStore`: 생성된 Offer 정보를 일시적으로 저장합니다.
- `SessionStore`: 발급 세션 및 지연 발급 정보를 관리합니다.
- `CNonceStore`: 유효한 c_nonce 목록을 관리합니다.

<br>

# 3. Data Classes

## 3.1 CredentialRequest
`클라이언트로부터 수신된 자격증명 발급 요청 데이터입니다.`

| Property | Type | Description |
|----------|------|-------------|
| credential_configuration_id | String | 요청하는 자격증명 설정 ID |
| credential_identifier | String | 자격증명 식별자 |
| proofs | Proofs | 소유 증명 데이터 |

## 3.2 Proofs
`다양한 유형의 소유 증명 데이터를 담는 컨테이너입니다.`

| Property | Type | Description |
|----------|------|-------------|
| jwt | List\<String\> | JWT 기반 소유 증명 목록 |
| di_vp | List\<String\> | DI-VP 기반 소유 증명 목록 |
| attestation | List\<String\> | Attestation 기반 소유 증명 목록 |

## 3.3 CredentialResponse
`자격증명 발급 성공 시 클라이언트에 반환되는 응답 데이터입니다.`

| Property | Type | Description |
|----------|------|-------------|
| credentials | List\<Credential\> | 발급된 자격증명 목록 |
| c_nonce | String | 다음 요청 시 사용할 Nonce (Optional) |
| c_nonce_expires_in | Integer | Nonce 만료 시간 (Optional) |

## 3.4 IssuerMetadataResponse
`발급자의 정보를 정의하는 메타데이터 응답 데이터입니다.`

| Property | Type | Description |
|----------|------|-------------|
| credential_issuer | String | 발급자 식별자 URL |
| credential_endpoint | String | 자격증명 발급 엔드포인트 |
| credential_configurations_supported | Map | 지원하는 자격증명 설정 목록 |

## 3.5 PreAuthorizeResponse
`인가 서버로부터 수신된 Pre-Authorized Code 정보입니다.`

| Property | Type | Description |
|----------|------|-------------|
| pre-authorized_code | String | 사전 인가 코드 |
| user_pin_required | boolean | PIN 코드 필요 여부 |
| expires_in | int | 만료 시간 |

## 3.6 DeferredIssuanceResponse
`자격증명 발급이 지연될 때 반환되는 응답 데이터입니다.`

| Property | Type | Description |
|----------|------|-------------|
| transaction_id | String | 지연 발급 트랜잭션 식별자 |
| interval | long | 폴링 간격 (초 단위) |

## 3.7 NotificationRequest
`알림 이벤트에 대한 요청 데이터입니다.`

| Property | Type | Description |
|----------|------|-------------|
| notification_id | String | 알림 식별자 |
| event | String | 이벤트 유형 |
| event_description | String | 이벤트 상세 설명 |
