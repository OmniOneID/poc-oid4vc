# SD-JWT SDK API 문서

- Subject: SD-JWT SDK (org.omnione.did.sdjwt + org.omnione.did.oid4vc) API Document
- Writer: 이수현
- Date: 2025-11-13
- Version: v1.0.0

| Version | Date | History |
| --- | --- | --- |
| v1.0.0 | 2025-11-13 | 초기 작성 |

## 목차

1. [개요](#1-개요)
2. [API 목록](#2-api-목록)
3. [SD-JWT 발급](#3-sd-jwt-발급)
4. [DCQL 처리](#4-dcql-처리)
5. [VP Token 생성](#5-vp-token-생성)
6. [VP Token 검증](#6-vp-token-검증)
7. [에러 코드](#7-에러-코드)

---

## 1. 개요

본 문서는 SD-JWT SDK의 실제 사용 흐름을 기반으로 실용적 관점에서 작성되었습니다. 본 SDK의 주요 기능은 다음과 같습니다:

- **SD-JWT 발급**: OID4VCIssuer를 통한 SD-JWT VC 발급
- **DCQL 검증**: 자격증명 쿼리 생성 및 검증
- **VP Token 생성**: 선택적 공개 기반 VP Token 생성
- **VP Token 검증**: Issuer 서명 및 Key Binding 검증

---

## 2. API 목록

| 카테고리 | 클래스/메서드 | 설명 |
| --- | --- | --- |
| **SD-JWT 발급** | `OID4VCIssuer.issueCredential()` | SD-JWT VC 발급 |
| **DCQL 생성** | `DCQLQuery.builder()` | DCQL Query 객체 생성 |
| **DCQL 검증** | `DCQLQueryValidator.validate()` | DCQL Query 검증 |
| **DCQL 매칭** | `DCQLCredentialMatcher.matchesMetadata()` | SD-JWT과 DCQL 메타데이터 매칭 |
| **클레임 추출** | `DCQLCredentialMatcher.extractMatchingClaimNames()` | DCQL 기반 필요 클레임 추출 |
| **VP Token 생성** | `OID4VPHandler.createVPToken()` | VP Token 생성 |
| **VP Token 검증** | `SDJWTVerifier.verify()` | VP Token 검증 |

---

## 3. SD-JWT 발급

### 개요

OID4VCIssuer를 사용하여 선택적 공개를 지원하는 SD-JWT VC를 발급합니다. 모든 클레임이 자동으로 Disclosure로 관리됩니다.

#### 메서드 시그니처

```java
public class OID4VCIssuer {
    public OID4VCIssuer(WalletManagerInterface walletManager, String keyType, String issuerDid);
    
    public String issueCredential(
        String credentialType,
        Map<String, Object> claims,
        PublicKey holderPublicKey
    );
}
```

#### 요청 예시

```java
import org.omnione.did.oid4vc.oid4vci.core.OID4VCIssuer;
import org.omnione.did.wallet.key.WalletManagerFactory;

// 1. Open DID Wallet 연결
WalletManagerInterface walletManager = WalletManagerFactory.getWalletManager(WalletManagerType.FILE);
walletManager.connect("/path/to/wallet", "password".toCharArray());

// 2. Issuer 생성
OID4VCIssuer issuer = new OID4VCIssuer(walletManager, "assert", "did:omn:issuer");

// 3. 자격증명 클레임 정의 (모두 선택적 공개 가능)
Map<String, Object> identityInfo = Map.of(
    "given_name", "Raon",
    "family_name", "Kim",
    "birth_date", "1990-01-01",
    "gender", "male",
    "nationality", "KR",
    "id_number", "900101-1234567",
    "phone_number", "+82-10-1234-5678",
    "email", "raonkim@raoncorp.com"
);

// 4. SD-JWT VC 발급
String identityVC = issuer.issueCredential(
    "https://credentials.gov.kr/identity_credential",
    identityInfo,
    holderPublicKey
);

System.out.println("Issued SD-JWT VC:");
System.out.println(identityVC);
```

#### 응답 예시

```
eyJhbGciOiJFUzI1NiIsInR5cCI6InZjK3NkLWp3dCIsImtpZCI6ImtleTEifQ.eyJpc3MiOiJkaWQ6b21uOmlzc3VlciIsInZjdCI6Imh0dHBzOi8vY3JlZGVudGlhbHMuZ292LmtyL2lkZW50aXR5X2NyZWRlbnRpYWwiLCJzdWIiOiJ1c2VyIiwiaWF0IjoxNjk5NTAwMDAwLCJleHAiOjE3MzA1MDAwMDAsImNuZiI6eyJraWQiOiJob2xkZXJfa2V5In0sIl9zZCI6WyJoYXNoMSIsImhhc2gyIiwiaGFzaDMiLCJoYXNoNCJdLCJfc2RfYWxnIjoic2hhLTI1NiJ9.signature~disclosure1~disclosure2~disclosure3~disclosure4~
```

---

## 4. DCQL 처리

### 4.1. DCQL Query 생성

자격증명 쿼리를 정의합니다. ClaimQuery의 path는 String(클레임명) 또는 Integer(객체 인덱스) 가능합니다.

#### 메서드 시그니처

```java
public class DCQLQuery {
    public static DCQLQuery.Builder builder();
    public List<CredentialQuery> getCredentials();
}

public static class CredentialQuery {
    public static CredentialQuery.Builder builder();
    
    public static class Builder {
        public Builder id(String id);
        public Builder format(String format);
        public Builder meta(Map<String, Object> meta);
        public Builder claims(List<ClaimQuery> claims);
        public Builder purpose(String purpose);
        public Builder requireCryptographicHolderBinding(boolean require);
        public CredentialQuery build();
    }
    
    public String getId();
    public Map<String, Object> getMeta();
    public List<ClaimQuery> getClaims();
}

public static class ClaimQuery {
    public static ClaimQuery.Builder builder();
    
    public static class Builder {
        public Builder path(List<?> path);
        public ClaimQuery build();
    }
}
```

#### 요청 예시

```java
import org.omnione.did.oid4vc.dcql.datamodel.DCQLQuery;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// 1. DCQL Meta 정보 설정
Map<String, Object> dcqlMeta = new HashMap<>();
dcqlMeta.put("vct_values", Arrays.asList("https://credentials.gov.kr/identity_credential"));
dcqlMeta.put("issuer_did", "did:omn:issuer");

// 2. Claim Query 생성 (단일 경로 요청)
DCQLQuery.ClaimQuery genderQuery = DCQLQuery.ClaimQuery.builder()
    .path(Arrays.asList("gender"))
    .build();

// 3. 클레임 쿼리 설정 : 클레임 네임 기반
DCQLQuery.ClaimQuery firstQuery = DCQLQuery.ClaimQuery.builder()
    .path(Arrays.asList("gender"))
    .build();

// 3. 클레임 쿼리 설정 : 배열 기반
DCQLQuery.ClaimQuery secondQuery = DCQLQuery.ClaimQuery.builder()
    .path(Arrays.asList(0))
    .build();

// 4. Credential Query 생성
DCQLQuery.CredentialQuery credentialQuery = DCQLQuery.CredentialQuery.builder()
    .id("identity_credential")
    .format("vc+sd-jwt")
    .meta(dcqlMeta)
    .claims(Arrays.asList(genderQuery, firstQuery, secondQuery))
    .purpose("Identity verification")
    .requireCryptographicHolderBinding(true)
    .build();

// 5. DCQL Query 완성
DCQLQuery dcqlQuery = DCQLQuery.builder()
    .credentials(Arrays.asList(credentialQuery))
    .build();

System.out.println("DCQL Query created");
System.out.println("  - Credential ID: " + credentialQuery.getId());
System.out.println("  - Required Claims: " + credentialQuery.getClaims().size());
```

---

### 4.2. DCQL Query 검증

생성된 DCQL Query의 유효성을 검증합니다.

#### 메서드 시그니처

```java
public class DCQLQueryValidator {
    public static ValidationResult validate(DCQLQuery dcqlQuery);
    
    public static class ValidationResult {
        public boolean isValid();
    }
}
```

#### 요청 예시

```java
import org.omnione.did.oid4vc.dcql.core.DCQLQueryValidator;

// 1. DCQL Query 검증
DCQLQueryValidator.ValidationResult validationResult = DCQLQueryValidator.validate(dcqlQuery);

// 2. 검증 결과 확인
if (validationResult.isValid()) {
    System.out.println("DCQL Query validation: SUCCESS");
} else {
    System.out.println("DCQL Query validation: FAILED");
}
```

---

### 4.3. DCQL과 SD-JWT 매칭

DCQL의 메타데이터와 SD-JWT이 일치하는지 확인하고, 필요한 클레임을 추출합니다.

#### 메서드 시그니처

```java
public class DCQLCredentialMatcher {
    public static boolean matchesMetadata(
        SDJWT sdjwt,
        Map<String, Object> dcqlMeta
    );
    
    public static Set<String> extractMatchingClaimNames(
        DCQLQuery dcqlQuery,
        SDJWT sdjwt
    );
}
```

#### 요청 예시

```java
import org.omnione.did.oid4vc.dcql.core.DCQLCredentialMatcher;

// 1. 메타데이터 매칭 확인
boolean isMatching = DCQLCredentialMatcher.matchesMetadata(
    parsedVC,
    credentialQuery.getMeta()
);

if (isMatching) {
    System.out.println("SD-JWT and DCQL metadata: MATCH");
} else {
    System.out.println("SD-JWT and DCQL metadata: MISMATCH");
    return;
}

// 2. 필요 클레임명 추출
Set<String> dcqlRequiredClaims = DCQLCredentialMatcher.extractMatchingClaimNames(
    dcqlQuery,
    parsedVC
);

System.out.println("Required claim names: " + dcqlRequiredClaims);
// Output: [gender, family_name, given_name]
```

---

## 5. VP Token 생성

### 개요

DCQL 기반으로 필요한 클레임만 선택하여 VP Token을 생성합니다. 자동으로 Key Binding JWT가 포함됩니다.

#### 메서드 시그니처

```java
public class OID4VPHandler {
    public static String createVPToken(
        String sdJwtString,
        Set<String> requestedClaims,
        PrivateKey holderPrivateKey,
        String audience,
        String nonce
    );
}
```

#### 요청 예시

```java
import org.omnione.did.oid4vc.oid4vp.core.OID4VPHandler;

// 1. VP Token 생성 (선택된 클레임만 포함)
String vpToken = OID4VPHandler.createVPToken(
    identityVC,                    // 원본 SD-JWT VC
    dcqlRequiredClaims,            // 필요한 클레임 목록 {gender, family_name, given_name}
    holderPrivateKey,              // Holder 개인키
    "did:omn:issuer",              // Audience
    "dcql-nonce-456"               // Nonce
);

System.out.println("DCQL-based VP Token:");
System.out.println(vpToken);
```

#### 응답 예시

```
eyJhbGciOiJFUzI1NiIsInR5cCI6InZjK3NkLWp3dCIsImtpZCI6ImtleTEifQ.eyJpc3MiOiJkaWQ6b21uOmlzc3VlciIsInZjdCI6Imh0dHBzOi8vY3JlZGVudGlhbHMuZ292LmtyL2lkZW50aXR5X2NyZWRlbnRpYWwiLCJzdWIiOiJ1c2VyIiwiaWF0IjoxNjk5NTAwMDAwLCJleHAiOjE3MzA1MDAwMDAsImNuZiI6eyJraWQiOiJob2xkZXJfa2V5In0sIl9zZCI6WyJnZW5kZXJfaGFzaCIsImZhbWlseV9uYW1lX2hhc2giLCJnaXZlbl9uYW1lX2hhc2giXSwiX3NkX2FsZyI6InNoYS0yNTYifQ.signature~gender_disclosure~family_name_disclosure~given_name_disclosure~kb_jwt~
```

---

## 6. VP Token 검증

### 개요

Holder의 VP Token을 Issuer 공개키와 Holder 공개키로 검증합니다. Issuer 서명, Key Binding JWT, Disclosure 무결성을 종합적으로 검증합니다.

#### 메서드 시그니처

```java
public class SDJWTVerifier {
    public SDJWTVerifier(PublicKey issuerPublicKey, PublicKey holderPublicKey);
    
    public SDJWTClaimsSet verify(String vpTokenString, String expectedAudience, String expectedNonce);
    
    public static class SDJWTClaimsSet {
        public Map<String, Object> getClaims();
    }
}
```

#### 요청 예시

```java
import org.omnione.did.oid4vc.oid4vp.core.SDJWTVerifier;
import org.omnione.did.oid4vc.exception.OID4VCException;

// 1. Verifier 생성
SDJWTVerifier verifier = new SDJWTVerifier(issuerPublicKey, holderPublicKey);

// 2. VP Token 검증
try {
    SDJWTVerifier.SDJWTClaimsSet claims = verifier.verify(
        vpToken,
        "did:omn:issuer",      // 예상 audience
        "dcql-nonce-456"       // 예상 nonce
    );
    
    System.out.println("VP Token verification: SUCCESS");
    
    // 3. 검증된 클레임 확인
    claims.getClaims().forEach((key, value) -> {
        if (!key.startsWith("_") && !key.equals("iss") && 
            !key.equals("iat") && !key.equals("exp") && 
            !key.equals("vct") && !key.equals("cnf")) {
            System.out.println(key + ": " + value);
        }
    });
    
} catch (OID4VCException e) {
    System.out.println("VP Token verification: FAILED");
    System.out.println("Error: " + e.getMessage());
}
```

#### 응답 예시 (성공)

```
VP Token verification: SUCCESS

gender: male
family_name: Kim
given_name: Raon
```

#### 검증 프로세스

1. **SD-JWT 구조 검증**: 토큰 형식 및 구성 요소 확인
2. **Issuer 서명 검증**: Issuer의 개인키로 서명된 Credential JWT 검증
3. **Disclosure 무결성 검증**: 모든 Disclosure의 해시가 JWT 페이로드의 _sd 배열과 일치하는지 확인
4. **Key Binding JWT 검증**: Holder의 개인키로 서명된 KB JWT 검증
5. **sd_hash 검증**: KB JWT의 sd_hash 클레임이 SD-JWT과 일치하는지 확인
6. **Audience/Nonce 검증**: 예상 값과 일치하는지 확인

---

## 7. 에러 코드

| 예외 클래스 | 설명 | 해결 방법 |
| --- | --- | --- |
| `OID4VCException` | DCQL 포함 OID4VCI, OID4VP 등 과정에서의 처리 오류 | 에러 메시지 확인 |
| `SDJWTException` | SD-JWT 구조 또는 파싱 오류 | SD-JWT 형식 확인 |