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

- Subject: SD-JWT VC SDK Android API
- Author: Open Source Development Team
- Date: 2026-03-31
- Version: v1.0.0

| Version | Date       | Changes         |
| ------- | ---------- | --------------- |
| v1.0.0  | 2026-03-31 | Initial version |

<div style="page-break-after: always;"></div>

# Table of Contents
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

## Overview

This document defines the SDK API for using **SD-JWT VC (Selective Disclosure for JWTs - Verifiable Credentials)** on Android.
It provides SD-JWT VC issuance, verification, Selective Disclosure processing, and OID4VP presentation capabilities.

### Key Features
- SD-JWT VC creation (Fluent Builder pattern)
- SD-JWT VC verification (signature verification, Key Binding verification)
- Selective Disclosure processing
- OID4VP VP Token generation

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
`Adds a regular (non-selectively-disclosable) claim to the SD-JWT payload.`

### Input Parameters

| Parameter | Type   | Description | **M/O** | **Note** |
|-----------|--------|-------------|---------|----------|
| name      | String | Claim name  | M       |          |
| value     | Object | Claim value | M       |          |

### Output Parameters

| Type         | Description              | **M/O** | **Note**   |
|--------------|--------------------------|---------|------------|
| SDJWTBuilder | Builder instance (chain) | M       | Fluent API |

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
`Adds a selectively disclosable claim. The claim will be separated into a Disclosure.`

### Input Parameters

| Parameter | Type   | Description | **M/O** | **Note**                      |
|-----------|--------|-------------|---------|-------------------------------|
| name      | String | Claim name  | M       |                               |
| value     | Object | Claim value | M       |                               |
| salt      | String | Salt value  | O       | Auto-generated if not provided |

### Output Parameters

| Type         | Description              | **M/O** | **Note**   |
|--------------|--------------------------|---------|------------|
| SDJWTBuilder | Builder instance (chain) | M       | Fluent API |

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
`Sets the vct (Verifiable Credential Type) claim for the SD-JWT VC.`

### Input Parameters

| Parameter | Type   | Description                | **M/O** | **Note** |
|-----------|--------|----------------------------|---------|----------|
| vct       | String | Verifiable Credential Type | M       |          |

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
`Sets the cnf (confirmation) claim for the Holder's Key Binding.`

### Input Parameters

| Parameter | Type                  | Description               | **M/O** | **Note**         |
|-----------|-----------------------|---------------------------|---------|------------------|
| cnf       | Map\<String, Object\> | cnf claim (includes JWK)  | M       | Contains jwk key |

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
`Builds the SD-JWT based on the configured claims and Disclosures.`

### Input Parameters

| Parameter | Type                       | Description          | **M/O** | **Note**             |
|-----------|----------------------------|----------------------|---------|----------------------|
| jwtSigner | Function\<String, String\> | JWT signing function | M       | signingInput -> JWT  |

### Output Parameters

| Type  | Description    | **M/O** | **Note**             |
|-------|----------------|---------|----------------------|
| SDJWT | SD-JWT object  | M       | [Link](#21-sdjwt)    |

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
    .selectivelyDisclosableClaim("name", "John Doe")
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
`Verifies the SD-JWT signature, processes Disclosures, and returns claims. Optionally performs Key Binding verification.`

### Input Parameters

| Parameter        | Type   | Description            | **M/O** | **Note**                          |
|------------------|--------|------------------------|---------|-----------------------------------|
| sdJwtString      | String | SD-JWT string          | M       |                                   |
| expectedAudience | String | Expected audience (aud) | O      | Required for Key Binding verification |
| expectedNonce    | String | Expected nonce         | O       | Required for Key Binding verification |

### Output Parameters

| Type           | Description         | **M/O** | **Note**                    |
|----------------|---------------------|---------|-----------------------------|
| SDJWTClaimsSet | Verified claims set | M       | [Link](#24-sdjwtclaimsset)  |

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
`Verifies the SD-JWT using the certificate chain included in the x5c header.`

### Input Parameters

| Parameter        | Type                    | Description                  | **M/O** | **Note**                          |
|------------------|-------------------------|------------------------------|---------|-----------------------------------|
| sdJwtString      | String                  | SD-JWT string                | M       |                                   |
| trustedRootCerts | List\<X509Certificate\> | Trusted root certificate list | O      | Chain-only verification if null   |
| expectedAudience | String                  | Expected audience            | O       |                                   |
| expectedNonce    | String                  | Expected nonce               | O       |                                   |

### Output Parameters

| Type           | Description         | **M/O** | **Note**                    |
|----------------|---------------------|---------|-----------------------------|
| SDJWTClaimsSet | Verified claims set | M       | [Link](#24-sdjwtclaimsset)  |

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
`Creates a Key Binding JWT for the Holder. Used for Holder authentication when submitting an SD-JWT.`

### Input Parameters

| Parameter      | Type           | Description                    | **M/O** | **Note**                     |
|----------------|----------------|--------------------------------|---------|------------------------------|
| holderKey      | PrivateKey     | Holder private key             | M       |                              |
| audience       | String         | Verifier's Client ID           | M       | aud claim                    |
| nonce          | String         | Nonce value                    | M       |                              |
| sdJwtString    | String         | SD-JWT string (for sd_hash)    | O       | Generates sd_hash if provided |
| holderX5cChain | List\<String\> | Holder x5c certificate chain   | O       |                              |

### Output Parameters

| Type   | Description              | **M/O** | **Note** |
|--------|--------------------------|---------|----------|
| String | Key Binding JWT string   | M       |          |

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
`Creates a VP Token for the OID4VP protocol. Only the requested claims are selectively disclosed.`

### Input Parameters

| Parameter        | Type           | Description                    | **M/O** | **Note**          |
|------------------|----------------|--------------------------------|---------|-------------------|
| sdJwtVC          | String         | Original SD-JWT VC string      | M       |                   |
| requestedClaims  | Set\<String\>  | Set of requested claim names   | M       |                   |
| holderPrivateKey | PrivateKey     | Holder private key             | M       | For Key Binding   |
| audience         | String         | Verifier Client ID             | M       | aud claim         |
| nonce            | String         | Nonce value                    | M       |                   |
| holderX5cChain   | List\<String\> | Holder x5c certificate chain   | O       |                   |

### Output Parameters

| Type   | Description     | **M/O** | **Note**                                    |
|--------|-----------------|---------|---------------------------------------------|
| String | VP Token string | M       | Selectively disclosed SD-JWT + Key Binding JWT |

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
`Creates a VP Token with a DCQL ID. Used when responding to DCQL query-based verification requests.`

### Input Parameters

| Parameter        | Type           | Description                    | **M/O** | **Note** |
|------------------|----------------|--------------------------------|---------|----------|
| sdJwtVC          | String         | Original SD-JWT VC string      | M       |          |
| requestedClaims  | Set\<String\>  | Set of requested claim names   | M       |          |
| dcqlId           | String         | DCQL Credential ID             | M       |          |
| holderPrivateKey | PrivateKey     | Holder private key             | M       |          |
| audience         | String         | Verifier Client ID             | M       |          |
| nonce            | String         | Nonce value                    | M       |          |
| holderX5cChain   | List\<String\> | Holder x5c certificate chain   | O       |          |

### Output Parameters

| Type   | Description     | **M/O** | **Note** |
|--------|-----------------|---------|----------|
| String | VP Token string | M       |          |

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
`Parses an SD-JWT string into an SDJWT object.`

### Input Parameters

| Parameter   | Type   | Description   | **M/O** | **Note** |
|-------------|--------|---------------|---------|----------|
| sdJwtString | String | SD-JWT string | M       |          |

### Output Parameters

| Type  | Description  | **M/O** | **Note**           |
|-------|--------------|---------|--------------------|
| SDJWT | SDJWT object | M       | [Link](#21-sdjwt)  |

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

| Parameter     | Type               | Description     | **M/O** |
|---------------|--------------------|-----------------|---------|
| credentialJwt | String             | Issuer-signed JWT | M     |
| disclosures   | List\<Disclosure\> | List of Disclosures | M   |
| keyBindingJwt | String             | Key Binding JWT | O       |

### Methods

| Method                 | Return Type         | Description                      |
|------------------------|---------------------|----------------------------------|
| `getCredentialJwt()`   | String              | Returns the credential JWT       |
| `getDisclosures()`     | List\<Disclosure\>  | Returns the list of Disclosures  |
| `getKeyBindingJwt()`   | String              | Returns the Key Binding JWT      |
| `hasKeyBindingJwt()`   | boolean             | Whether Key Binding JWT exists   |
| `getDisclosureCount()` | int                 | Returns the number of Disclosures |
| `toString()`           | String              | Returns the serialized SD-JWT string |

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

| Parameter  | Type   | Description | **M/O** | **Note**                   |
|------------|--------|-------------|---------|----------------------------|
| salt       | String | Salt value  | M       |                            |
| claimName  | String | Claim name  | O       | null for array elements    |
| claimValue | Object | Claim value | M       |                            |

### Methods

| Method                     | Return Type | Description                             |
|----------------------------|-------------|-----------------------------------------|
| `getSalt()`                | String      | Returns the salt                        |
| `getClaimName()`           | String      | Returns the claim name                  |
| `getClaimValue()`          | Object      | Returns the claim value                 |
| `isArrayElement()`         | boolean     | Whether this is an array element        |
| `getDisclosure()`          | String      | Returns Base64URL-encoded string        |
| `digest()`                 | String      | Returns SHA-256 hash digest             |
| `digest(String algorithm)` | String      | Returns hash digest with given algorithm |

### Static Factory Methods

| Method                                         | Description                         |
|------------------------------------------------|-------------------------------------|
| `forObjectProperty(String name, Object value)` | Creates a Disclosure for an object property |
| `parse(String disclosureString)`               | Parses from a Base64URL string      |

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

| Parameter    | Type                           | Description                           | **M/O** |
|--------------|--------------------------------|---------------------------------------|---------|
| sdFieldNames | List\<String\>                 | Field names for selective disclosure  | M       |
| nestedFrames | Map\<String, DisclosureFrame\> | Nested Disclosure Frames              | O       |

### Methods

| Method                                              | Return Type     | Description                              |
|-----------------------------------------------------|-----------------|------------------------------------------|
| `addSdField(String fieldName)`                      | DisclosureFrame | Adds a selectively disclosable field     |
| `addNestedFrame(String name, DisclosureFrame frame)` | DisclosureFrame | Adds a nested frame                     |
| `isSdField(String fieldName)`                       | boolean         | Whether the field is selectively disclosable |
| `validate(Map<String, Object> claims)`              | void            | Validates the frame against claims       |
| `fromJson(String jsonString)`                       | DisclosureFrame | Creates a DisclosureFrame from JSON (static) |

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

| Parameter   | Type                  | Description               | **M/O** |
|-------------|-----------------------|---------------------------|---------|
| claims      | Map\<String, Object\> | Verified claims map       | M       |
| disclosures | List\<Disclosure\>    | Verified Disclosure list  | M       |

### Methods

| Method                         | Return Type           | Description                   |
|--------------------------------|-----------------------|-------------------------------|
| `getClaims()`                  | Map\<String, Object\> | Returns the full claims map  |
| `getClaim(String name)`        | Object                | Returns a specific claim value |
| `getStringClaim(String name)`  | String                | Returns a String claim        |
| `getLongClaim(String name)`    | Long                  | Returns a Long claim          |
| `getBooleanClaim(String name)` | Boolean               | Returns a Boolean claim       |
| `getClaimNames()`             | Set\<String\>          | Returns the set of claim names |
| `hasClaim(String name)`       | boolean                | Whether the claim exists      |

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

| Parameter | Type   | Description              | **M/O** |
|-----------|--------|--------------------------|---------|
| header    | String | Base64URL-encoded header | M       |
| payload   | String | Base64URL-encoded payload | M      |
| signature | String | Base64URL-encoded signature | O    |

### Methods

| Method                           | Return Type           | Description                      |
|----------------------------------|-----------------------|----------------------------------|
| `getJWTClaimsSet()`             | Map\<String, Object\> | Returns the payload claims map   |
| `getHeader()`                    | Map\<String, Object\> | Returns the header map          |
| `getSigningInput()`             | String                 | Returns the signing input string |
| `getSignatureBytes()`           | byte[]                 | Returns the signature bytes     |
| `sign(JWSSigner signer)`        | void                   | Performs JWT signing            |
| `serialize()`                    | String                 | Returns the serialized JWT string |
| `verify(JWSVerifier verifier)`   | boolean                | Verifies the JWT signature      |
| `parse(String jwt)`             | SignedJWT              | Parses a JWT string (static)    |

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
`Interface for JWT signing. ECDSASigner and RSASSASigner implementations are provided.`

### Implementations

| Class        | Description                    |
|--------------|--------------------------------|
| ECDSASigner  | ECDSA (ES256) signing implementation |
| RSASSASigner | RSA (RS256) signing implementation   |

<br>

## 3.2 JWSVerifier

### Declaration

```java
public interface JWSVerifier {
    boolean verify(SignedJWT signedJWT) throws SDJWTException;
}
```

### Description
`Interface for JWT signature verification. ECDSAVerifier and RSASSAVerifier implementations are provided.`

### Implementations

| Class          | Description                          |
|----------------|--------------------------------------|
| ECDSAVerifier  | ECDSA (ES256) verification implementation |
| RSASSAVerifier | RSA (RS256) verification implementation   |

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
`Enum for SDK error codes. Codes start with the MSDKSDJ prefix.`

### Methods

| Method                   | Return Type    | Description                    |
|--------------------------|----------------|--------------------------------|
| `getCode()`              | String         | Returns the error code string  |
| `getMsg()`               | String         | Returns the error message      |
| `getByCode(String code)` | SDJWTErrorCode | Finds error code by code string |

> For the full list of error codes, refer to the [SDJWTVCSDKError](SDJWTVCSDKError.md) document.

<br>
