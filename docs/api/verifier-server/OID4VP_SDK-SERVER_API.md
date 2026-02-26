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

- Subject: OID4VP SDK Server API
- Author: Suhyun Forten Lee
- Date: 2026-01-30
- Version: v1.0.0

| Version | Date | Changes |
|---------|------|---------|
| v1.0.0 | 2026-01-30 | Initial release |


<div style="page-break-after: always;"></div>

# Table of Contents
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

## Overview

This document defines the Server SDK API for implementing the **OpenID for Verifiable Presentations (OID4VP)** protocol.
It enables Verifier server developers to easily implement VP (Verifiable Presentation) verification functionality.

### Key Features
- Authorization Request creation and signing (RFC9101 JAR support)
- DCQL (Digital Credentials Query Language) based Credential requests
- VP Token reception and verification (multi-format support including SD-JWT, mDoc)
- Session management and state tracking

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
`Initializes an OID4VP verification request. Processes DCQL query or scope parameters to generate an Authorization Request URI.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| dcqlQuery | String | DCQL query JSON string | O | Mutually exclusive with scope |
| scope | String | Scope parameter | O | Mutually exclusive with dcqlQuery |
| responseMode | String | Response mode (direct_post, query, etc.) | M | |
| clientMetadata | String | Client metadata JSON | O | |
| useRequestUri | boolean | Whether to use request_uri (by_reference) | M | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| ServiceResult\<Map\<String, Object\>\> | Initialization result | M | [Link](#31-serviceresult) |

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

// Initialize with DCQL query
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
`Generates an RFC9101 compliant JWS format Authorization Request using CompactSigner.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| requestId | String | Request identifier | M | |
| compactSigner | CompactSigner | Signing interface | M | [Link](#21-compactsigner) |
| publicKeyMultibase | String | Multibase encoded public key | M | For JWK header |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| ServiceResult\<String\> | JWS string | M | Content-Type: application/oauth-authz-req+jwt |

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
    // Implement signing logic
    return signatureBytes;
};

ServiceResult<String> result = authorizationService.getAuthorizationRequest(
    requestId, 
    signer, 
    "zPublicKeyMultibase..."
);

if (result.isSuccess()) {
    String jws = result.getData();
    // Return JWS
}
```

<br>

### 1.2.2 getAuthorizationRequest (PrivateKey)

### Class Name
`AuthorizationService`

### Function Name
`getAuthorizationRequest`

### Function Introduction
`Generates an RFC9101 compliant JWS format Authorization Request using PrivateKey directly.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| requestId | String | Request identifier | M | |
| privateKey | PrivateKey | Private key for signing | M | java.security.PrivateKey |
| publicKeyMultibase | String | Multibase encoded public key | M | For JWK header |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| ServiceResult\<String\> | JWS string | M | |

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
`Receives and processes VP Token response from Wallet.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| vpTokenMap | Map\<String, List\<Object\>\> | VP Token map (credential type → credentials) | O | |
| issuerPublicKeys | List\<String\> | List of Base64 encoded issuer public keys | M | |
| holderPublicKeys | List\<String\> | List of Base64 encoded holder public keys | O | Can be null |
| state | String | State parameter | M | |
| error | String | Error code | O | |
| errorDescription | String | Error description | O | |
| httpMethod | String | HTTP method | M | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| ServiceResult\<Map\<String, Object\>\> | Verification result | M | |

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
`Performs complete verification of VP Token. Includes both signature verification and Presentation Binding verification.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| credential | String | Credential string to verify | M | |
| verificationConfig | VerificationConfig | Verification configuration | M | [Link](#33-verificationconfig) |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| boolean | Verification success status | M | |

### Function Declaration

```java
boolean verifyVerifiablePresentation(String credential, VerificationConfig verificationConfig) 
    throws FormatterException
```

### Function Usage
```java
VPTokenVerifier verifier = new SDJWTVerifier(); // Implementation

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
`Verifies only the cryptographic signature of Credential. Does not verify Presentation Binding.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| credential | String | Credential string | M | |
| issuerPublicKey | String | Base64 encoded issuer compressed public key | M | |
| holderPublicKey | String | Base64 encoded holder compressed public key | O | Can be null |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| boolean | Signature validity | M | |

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
`Verifies that Credential's Presentation Binding (aud, nonce) matches expected values.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| credential | String | Credential string | M | |
| expectedClientId | String | Expected Client ID (aud) | M | |
| expectedNonce | String | Expected Nonce | M | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| boolean | Binding match status | M | |

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
`Extracts issuer identifier (kid or x5c) from Credential.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| credential | String | Credential string | M | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| IdentifierResult | Identifier type and value | O | Can be null |

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
`Extracts holder identifier (cnf.kid or cnf.x5c) from Credential.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| credential | String | Credential string | M | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| IdentifierResult | Identifier type and value | O | null if no Key Binding |

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
`Parses raw Credential string into ParsedCredential object.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| rawCredential | String | Raw Credential string | M | |
| format | String | Credential format | O | Auto-detect if null |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| ParsedCredential | Parsed Credential | M | [Link](#35-parsedcredential) |

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
`Checks if the requested format is supported.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| requiredFormat | String | Requested format | O | Returns true if null |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| boolean | Support status | M | |

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
`Checks if Credential matches metadata requirements (vct_values, doctype, etc.).`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| credential | ParsedCredential | Parsed Credential | M | |
| metadata | Map\<String, Object\> | Metadata requirements | O | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| boolean | Match status | M | |

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
`Extracts matching claim names from Credential based on DCQL query.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| dcqlQuery | DCQLQuery | DCQL query | M | [Link](#34-dcqlquery) |
| credential | ParsedCredential | Parsed Credential | M | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| Set\<String\> | Set of matched claim names | M | |

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
`A functional interface for signing operations. Used when signing Authorization Requests.`

### Usage
```java
CompactSigner signer = (keyId, hash) -> {
    // Sign hash with private key corresponding to keyId
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
`Verification session storage interface. A default InMemory implementation is provided, and custom implementations (Redis, DB, etc.) are possible.`

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
`An adapter interface for processing various Credential formats (SD-JWT, mDoc, etc.).`

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
| `success(T data)` | Create success result (HTTP 200) |
| `success(T data, String contentType)` | Create success result (custom Content-Type) |
| `badRequest(String errorCode, String errorDescription)` | Create 400 error result |
| `notFound(String errorCode, String errorDescription)` | Create 404 error result |
| `serverError(String errorCode, String errorDescription)` | Create 500 error result |

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
| transactionId | String | Transaction identifier | M |
| state | String | State parameter | M |
| nonce | String | Nonce value | M |
| dcqlQuery | String | DCQL query JSON | M |
| responseMode | String | Response mode | M |
| requestId | String | Request identifier | M |
| status | String | Session status (CREATED, REQUEST_FETCHED, COMPLETED, EXPIRED) | M |
| clientMetadata | String | Merged client metadata JSON | O |

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

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| issuerPublicKey | String | Base64 encoded issuer compressed public key | M | |
| holderPublicKey | String | Base64 encoded holder compressed public key | O | Extracted from cnf.jwk if null |
| clientId | String | Verifier's Client ID | M | For aud claim verification |
| nonce | String | Expected Nonce value | M | |

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
| `getClaim(String claimName)` | Object | Retrieve specific claim value |
| `hasClaim(String claimName)` | boolean | Check claim existence |
| `getMetadataValue(String key)` | Object | Retrieve metadata value |
| `getNativeCredentialAs(Class<T> type)` | T | Cast native Credential |

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