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

SD-JWT VC SDK AOS API
==

- 주제: SD-JWT VC SDK Android API
- 작성: 오픈소스개발팀
- 일자: 2026-03-31
- 버전: v1.0.0

| 버전   | 일자       | 변경 내용                 |
| ------ | ---------- | -------------------------|
| v1.0.0 | 2026-03-31 | 초기 작성                 |

<div style="page-break-after: always;"></div>

# 목차
- [1. APIs](#1-apis)
    - [1.1 SDJWTBuilder](#11-sdjwtbuilder)
        - [1.1.1 claim](#111-claim)
        - [1.1.2 selectivelyDisclosableClaim](#112-selectivelydisclosableclaim)
        - [1.1.3 verifiableCredentialType](#113-verifiablecredentialtype)
        - [1.1.4 confirmation](#114-confirmation)
        - [1.1.5 build](#115-build)
    - [1.2 SDJWTVerifier](#12-sdjwtverifier)
        - [1.2.1 verify](#121-verify)
        - [1.2.2 verifyWithX5c](#122-verifywithx5c)
    - [1.3 KeyBindingJWTBuilder](#13-keybindingjwtbuilder)
        - [1.3.1 createKeyBindingJWT](#131-createkeybindingjwt)
    - [1.4 OID4VPHandler](#14-oid4vphandler)
        - [1.4.1 createVPToken](#141-createvptoken)
        - [1.4.2 createVPTokenWithDcqlId](#142-createvptokenwithdcqlid)
    - [1.5 SDJWT](#15-sdjwt)
        - [1.5.1 parse](#151-parse)
- [2. Data Classes](#2-data-classes)
    - [2.1 SDJWT](#21-sdjwt)
    - [2.2 Disclosure](#22-disclosure)
    - [2.3 DisclosureFrame](#23-disclosureframe)
    - [2.4 SDJWTClaimsSet](#24-sdjwtclaimsset)
    - [2.5 SignedJWT](#25-signedjwt)
- [3. Interfaces](#3-interfaces)
    - [3.1 JWSSigner](#31-jwssigner)
    - [3.2 JWSVerifier](#32-jwsverifier)
- [4. Enums](#4-enums)
    - [4.1 SDJWTErrorCode](#41-sdjwterrorcode)

<div style="page-break-after: always;"></div>

## 개요

본 문서는 **SD-JWT VC (Selective Disclosure for JWTs - Verifiable Credentials)**를 Android 환경에서 사용하기 위한 SDK API를 정의합니다.
SD-JWT VC의 생성, 검증, 선택적 공개(Selective Disclosure), OID4VP 프레젠테이션 기능을 제공합니다.

### 주요 기능
- SD-JWT VC 생성 (Fluent Builder 패턴)
- SD-JWT VC 검증 (서명 검증, Key Binding 검증)
- Selective Disclosure 처리
- OID4VP VP Token 생성

<br>

<div style="page-break-after: always;"></div>

# 1. APIs

## 1.1 SDJWTBuilder

### 1.1.1 claim

### Class Name
`SDJWTBuilder`

### Function Name
`claim`

### Function Introduction
`SD-JWT 페이로드에 일반(비공개 대상이 아닌) 클레임을 추가합니다.`

### Input Parameters

| Parameter | Type   | Description    | **M/O** | **비고** |
|-----------|--------|----------------|---------|---------|
| name      | String | 클레임 이름      | M       |         |
| value     | Object | 클레임 값        | M       |         |

### Output Parameters

| Type          | Description          | **M/O** | **비고**       |
|---------------|----------------------|---------|---------------|
| SDJWTBuilder  | Builder 인스턴스 (체이닝) | M       | Fluent API    |

### Function Declaration

```java
SDJWTBuilder claim(String name, Object value)
```

### Function Usage
```java
SDJWTBuilder builder = new SDJWTBuilder()
    .claim("iss", "https://issuer.example.com")
    .claim("iat", Instant.now().getEpochSecond());
```

<br>

### 1.1.2 selectivelyDisclosableClaim

### Class Name
`SDJWTBuilder`

### Function Name
`selectivelyDisclosableClaim`

### Function Introduction
`선택적 공개 대상 클레임을 추가합니다. 해당 클레임은 Disclosure로 분리됩니다.`

### Input Parameters

| Parameter | Type   | Description    | **M/O** | **비고**            |
|-----------|--------|----------------|---------|-------------------|
| name      | String | 클레임 이름      | M       |                   |
| value     | Object | 클레임 값        | M       |                   |
| salt      | String | Salt 값         | O       | 미지정 시 자동 생성  |

### Output Parameters

| Type          | Description          | **M/O** | **비고**       |
|---------------|----------------------|---------|---------------|
| SDJWTBuilder  | Builder 인스턴스 (체이닝) | M       | Fluent API    |

### Function Declaration

```java
SDJWTBuilder selectivelyDisclosableClaim(String name, Object value)
SDJWTBuilder selectivelyDisclosableClaim(String salt, String name, Object value)
```

<br>

### 1.1.3 verifiableCredentialType

### Class Name
`SDJWTBuilder`

### Function Name
`verifiableCredentialType`

### Function Introduction
`SD-JWT VC의 vct(Verifiable Credential Type) 클레임을 설정합니다.`

### Input Parameters

| Parameter | Type   | Description               | **M/O** | **비고** |
|-----------|--------|---------------------------|---------|---------|
| vct       | String | Verifiable Credential Type | M       |         |

### Function Declaration

```java
SDJWTBuilder verifiableCredentialType(String vct)
```

<br>

### 1.1.4 confirmation

### Class Name
`SDJWTBuilder`

### Function Name
`confirmation`

### Function Introduction
`Holder의 Key Binding을 위한 cnf(confirmation) 클레임을 설정합니다.`

### Input Parameters

| Parameter | Type                  | Description         | **M/O** | **비고**        |
|-----------|-----------------------|---------------------|---------|---------------|
| cnf       | Map\<String, Object\> | cnf 클레임 (JWK 포함)  | M       | jwk 키 포함     |

### Function Declaration

```java
SDJWTBuilder confirmation(Map<String, Object> cnf)
```

<br>

### 1.1.5 build

### Class Name
`SDJWTBuilder`

### Function Name
`build`

### Function Introduction
`설정된 클레임과 Disclosure를 기반으로 SD-JWT를 생성합니다.`

### Input Parameters

| Parameter  | Type                        | Description    | **M/O** | **비고**             |
|------------|-----------------------------|----------------|---------|---------------------|
| jwtSigner  | Function\<String, String\>  | JWT 서명 함수    | M       | signingInput → JWT  |

### Output Parameters

| Type  | Description       | **M/O** | **비고**              |
|-------|-------------------|---------|-----------------------|
| SDJWT | SD-JWT 객체        | M       | [Link](#21-sdjwt)    |

### Function Declaration

```java
SDJWT build(Function<String, String> jwtSigner)
```

### Function Usage
```java
SDJWT sdjwt = new SDJWTBuilder()
    .issuer("https://issuer.example.com")
    .verifiableCredentialType("IdentityCredential")
    .issuedAtNow()
    .expiresIn(365, ChronoUnit.DAYS)
    .confirmation(cnfMap)
    .selectivelyDisclosableClaim("name", "홍길동")
    .selectivelyDisclosableClaim("birthdate", "1990-01-01")
    .claim("iss", "https://issuer.example.com")
    .build(signingInput -> issuerSigner.sign(signingInput));

String sdJwtString = sdjwt.toString();
```

<br>

## 1.2 SDJWTVerifier

### 1.2.1 verify

### Class Name
`SDJWTVerifier`

### Function Name
`verify`

### Function Introduction
`SD-JWT의 서명을 검증하고 Disclosure를 처리하여 클레임을 반환합니다. Key Binding 검증도 선택적으로 수행합니다.`

### Input Parameters

| Parameter         | Type   | Description         | **M/O** | **비고**                 |
|-------------------|--------|---------------------|---------|-------------------------|
| sdJwtString       | String | SD-JWT 문자열         | M       |                         |
| expectedAudience  | String | 예상 Audience (aud)   | O       | Key Binding 검증 시 필요  |
| expectedNonce     | String | 예상 Nonce            | O       | Key Binding 검증 시 필요  |

### Output Parameters

| Type            | Description        | **M/O** | **비고**                    |
|-----------------|--------------------|---------|-----------------------------|
| SDJWTClaimsSet  | 검증된 클레임 셋      | M       | [Link](#24-sdjwtclaimsset)  |

### Function Declaration

```java
SDJWTClaimsSet verify(String sdJwtString)
SDJWTClaimsSet verify(String sdJwtString, String expectedAudience, String expectedNonce)
```

### Function Usage
```java
SDJWTVerifier verifier = new SDJWTVerifier(issuerPublicKey, holderPublicKey)
    .requireKeyBinding(true)
    .clockSkew(Duration.ofSeconds(30));

SDJWTClaimsSet claimsSet = verifier.verify(sdJwtString, "did:web:verifier.example.com", "nonce123");

String name = claimsSet.getStringClaim("name");
```

<br>

### 1.2.2 verifyWithX5c

### Class Name
`SDJWTVerifier`

### Function Name
`verifyWithX5c`

### Function Introduction
`x5c 헤더에 포함된 인증서 체인을 사용하여 SD-JWT를 검증합니다.`

### Input Parameters

| Parameter          | Type                    | Description              | **M/O** | **비고**             |
|--------------------|-------------------------|--------------------------|---------|---------------------|
| sdJwtString        | String                  | SD-JWT 문자열              | M       |                     |
| trustedRootCerts   | List\<X509Certificate\> | 신뢰 루트 인증서 목록         | O       | null이면 체인만 검증   |
| expectedAudience   | String                  | 예상 Audience              | O       |                     |
| expectedNonce      | String                  | 예상 Nonce                 | O       |                     |

### Output Parameters

| Type            | Description        | **M/O** | **비고**                    |
|-----------------|--------------------|---------|-----------------------------|
| SDJWTClaimsSet  | 검증된 클레임 셋      | M       | [Link](#24-sdjwtclaimsset)  |

### Function Declaration

```java
static SDJWTClaimsSet verifyWithX5c(String sdJwtString)
static SDJWTClaimsSet verifyWithX5c(String sdJwtString, List<X509Certificate> trustedRootCerts)
static SDJWTClaimsSet verifyWithX5c(String sdJwtString, String expectedAudience, String expectedNonce)
static SDJWTClaimsSet verifyWithX5c(String sdJwtString, List<X509Certificate> trustedRootCerts,
    String expectedAudience, String expectedNonce)
```

<br>

## 1.3 KeyBindingJWTBuilder

### 1.3.1 createKeyBindingJWT

### Class Name
`KeyBindingJWTBuilder`

### Function Name
`createKeyBindingJWT`

### Function Introduction
`Holder의 Key Binding JWT를 생성합니다. SD-JWT 제출 시 Holder 인증에 사용됩니다.`

### Input Parameters

| Parameter        | Type          | Description              | **M/O** | **비고**              |
|------------------|---------------|--------------------------|---------|----------------------|
| holderKey        | PrivateKey    | Holder 개인키              | M       |                      |
| audience         | String        | Verifier의 Client ID      | M       | aud 클레임             |
| nonce            | String        | Nonce 값                  | M       |                      |
| sdJwtString      | String        | SD-JWT 문자열 (sd_hash용)   | O       | 포함 시 sd_hash 생성   |
| holderX5cChain   | List\<String\>| Holder x5c 인증서 체인      | O       |                      |

### Output Parameters

| Type   | Description          | **M/O** | **비고** |
|--------|----------------------|---------|---------|
| String | Key Binding JWT 문자열 | M       |         |

### Function Declaration

```java
static String createKeyBindingJWT(PrivateKey holderKey, String audience, String nonce)
static String createKeyBindingJWT(PrivateKey holderKey, String audience, String nonce, String sdJwtString)
static String createKeyBindingJWT(PrivateKey holderKey, List<String> x5cChain, String audience, String nonce)
static String createKeyBindingJWT(PrivateKey holderKey, List<String> x5cChain,
    String audience, String nonce, String sdJwtString)
```

<br>

## 1.4 OID4VPHandler

### 1.4.1 createVPToken

### Class Name
`OID4VPHandler`

### Function Name
`createVPToken`

### Function Introduction
`OID4VP 프로토콜을 위한 VP Token을 생성합니다. 요청된 클레임만 선택적으로 공개합니다.`

### Input Parameters

| Parameter         | Type           | Description             | **M/O** | **비고**              |
|-------------------|----------------|-------------------------|---------|----------------------|
| sdJwtVC           | String         | SD-JWT VC 원본 문자열      | M       |                      |
| requestedClaims   | Set\<String\>  | 요청된 클레임 이름 집합       | M       |                      |
| holderPrivateKey  | PrivateKey     | Holder 개인키             | M       | Key Binding용         |
| audience          | String         | Verifier Client ID      | M       | aud 클레임             |
| nonce             | String         | Nonce 값                 | M       |                      |
| holderX5cChain    | List\<String\> | Holder x5c 인증서 체인     | O       |                      |

### Output Parameters

| Type   | Description    | **M/O** | **비고**                           |
|--------|----------------|---------|------------------------------------|
| String | VP Token 문자열  | M       | 선택적 공개된 SD-JWT + Key Binding JWT |

### Function Declaration

```java
static String createVPToken(String sdJwtVC, Set<String> requestedClaims,
    PrivateKey holderPrivateKey, String audience, String nonce)
static String createVPToken(String sdJwtVC, Set<String> requestedClaims,
    PrivateKey holderPrivateKey, List<String> holderX5cChain, String audience, String nonce)
```

### Function Usage
```java
Set<String> requestedClaims = Set.of("name", "birthdate");

String vpToken = OID4VPHandler.createVPToken(
    sdJwtVCString,
    requestedClaims,
    holderPrivateKey,
    "did:web:verifier.example.com",
    "nonce123"
);
```

<br>

### 1.4.2 createVPTokenWithDcqlId

### Class Name
`OID4VPHandler`

### Function Name
`createVPTokenWithDcqlId`

### Function Introduction
`DCQL ID를 포함하여 VP Token을 생성합니다. DCQL 쿼리 기반 검증 요청에 응답할 때 사용합니다.`

### Input Parameters

| Parameter         | Type           | Description             | **M/O** | **비고**              |
|-------------------|----------------|-------------------------|---------|----------------------|
| sdJwtVC           | String         | SD-JWT VC 원본 문자열      | M       |                      |
| requestedClaims   | Set\<String\>  | 요청된 클레임 이름 집합       | M       |                      |
| dcqlId            | String         | DCQL Credential ID      | M       |                      |
| holderPrivateKey  | PrivateKey     | Holder 개인키             | M       |                      |
| audience          | String         | Verifier Client ID      | M       |                      |
| nonce             | String         | Nonce 값                 | M       |                      |
| holderX5cChain    | List\<String\> | Holder x5c 인증서 체인     | O       |                      |

### Output Parameters

| Type   | Description    | **M/O** | **비고** |
|--------|----------------|---------|---------|
| String | VP Token 문자열  | M       |         |

### Function Declaration

```java
static String createVPTokenWithDcqlId(String sdJwtVC, Set<String> requestedClaims,
    String dcqlId, PrivateKey holderPrivateKey, String audience, String nonce)
static String createVPTokenWithDcqlId(String sdJwtVC, Set<String> requestedClaims,
    String dcqlId, PrivateKey holderPrivateKey, List<String> holderX5cChain,
    String audience, String nonce)
```

<br>

## 1.5 SDJWT

### 1.5.1 parse

### Class Name
`SDJWT`

### Function Name
`parse`

### Function Introduction
`SD-JWT 문자열을 파싱하여 SDJWT 객체로 변환합니다.`

### Input Parameters

| Parameter    | Type   | Description      | **M/O** | **비고** |
|--------------|--------|------------------|---------|---------|
| sdJwtString  | String | SD-JWT 문자열      | M       |         |

### Output Parameters

| Type  | Description | **M/O** | **비고**           |
|-------|-------------|---------|-------------------|
| SDJWT | SDJWT 객체   | M       | [Link](#21-sdjwt) |

### Function Declaration

```java
static SDJWT parse(String sdJwtString)
```

### Function Usage
```java
SDJWT sdjwt = SDJWT.parse("eyJ...~eyJ...~eyJ...~");

String credentialJwt = sdjwt.getCredentialJwt();
List<Disclosure> disclosures = sdjwt.getDisclosures();
boolean hasKB = sdjwt.hasKeyBindingJwt();
```

<br>

<div style="page-break-after: always;"></div>

# 2. Data Classes

## 2.1 SDJWT

### Declaration

```java
public class SDJWT {
    private final String credentialJwt;
    private final List<Disclosure> disclosures;
    private final String keyBindingJwt;
}
```

### Property

| Parameter      | Type                | Description            | **M/O** |
|----------------|---------------------|------------------------|---------|
| credentialJwt  | String              | Issuer 서명 JWT          | M       |
| disclosures    | List\<Disclosure\>  | Disclosure 목록          | M       |
| keyBindingJwt  | String              | Key Binding JWT         | O       |

### Methods

| Method              | Return Type          | Description                 |
|---------------------|----------------------|-----------------------------|
| `getCredentialJwt()` | String              | Credential JWT 반환          |
| `getDisclosures()`   | List\<Disclosure\>  | Disclosure 목록 반환          |
| `getKeyBindingJwt()` | String              | Key Binding JWT 반환         |
| `hasKeyBindingJwt()` | boolean             | Key Binding JWT 존재 여부     |
| `getDisclosureCount()` | int               | Disclosure 개수 반환          |
| `toString()`         | String              | SD-JWT 직렬화 문자열 반환       |

<br>

## 2.2 Disclosure

### Declaration

```java
public class Disclosure {
    private final String salt;
    private final String claimName;
    private final Object claimValue;
}
```

### Property

| Parameter   | Type   | Description    | **M/O** | **비고**                    |
|-------------|--------|----------------|---------|-----------------------------|
| salt        | String | Salt 값         | M       |                             |
| claimName   | String | 클레임 이름      | O       | 배열 요소일 경우 null          |
| claimValue  | Object | 클레임 값        | M       |                             |

### Methods

| Method                    | Return Type          | Description                    |
|---------------------------|----------------------|--------------------------------|
| `getSalt()`               | String               | Salt 반환                       |
| `getClaimName()`          | String               | 클레임 이름 반환                  |
| `getClaimValue()`         | Object               | 클레임 값 반환                   |
| `isArrayElement()`        | boolean              | 배열 요소 여부                   |
| `getDisclosure()`         | String               | Base64URL 인코딩 문자열 반환      |
| `digest()`                | String               | SHA-256 해시 다이제스트 반환       |
| `digest(String algorithm)` | String              | 지정 알고리즘 해시 다이제스트 반환   |

### Static Factory Methods

| Method                                         | Description                  |
|------------------------------------------------|------------------------------|
| `forObjectProperty(String name, Object value)` | Object 속성 Disclosure 생성    |
| `parse(String disclosureString)`               | Base64URL 문자열에서 파싱        |

<br>

## 2.3 DisclosureFrame

### Declaration

```java
public class DisclosureFrame {
    private final List<String> sdFieldNames;
    private final Map<String, DisclosureFrame> nestedFrames;
}
```

### Property

| Parameter     | Type                              | Description            | **M/O** |
|---------------|-----------------------------------|------------------------|---------|
| sdFieldNames  | List\<String\>                    | 선택적 공개 대상 필드 이름  | M       |
| nestedFrames  | Map\<String, DisclosureFrame\>    | 중첩 Disclosure Frame   | O       |

### Methods

| Method                                              | Return Type    | Description               |
|-----------------------------------------------------|----------------|---------------------------|
| `addSdField(String fieldName)`                      | DisclosureFrame | 선택적 공개 필드 추가         |
| `addNestedFrame(String name, DisclosureFrame frame)` | DisclosureFrame | 중첩 Frame 추가             |
| `isSdField(String fieldName)`                       | boolean        | 선택적 공개 대상 여부          |
| `validate(Map<String, Object> claims)`              | void           | 클레임 대비 Frame 유효성 검증   |
| `fromJson(String jsonString)`                       | DisclosureFrame | JSON에서 DisclosureFrame 생성 |

<br>

## 2.4 SDJWTClaimsSet

### Declaration

```java
public class SDJWTClaimsSet {
    private final Map<String, Object> claims;
    private final List<Disclosure> disclosures;
}
```

### Property

| Parameter    | Type                     | Description        | **M/O** |
|--------------|--------------------------|--------------------|---------|
| claims       | Map\<String, Object\>    | 검증된 클레임 맵      | M       |
| disclosures  | List\<Disclosure\>       | 검증된 Disclosure 목록 | M       |

### Methods

| Method                         | Return Type          | Description           |
|--------------------------------|----------------------|-----------------------|
| `getClaims()`                  | Map\<String, Object\> | 전체 클레임 맵 반환       |
| `getClaim(String name)`        | Object               | 특정 클레임 값 반환       |
| `getStringClaim(String name)`  | String               | 문자열 클레임 반환        |
| `getLongClaim(String name)`    | Long                 | Long 클레임 반환         |
| `getBooleanClaim(String name)` | Boolean              | Boolean 클레임 반환      |
| `getClaimNames()`             | Set\<String\>         | 클레임 이름 집합 반환      |
| `hasClaim(String name)`       | boolean               | 클레임 존재 여부 확인      |

<br>

## 2.5 SignedJWT

### Declaration

```java
public class SignedJWT {
    private String header;
    private String payload;
    private String signature;
}
```

### Property

| Parameter  | Type   | Description           | **M/O** |
|------------|--------|-----------------------|---------|
| header     | String | Base64URL 인코딩 헤더    | M       |
| payload    | String | Base64URL 인코딩 페이로드 | M       |
| signature  | String | Base64URL 인코딩 서명    | O       |

### Methods

| Method                      | Return Type              | Description          |
|-----------------------------|--------------------------|----------------------|
| `getJWTClaimsSet()`         | Map\<String, Object\>   | 페이로드 클레임 맵 반환   |
| `getHeader()`               | Map\<String, Object\>   | 헤더 맵 반환           |
| `getSigningInput()`         | String                  | 서명 입력 문자열 반환     |
| `getSignatureBytes()`       | byte[]                  | 서명 바이트 배열 반환     |
| `sign(JWSSigner signer)`    | void                    | JWT 서명 수행          |
| `serialize()`               | String                  | JWT 직렬화 문자열 반환    |
| `verify(JWSVerifier verifier)` | boolean              | JWT 서명 검증          |
| `parse(String jwt)`         | SignedJWT               | JWT 문자열 파싱 (static) |

<br>

<div style="page-break-after: always;"></div>

# 3. Interfaces

## 3.1 JWSSigner

### Declaration

```java
public interface JWSSigner {
    byte[] sign(String signingInput) throws SDJWTException;
}
```

### Description
`JWT 서명을 위한 인터페이스입니다. ECDSASigner, RSASSASigner 구현체가 제공됩니다.`

### Implementations

| Class        | Description              |
|--------------|--------------------------|
| ECDSASigner  | ECDSA (ES256) 서명 구현체   |
| RSASSASigner | RSA (RS256) 서명 구현체     |

<br>

## 3.2 JWSVerifier

### Declaration

```java
public interface JWSVerifier {
    boolean verify(SignedJWT signedJWT) throws SDJWTException;
}
```

### Description
`JWT 서명 검증을 위한 인터페이스입니다. ECDSAVerifier, RSASSAVerifier 구현체가 제공됩니다.`

### Implementations

| Class          | Description              |
|----------------|--------------------------|
| ECDSAVerifier  | ECDSA (ES256) 검증 구현체   |
| RSASSAVerifier | RSA (RS256) 검증 구현체     |

<br>

<div style="page-break-after: always;"></div>

# 4. Enums

## 4.1 SDJWTErrorCode

### Declaration

```java
public enum SDJWTErrorCode implements SDJWTErrorCodeInterface {
    // General (00xxx)
    ERR_CODE_GENERAL_INVALID_PARAMETER("MSDKSDJ00001", "Invalid parameter"),
    ERR_CODE_GENERAL_NULL_PARAMETER("MSDKSDJ00002", "Parameter cannot be null"),
    ...
}
```

### Description
`SDK 에러 코드 열거형입니다. 코드는 MSDKSDJ 접두사로 시작합니다.`

### Methods

| Method                        | Return Type     | Description           |
|-------------------------------|----------------|-----------------------|
| `getCode()`                   | String         | 에러 코드 문자열 반환     |
| `getMsg()`                    | String         | 에러 메시지 반환         |
| `getByCode(String code)`      | SDJWTErrorCode | 코드로 에러 코드 조회     |

> 상세 에러 코드 목록은 [SDJWTVCSDKError](SDJWTVCSDKError.md) 문서를 참고하세요.

<br>
