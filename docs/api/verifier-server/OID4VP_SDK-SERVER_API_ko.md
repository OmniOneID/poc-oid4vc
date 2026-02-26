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

OID4VP SDK Server API
==

- 주제: OID4VP SDK Server API
- 작성: 이수현
- 일자: 2026-01-30
- 버전: v1.0.0

| 버전   | 일자       | 변경 내용                 |
| ------ | ---------- | -------------------------|
| v1.0.0 | 2026-01-30 | 초기 작성                 |


<div style="page-break-after: always;"></div>

# 목차
- [1. APIs](#1-apis)
    - [1.1 InitiationService](#11-initiationservice)
        - [1.1.1 initiateVerification](#111-initiateverification)
    - [1.2 AuthorizationService](#12-authorizationservice)
        - [1.2.1 getAuthorizationRequest (CompactSigner)](#121-getauthorizationrequest-compactsigner)
        - [1.2.2 getAuthorizationRequest (PrivateKey)](#122-getauthorizationrequest-privatekey)
        - [1.2.3 receiveResponse](#123-receiveresponse)
    - [1.3 VPTokenVerifier](#13-vptokenverifier)
        - [1.3.1 verifyVerifiablePresentation](#131-verifyverifiablepresentation)
        - [1.3.2 validateSignature](#132-validatesignature)
        - [1.3.3 validatePresentationBinding](#133-validatepresentationbinding)
        - [1.3.4 extractIssuerIdentifier](#134-extractissueridentifier)
        - [1.3.5 extractHolderIdentifier](#135-extractholderidentifier)
    - [1.4 DCQLCredentialMatcher](#14-dcqlcredentialmatcher)
        - [1.4.1 parseCredential](#141-parsecredential)
        - [1.4.2 matchesFormat](#142-matchesformat)
        - [1.4.3 matchesMetadata](#143-matchesmetadata)
        - [1.4.4 extractMatchingClaimNames](#144-extractmatchingclaimnames)
- [2. Interfaces](#2-interfaces)
    - [2.1 CompactSigner](#21-compactsigner)
    - [2.2 SessionRepository](#22-sessionrepository)
    - [2.3 CredentialAdapter](#23-credentialadapter)
- [3. Data Classes](#3-data-classes)
    - [3.1 ServiceResult](#31-serviceresult)
    - [3.2 VerificationSession](#32-verificationsession)
    - [3.3 VerificationConfig](#33-verificationconfig)
    - [3.4 DCQLQuery](#34-dcqlquery)
    - [3.5 ParsedCredential](#35-parsedcredential)
    - [3.6 OID4VPConfig](#36-oid4vpconfig)

<div style="page-break-after: always;"></div>

## 개요

본 문서는 **OpenID for Verifiable Presentations (OID4VP)** 프로토콜을 구현하기 위한 Server SDK API를 정의합니
Verifier 서버 개발자가 VP(Verifiable Presentation) 검증 기능을 쉽게 구현할 수 있도록 지원합니다.

### 주요 기능
- Authorization Request 생성 및 서명 (RFC9101 JAR 지원)
- DCQL(Digital Credentials Query Language) 기반 Credential 요청
- VP Token 수신 및 검증 (SD-JWT, mDoc 등 다중 포맷 지원)
- 세션 관리 및 상태 추적

<br>

<div style="page-break-after: always;"></div>

# 1. APIs

## 1.1 InitiationService

### 1.1.1 initiateVerification

### Class Name
`InitiationService`

### Function Name
`initiateVerification`

### Function Introduction
`OID4VP 검증 요청을 초기화합니다. DCQL 쿼리 또는 scope 파라미터를 처리하여 Authorization Request URI를 생성합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| dcqlQuery | String | DCQL 쿼리 JSON 문자열 | O | scope와 상호 배타적 |
| scope | String | Scope 파라미터 | O | dcqlQuery와 상호 배타적 |
| responseMode | String | 응답 모드 (direct_post, query 등) | M | |
| clientMetadata | String | 클라이언트 메타데이터 JSON | O | |
| useRequestUri | boolean | request_uri 사용 여부 (by_reference) | M | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| ServiceResult\<Map\<String, Object\>\> | 초기화 결과 | M | [Link](#31-serviceresult) |

### Function Declaration

```java
ServiceResult<Map<String, Object>> initiateVerification(
    String dcqlQuery,
    String scope,
    String responseMode,
    String clientMetadata,
    boolean useRequestUri) throws OID4VPException
```

### Function Usage
```java
@Autowired
private InitiationService initiationService;

// DCQL 쿼리로 초기화
String dcqlQuery = "{\"credentials\":[{\"id\":\"my_credential\",\"format\":\"dc+sd-jwt\"}]}";
ServiceResult<Map<String, Object>> result = initiationService.initiateVerification(
    dcqlQuery,
    null,
    "direct_post",
    null,
    true
);

if (result.isSuccess()) {
    Map<String, Object> data = result.getData();
    String authRequestUri = (String) data.get("authorization_request_uri");
    String transactionId = (String) data.get("transaction_id");
}
```

<br>

## 1.2 AuthorizationService

### 1.2.1 getAuthorizationRequest (CompactSigner)

### Class Name
`AuthorizationService`

### Function Name
`getAuthorizationRequest`

### Function Introduction
`CompactSigner를 사용하여 RFC9101 규격의 JWS 형식 Authorization Request를 생성합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| requestId | String | 요청 식별자 | M | |
| compactSigner | CompactSigner | 서명 인터페이스 | M | [Link](#21-compactsigner) |
| publicKeyMultibase | String | Multibase 인코딩된 공개키 | M | JWK 헤더용 |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| ServiceResult\<String\> | JWS 문자열 | M | Content-Type: application/oauth-authz-req+jwt |

### Function Declaration

```java
ServiceResult<String> getAuthorizationRequest(
    String requestId, 
    CompactSigner compactSigner, 
    String publicKeyMultibase)
```

### Function Usage
```java
@Autowired
private AuthorizationService authorizationService;

CompactSigner signer = (keyId, hash) -> {
    // 서명 로직 구현
    return signatureBytes;
};

ServiceResult<String> result = authorizationService.getAuthorizationRequest(
    requestId, 
    signer, 
    "zPublicKeyMultibase..."
);

if (result.isSuccess()) {
    String jws = result.getData();
    // JWS 반환
}
```

<br>

### 1.2.2 getAuthorizationRequest (PrivateKey)

### Class Name
`AuthorizationService`

### Function Name
`getAuthorizationRequest`

### Function Introduction
`PrivateKey를 직접 사용하여 RFC9101 규격의 JWS 형식 Authorization Request를 생성합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| requestId | String | 요청 식별자 | M | |
| privateKey | PrivateKey | 서명용 개인키 | M | java.security.PrivateKey |
| publicKeyMultibase | String | Multibase 인코딩된 공개키 | M | JWK 헤더용 |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| ServiceResult\<String\> | JWS 문자열 | M | |

### Function Declaration

```java
ServiceResult<String> getAuthorizationRequest(
    String requestId, 
    PrivateKey privateKey, 
    String publicKeyMultibase)
```

<br>

### 1.2.3 receiveResponse

### Class Name
`AuthorizationService`

### Function Name
`receiveResponse`

### Function Introduction
`Wallet으로부터 VP Token 응답을 수신하여 처리합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| vpTokenMap | Map\<String, List\<Object\>\> | VP Token 맵 (credential type → credentials) | O | |
| issuerPublicKeys | List\<String\> | Base64 인코딩된 발급자 공개키 목록 | M | |
| holderPublicKeys | List\<String\> | Base64 인코딩된 보유자 공개키 목록 | O | null 가능 |
| state | String | State 파라미터 | M | |
| error | String | 오류 코드 | O | |
| errorDescription | String | 오류 설명 | O | |
| httpMethod | String | HTTP 메서드 | M | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| ServiceResult\<Map\<String, Object\>\> | 검증 결과 | M | |

### Function Declaration

```java
ServiceResult<Map<String, Object>> receiveResponse(
    Map<String, List<Object>> vpTokenMap,
    List<String> issuerPublicKeys,
    List<String> holderPublicKeys,
    String state,
    String error,
    String errorDescription,
    String httpMethod)
```

<br>

## 1.3 VPTokenVerifier

### 1.3.1 verifyVerifiablePresentation

### Interface Name
`VPTokenVerifier`

### Function Name
`verifyVerifiablePresentation`

### Function Introduction
`VP Token의 전체 검증을 수행합니다. 서명 검증과 Presentation Binding 검증을 모두 포함합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| credential | String | 검증할 Credential 문자열 | M | |
| verificationConfig | VerificationConfig | 검증 설정 | M | [Link](#33-verificationconfig) |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| boolean | 검증 성공 여부 | M | |

### Function Declaration

```java
boolean verifyVerifiablePresentation(String credential, VerificationConfig verificationConfig) 
    throws FormatterException
```

### Function Usage
```java
VPTokenVerifier verifier = new SDJWTVerifier(); // 구현체

VerificationConfig config = VerificationConfig.builder()
    .issuerPublicKey("Base64EncodedIssuerPublicKey")
    .holderPublicKey("Base64EncodedHolderPublicKey")
    .clientId("did:web:verifier.example.com")
    .nonce("randomNonce123")
    .build();

boolean isValid = verifier.verifyVerifiablePresentation(sdJwtCredential, config);
```

<br>

### 1.3.2 validateSignature

### Interface Name
`VPTokenVerifier`

### Function Name
`validateSignature`

### Function Introduction
`Credential의 암호학적 서명만 검증합니다. Presentation Binding은 검증하지 않습니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| credential | String | Credential 문자열 | M | |
| issuerPublicKey | String | Base64 인코딩된 발급자 압축 공개키 | M | |
| holderPublicKey | String | Base64 인코딩된 보유자 압축 공개키 | O | null 가능 |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| boolean | 서명 유효 여부 | M | |

### Function Declaration

```java
boolean validateSignature(String credential, String issuerPublicKey, String holderPublicKey) 
    throws FormatterException
```

<br>

### 1.3.3 validatePresentationBinding

### Interface Name
`VPTokenVerifier`

### Function Name
`validatePresentationBinding`

### Function Introduction
`Credential의 Presentation Binding(aud, nonce)이 예상값과 일치하는지 검증합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| credential | String | Credential 문자열 | M | |
| expectedClientId | String | 예상 Client ID (aud) | M | |
| expectedNonce | String | 예상 Nonce | M | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| boolean | 바인딩 일치 여부 | M | |

### Function Declaration

```java
boolean validatePresentationBinding(String credential, String expectedClientId, String expectedNonce)
```

<br>

### 1.3.4 extractIssuerIdentifier

### Interface Name
`VPTokenVerifier`

### Function Name
`extractIssuerIdentifier`

### Function Introduction
`Credential에서 발급자 식별자(kid 또는 x5c)를 추출합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| credential | String | Credential 문자열 | M | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| IdentifierResult | 식별자 타입과 값 | O | null 가능 |

### Function Declaration

```java
IdentifierResult extractIssuerIdentifier(String credential) throws FormatterException
```

<br>

### 1.3.5 extractHolderIdentifier

### Interface Name
`VPTokenVerifier`

### Function Name
`extractHolderIdentifier`

### Function Introduction
`Credential에서 보유자 식별자(cnf.kid 또는 cnf.x5c)를 추출합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| credential | String | Credential 문자열 | M | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| IdentifierResult | 식별자 타입과 값 | O | Key Binding 없으면 null |

### Function Declaration

```java
IdentifierResult extractHolderIdentifier(String credential) throws FormatterException
```

<br>

## 1.4 DCQLCredentialMatcher

### 1.4.1 parseCredential

### Class Name
`DCQLCredentialMatcher`

### Function Name
`parseCredential`

### Function Introduction
`Raw Credential 문자열을 ParsedCredential 객체로 파싱합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| rawCredential | String | Raw Credential 문자열 | M | |
| format | String | Credential 포맷 | O | null이면 자동 감지 |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| ParsedCredential | 파싱된 Credential | M | [Link](#35-parsedcredential) |

### Function Declaration

```java
static ParsedCredential parseCredential(String rawCredential, String format) throws DCQLException
```

### Function Usage
```java
String sdJwtCredential = "eyJ...~eyJ...~...";
ParsedCredential parsed = DCQLCredentialMatcher.parseCredential(sdJwtCredential, "dc+sd-jwt");

Map<String, Object> claims = parsed.getAllClaims();
String vct = (String) parsed.getMetadataValue("vct");
```

<br>

### 1.4.2 matchesFormat

### Class Name
`DCQLCredentialMatcher`

### Function Name
`matchesFormat`

### Function Introduction
`요청된 포맷이 지원되는지 확인합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| requiredFormat | String | 요청 포맷 | O | null이면 true 반환 |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| boolean | 지원 여부 | M | |

### Function Declaration

```java
static boolean matchesFormat(String requiredFormat)
```

<br>

### 1.4.3 matchesMetadata

### Class Name
`DCQLCredentialMatcher`

### Function Name
`matchesMetadata`

### Function Introduction
`Credential이 메타데이터 요구사항(vct_values, doctype 등)과 일치하는지 확인합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| credential | ParsedCredential | 파싱된 Credential | M | |
| metadata | Map\<String, Object\> | 메타데이터 요구사항 | O | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| boolean | 일치 여부 | M | |

### Function Declaration

```java
static boolean matchesMetadata(ParsedCredential credential, Map<String, Object> metadata)
```

<br>

### 1.4.4 extractMatchingClaimNames

### Class Name
`DCQLCredentialMatcher`

### Function Name
`extractMatchingClaimNames`

### Function Introduction
`DCQL 쿼리를 기반으로 Credential에서 매칭되는 클레임 이름을 추출합니다.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| dcqlQuery | DCQLQuery | DCQL 쿼리 | M | [Link](#34-dcqlquery) |
| credential | ParsedCredential | 파싱된 Credential | M | |

### Output Parameters

| Type | Description | **M/O** | **비고** |
|------|-------------|---------|---------|
| Set\<String\> | 매칭된 클레임 이름 집합 | M | |

### Function Declaration

```java
static Set<String> extractMatchingClaimNames(DCQLQuery dcqlQuery, ParsedCredential credential)
```

<br>

# 2. Interfaces

## 2.1 CompactSigner

### Declaration

```java
@FunctionalInterface
public interface CompactSigner {
    byte[] sign(String keyId, byte[] hash) throws Exception;
}
```

### Description
`서명 작업을 위한 함수형 인터페이스입니다. Authorization Request 서명 시 사용됩니다.`

### Usage
```java
CompactSigner signer = (keyId, hash) -> {
    // keyId에 해당하는 개인키로 hash 서명
    PrivateKey privateKey = keyStore.getPrivateKey(keyId);
    Signature sig = Signature.getInstance("SHA256withECDSA");
    sig.initSign(privateKey);
    sig.update(hash);
    return sig.sign();
};
```

<br>

## 2.2 SessionRepository

### Declaration

```java
public interface SessionRepository {
    Optional<VerificationSession> findByState(String state);
    Optional<VerificationSession> findByRequestId(String requestId);
    Optional<VerificationSession> findByTransactionId(String transactionId);
    Map<String, VerificationSession> findAll();
    void saveByState(String state, VerificationSession session);
    boolean existsByState(String state);
    void clear();
    int count();
}
```

### Description
`검증 세션 저장소 인터페이스입니다. 기본 InMemory 구현체가 제공되며, 커스텀 구현(Redis, DB 등)이 가능합니다.`

<br>

## 2.3 CredentialAdapter

### Declaration

```java
public interface CredentialAdapter {
    Set<String> getSupportedFormats();
    boolean supports(String format);
    ParsedCredential parse(String rawCredential) throws DCQLException;
    boolean matchesMetadata(ParsedCredential credential, Map<String, Object> metadata);
    Map<String, Object> extractAllClaims(ParsedCredential credential);
    Set<String> getReservedClaimNames();
}
```

### Description
`다양한 Credential 포맷(SD-JWT, mDoc 등)을 처리하기 위한 어댑터 인터페이스입니다.`

<br>

# 3. Data Classes

## 3.1 ServiceResult

### Declaration

```java
@Getter
@Builder
public class ServiceResult<T> {
    private boolean success;
    private T data;
    private String errorCode;
    private String errorDescription;
    private String state;
    private int httpStatus;
    private String contentType;
}
```

### Factory Methods

| Method | Description |
|--------|-------------|
| `success(T data)` | 성공 결과 생성 (HTTP 200) |
| `success(T data, String contentType)` | 성공 결과 생성 (커스텀 Content-Type) |
| `badRequest(String errorCode, String errorDescription)` | 400 오류 결과 생성 |
| `notFound(String errorCode, String errorDescription)` | 404 오류 결과 생성 |
| `serverError(String errorCode, String errorDescription)` | 500 오류 결과 생성 |

<br>

## 3.2 VerificationSession

### Declaration

```java
@Data
public class VerificationSession {
    private String transactionId;
    private String state;
    private String nonce;
    private String dcqlQuery;
    private String responseMode;
    private String requestId;
    private String status;
    private String clientMetadata;
    private Long requestUriFetchedAt;
    private Long createdAt;
    private Long expiresAt;
    private Long updatedAt;
    private String vpToken;
}
```

### Property

| Parameter | Type | Description | **M/O** |
|-----------|------|-------------|---------|
| transactionId | String | 트랜잭션 식별자 | M |
| state | String | State 파라미터 | M |
| nonce | String | Nonce 값 | M |
| dcqlQuery | String | DCQL 쿼리 JSON | M |
| responseMode | String | 응답 모드 | M |
| requestId | String | 요청 식별자 | M |
| status | String | 세션 상태 (CREATED, REQUEST_FETCHED, COMPLETED, EXPIRED) | M |
| clientMetadata | String | 머지된 클라이언트 메타데이터 JSON | O |

<br>

## 3.3 VerificationConfig

### Declaration

```java
@Getter
@Builder
public class VerificationConfig {
    private final String issuerPublicKey;
    private final String holderPublicKey;
    private final String clientId;
    private final String nonce;
}
```

### Property

| Parameter | Type | Description | **M/O** | **비고** |
|-----------|------|-------------|---------|---------|
| issuerPublicKey | String | Base64 인코딩된 발급자 압축 공개키 | M | |
| holderPublicKey | String | Base64 인코딩된 보유자 압축 공개키 | O | null이면 cnf.jwk에서 추출 |
| clientId | String | Verifier의 Client ID | M | aud 클레임 검증용 |
| nonce | String | 예상 Nonce 값 | M | |

<br>

## 3.4 DCQLQuery

### Declaration

```java
@Data
@Builder
public class DCQLQuery {
    private List<CredentialQuery> credentials;
    private List<CredentialSet> credentialSets;
    private List<Map<String, Object>> transactionData;

    @Data
    @Builder
    public static class CredentialQuery {
        private String id;
        private String format;
        private Map<String, Object> meta;
        private List<ClaimQuery> claims;
        private List<ClaimSet> claimSets;
        private String purpose;
        private Boolean requireCryptographicHolderBinding;
    }

    @Data
    @Builder
    public static class ClaimQuery {
        private String id;
        private List<Object> path;
        private String purpose;
        private List<Object> values;
        private Object value;
        private Object max;
        private Object min;
    }
}
```

<br>

## 3.5 ParsedCredential

### Declaration

```java
@Getter
@Builder
public class ParsedCredential {
    private final String format;
    private final String rawCredential;
    private final Map<String, Object> baseClaims;
    private final Map<String, Object> allClaims;
    private final Map<String, Object> metadata;
    private final Object nativeCredential;
}
```

### Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getClaim(String claimName)` | Object | 특정 클레임 값 조회 |
| `hasClaim(String claimName)` | boolean | 클레임 존재 여부 확인 |
| `getMetadataValue(String key)` | Object | 메타데이터 값 조회 |
| `getNativeCredentialAs(Class<T> type)` | T | 네이티브 Credential 캐스팅 |

<br>

## 3.6 OID4VPConfig

### Declaration

```java
@Data
public class OID4VPConfig {
    private String baseUrl;
    private String clientName;
    private String invocationScheme;
    private ClientId clientId;
    private Session session;
    private Endpoints endpoints;
    private ClientMetadata clientMetadata;
    private Crypto crypto;

    @Data
    public static class ClientId {
        private String scheme;  // e.g., "did:web"
        private String value;   // e.g., "verifier.example.com"
    }

    @Data
    public static class Session {
        private long sessionTtl;  // default: 300000ms
    }

    @Data
    public static class Crypto {
        private String vpTokenEncryptionKey;
    }
}
```