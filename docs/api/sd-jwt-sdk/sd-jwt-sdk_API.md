# SD-JWT SDK API Documentation

- Subject: SD-JWT SDK (org.omnione.did.sdjwt + org.omnione.did.oid4vc) API Documentation
- Writer: Suhyun Forten Lee
- Date: 2025-11-13
- Version: v1.0.0

| Version | Date | History |
| --- | --- | --- |
| v1.0.0 | 2025-11-13 | Initial Draft |

## Table of Contents

1. [Overview](#1-overview)
2. [API List](#2-api-list)
3. [SD-JWT Issuance](#3-sd-jwt-issuance)
4. [DCQL Processing](#4-dcql-processing)
5. [VP Token Generation](#5-vp-token-generation)
6. [VP Token Verification](#6-vp-token-verification)
7. [Error Codes](#7-error-codes)

---

## 1. Overview

This document is written from a practical perspective based on the actual usage flow of the SD-JWT SDK. The main features of this SDK are as follows:

- **SD-JWT Issuance**: Issue SD-JWT VC through OID4VCIssuer
- **DCQL Validation**: Create and validate credential queries
- **VP Token Generation**: Generate VP Token based on selective disclosure
- **VP Token Verification**: Verify issuer signature and key binding

---

## 2. API List

| Category | Class/Method | Description |
| --- | --- | --- |
| **SD-JWT Issuance** | `OID4VCIssuer.issueCredential()` | Issue SD-JWT VC |
| **DCQL Creation** | `DCQLQuery.builder()` | Create DCQL Query object |
| **DCQL Validation** | `DCQLQueryValidator.validate()` | Validate DCQL Query |
| **DCQL Matching** | `DCQLCredentialMatcher.matchesMetadata()` | Match SD-JWT and DCQL metadata |
| **Claim Extraction** | `DCQLCredentialMatcher.extractMatchingClaimNames()` | Extract required claims based on DCQL |
| **VP Token Generation** | `OID4VPHandler.createVPToken()` | Generate VP Token |
| **VP Token Verification** | `SDJWTVerifier.verify()` | Verify VP Token |

---

## 3. SD-JWT Issuance

### Overview

Issue SD-JWT VC supporting selective disclosure using OID4VCIssuer. All claims are automatically managed as Disclosures.

#### Method Signature

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

#### Request Example

```java
import org.omnione.did.oid4vc.oid4vci.core.OID4VCIssuer;
import org.omnione.did.wallet.key.WalletManagerFactory;

// 1. Connect Open DID Wallet
WalletManagerInterface walletManager = WalletManagerFactory.getWalletManager(WalletManagerType.FILE);
walletManager.connect("/path/to/wallet", "password".toCharArray());

// 2. Create Issuer
OID4VCIssuer issuer = new OID4VCIssuer(walletManager, "assert", "did:omn:issuer");

// 3. Define credential claims (all are selectively disclosable)
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

// 4. Issue SD-JWT VC
String identityVC = issuer.issueCredential(
    "https://credentials.gov.kr/identity_credential",
    identityInfo,
    holderPublicKey
);

System.out.println("Issued SD-JWT VC:");
System.out.println(identityVC);
```

#### Response Example

```
eyJhbGciOiJFUzI1NiIsInR5cCI6InZjK3NkLWp3dCIsImtpZCI6ImtleTEifQ.eyJpc3MiOiJkaWQ6b21uOmlzc3VlciIsInZjdCI6Imh0dHBzOi8vY3JlZGVudGlhbHMuZ292LmtyL2lkZW50aXR5X2NyZWRlbnRpYWwiLCJzdWIiOiJ1c2VyIiwiaWF0IjoxNjk5NTAwMDAwLCJleHAiOjE3MzA1MDAwMDAsImNuZiI6eyJraWQiOiJob2xkZXJfa2V5In0sIl9zZCI6WyJoYXNoMSIsImhhc2gyIiwiaGFzaDMiLCJoYXNoNCJdLCJfc2RfYWxnIjoic2hhLTI1NiJ9.signature~disclosure1~disclosure2~disclosure3~disclosure4~
```

---

## 4. DCQL Processing

### 4.1. DCQL Query Creation

Define credential queries. The path of ClaimQuery can be either String (claim name) or Integer (object index).

#### Method Signature

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

#### Request Example

```java
import org.omnione.did.oid4vc.dcql.datamodel.DCQLQuery;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// 1. Configure DCQL Meta information
Map<String, Object> dcqlMeta = new HashMap<>();
dcqlMeta.put("vct_values", Arrays.asList("https://credentials.gov.kr/identity_credential"));
dcqlMeta.put("issuer_did", "did:omn:issuer");

// 2. Create Claim Query (single path request)
DCQLQuery.ClaimQuery genderQuery = DCQLQuery.ClaimQuery.builder()
    .path(Arrays.asList("gender"))
    .build();

// 3. Configure Claim Query: Claim name based
DCQLQuery.ClaimQuery firstQuery = DCQLQuery.ClaimQuery.builder()
    .path(Arrays.asList("gender"))
    .build();

// 3. Configure Claim Query: Array based
DCQLQuery.ClaimQuery secondQuery = DCQLQuery.ClaimQuery.builder()
    .path(Arrays.asList(0))
    .build();

// 4. Create Credential Query
DCQLQuery.CredentialQuery credentialQuery = DCQLQuery.CredentialQuery.builder()
    .id("identity_credential")
    .format("vc+sd-jwt")
    .meta(dcqlMeta)
    .claims(Arrays.asList(genderQuery, firstQuery, secondQuery))
    .purpose("Identity verification")
    .requireCryptographicHolderBinding(true)
    .build();

// 5. Complete DCQL Query
DCQLQuery dcqlQuery = DCQLQuery.builder()
    .credentials(Arrays.asList(credentialQuery))
    .build();

System.out.println("DCQL Query created");
System.out.println("  - Credential ID: " + credentialQuery.getId());
System.out.println("  - Required Claims: " + credentialQuery.getClaims().size());
```

---

### 4.2. DCQL Query Validation

Validate the validity of the created DCQL Query.

#### Method Signature

```java
public class DCQLQueryValidator {
    public static ValidationResult validate(DCQLQuery dcqlQuery);
    
    public static class ValidationResult {
        public boolean isValid();
    }
}
```

#### Request Example

```java
import org.omnione.did.oid4vc.dcql.core.DCQLQueryValidator;

// 1. Validate DCQL Query
DCQLQueryValidator.ValidationResult validationResult = DCQLQueryValidator.validate(dcqlQuery);

// 2. Check validation result
if (validationResult.isValid()) {
    System.out.println("DCQL Query validation: SUCCESS");
} else {
    System.out.println("DCQL Query validation: FAILED");
}
```

---

### 4.3. DCQL and SD-JWT Matching

Verify that DCQL metadata matches the SD-JWT and extract required claims.

#### Method Signature

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

#### Request Example

```java
import org.omnione.did.oid4vc.dcql.core.DCQLCredentialMatcher;

// 1. Check metadata matching
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

// 2. Extract required claim names
Set<String> dcqlRequiredClaims = DCQLCredentialMatcher.extractMatchingClaimNames(
    dcqlQuery,
    parsedVC
);

System.out.println("Required claim names: " + dcqlRequiredClaims);
// Output: [gender, family_name, given_name]
```

---

## 5. VP Token Generation

### Overview

Generate VP Token by selecting only the required claims based on DCQL. Key Binding JWT is automatically included.

#### Method Signature

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

#### Request Example

```java
import org.omnione.did.oid4vc.oid4vp.core.OID4VPHandler;

// 1. Generate VP Token (includes only selected claims)
String vpToken = OID4VPHandler.createVPToken(
    identityVC,                    // Original SD-JWT VC
    dcqlRequiredClaims,            // List of required claims {gender, family_name, given_name}
    holderPrivateKey,              // Holder private key
    "did:omn:issuer",              // Audience
    "dcql-nonce-456"               // Nonce
);

System.out.println("DCQL-based VP Token:");
System.out.println(vpToken);
```

#### Response Example

```
eyJhbGciOiJFUzI1NiIsInR5cCI6InZjK3NkLWp3dCIsImtpZCI6ImtleTEifQ.eyJpc3MiOiJkaWQ6b21uOmlzc3VlciIsInZjdCI6Imh0dHBzOi8vY3JlZGVudGlhbHMuZ292LmtyL2lkZW50aXR5X2NyZWRlbnRpYWwiLCJzdWIiOiJ1c2VyIiwiaWF0IjoxNjk5NTAwMDAwLCJleHAiOjE3MzA1MDAwMDAsImNuZiI6eyJraWQiOiJob2xkZXJfa2V5In0sIl9zZCI6WyJnZW5kZXJfaGFzaCIsImZhbWlseV9uYW1lX2hhc2giLCJnaXZlbl9uYW1lX2hhc2giXSwiX3NkX2FsZyI6InNoYS0yNTYifQ.signature~gender_disclosure~family_name_disclosure~given_name_disclosure~kb_jwt~
```

---

## 6. VP Token Verification

### Overview

Verify the holder's VP Token using the issuer's public key and holder's public key. Comprehensively verifies issuer signature, key binding JWT, and disclosure integrity.

#### Method Signature

```java
public class SDJWTVerifier {
    public SDJWTVerifier(PublicKey issuerPublicKey, PublicKey holderPublicKey);
    
    public SDJWTClaimsSet verify(String vpTokenString, String expectedAudience, String expectedNonce);
    
    public static class SDJWTClaimsSet {
        public Map<String, Object> getClaims();
    }
}
```

#### Request Example

```java
import org.omnione.did.oid4vc.oid4vp.core.SDJWTVerifier;
import org.omnione.did.oid4vc.exception.OID4VCException;

// 1. Create Verifier
SDJWTVerifier verifier = new SDJWTVerifier(issuerPublicKey, holderPublicKey);

// 2. Verify VP Token
try {
    SDJWTVerifier.SDJWTClaimsSet claims = verifier.verify(
        vpToken,
        "did:omn:issuer",      // Expected audience
        "dcql-nonce-456"       // Expected nonce
    );
    
    System.out.println("VP Token verification: SUCCESS");
    
    // 3. Check verified claims
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

#### Response Example (Success)

```
VP Token verification: SUCCESS

gender: male
family_name: Kim
given_name: Raon
```

#### Verification Process

1. **SD-JWT Structure Validation**: Verify token format and components
2. **Issuer Signature Verification**: Verify Credential JWT signed with issuer's private key
3. **Disclosure Integrity Verification**: Verify all disclosure hashes match the _sd array in JWT payload
4. **Key Binding JWT Verification**: Verify KB JWT signed with holder's private key
5. **sd_hash Verification**: Verify sd_hash claim in KB JWT matches the SD-JWT
6. **Audience/Nonce Verification**: Verify against expected values

---

## 7. Error Codes

| Exception Class | Description | Resolution |
| --- | --- | --- |
| `OID4VCException` | Processing error during DCQL, OID4VCI, OID4VP, etc. | Check error message |
| `SDJWTException` | SD-JWT structure or parsing error | Verify SD-JWT format |