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

SD-JWT VC SDK Error
==

- Subject: SDJWTVCSDKError
- Author: 오픈소스개발팀
- Date: 2026-03-31
- Version: v1.0.0

| Version  | Date       | Changes         |
| -------- |------------| --------------- |
| v1.0.0   | 2026-03-31 | Initial version |

<div style="page-break-after: always;"></div>

# Table of Contents
- [Model](#model)
    - [Error Response](#error-response)
- [Error Code](#error-code)
    - [1. General (00xxx)](#1-general-00xxx)
    - [2. Crypto (01xxx)](#2-crypto-01xxx)
    - [3. JWT (02xxx)](#3-jwt-02xxx)
    - [4. SD-JWT (03xxx)](#4-sd-jwt-03xxx)
    - [5. Disclosure (04xxx)](#5-disclosure-04xxx)
    - [6. Disclosure Frame (05xxx)](#6-disclosure-frame-05xxx)
    - [7. Key Binding (06xxx)](#7-key-binding-06xxx)
    - [8. JSON Processing (07xxx)](#8-json-processing-07xxx)
    - [9. Encoding (08xxx)](#9-encoding-08xxx)
    - [10. Hash (09xxx)](#10-hash-09xxx)
    - [11. Salt (10xxx)](#11-salt-10xxx)
    - [12. Validation (11xxx)](#12-validation-11xxx)
    - [13. OID4VCI (12xxx)](#13-oid4vci-12xxx)

# Model
## Error Response

### Description
```
Error class for SD-JWT VC SDK. It has code and message pair.
Code starts with MSDKSDJ.
```

### Declaration
```java
public class SDJWTException extends RuntimeException {
    // message is inherited from RuntimeException
}
```

### Property

| Name    | Type   | Description                              | **M/O** | **Note**                  |
|---------|--------|------------------------------------------|---------|---------------------------|
| message | String | Error message (inherited from Exception) | M       | Includes error code info  |

<br>

# Error Code
## 1. General (00xxx)

| Error Code     | Error Message              | Description | Action Required                          |
|----------------|----------------------------|-------------|------------------------------------------|
| MSDKSDJ00001   | Invalid parameter          | -           | Check parameter value and format.        |
| MSDKSDJ00002   | Parameter cannot be null   | -           | Provide a non-null value.                |
| MSDKSDJ00003   | Parameter cannot be empty  | -           | Provide a non-empty value.               |
| MSDKSDJ00004   | Unsupported operation      | -           | Check if the operation is supported.     |

<br>

## 2. Crypto (01xxx)

| Error Code     | Error Message                      | Description | Action Required                                  |
|----------------|------------------------------------|-------------|--------------------------------------------------|
| MSDKSDJ01001   | Invalid key                        | -           | Verify key format and validity.                  |
| MSDKSDJ01002   | Signature operation failed         | -           | Check signer and key configuration.              |
| MSDKSDJ01003   | Signature verification failed      | -           | Check public key and signature data.             |
| MSDKSDJ01004   | Unsupported algorithm              | -           | Use a supported algorithm (e.g., ES256).         |
| MSDKSDJ01005   | Unsupported key type               | -           | Use a supported key type (EC, RSA).              |
| MSDKSDJ01006   | Algorithm not available            | -           | Install required cryptographic provider.         |
| MSDKSDJ01007   | Invalid signature format           | -           | Check signature encoding and format.             |

<br>

## 3. JWT (02xxx)

| Error Code     | Error Message              | Description | Action Required                          |
|----------------|----------------------------|-------------|------------------------------------------|
| MSDKSDJ02001   | Invalid JWT format         | -           | Check JWT structure (header.payload.signature). |
| MSDKSDJ02002   | JWT parse failed           | -           | Verify JWT string is valid Base64URL.    |
| MSDKSDJ02003   | JWT header missing         | -           | Provide a valid JWT header.              |
| MSDKSDJ02004   | JWT payload missing        | -           | Provide a valid JWT payload.             |
| MSDKSDJ02005   | JWT sign failed            | -           | Check signer and key configuration.      |

<br>

## 4. SD-JWT (03xxx)

| Error Code     | Error Message              | Description | Action Required                          |
|----------------|----------------------------|-------------|------------------------------------------|
| MSDKSDJ03001   | Invalid SD-JWT format      | -           | Check SD-JWT structure.                  |
| MSDKSDJ03002   | SD-JWT parse failed        | -           | Verify SD-JWT string format.             |
| MSDKSDJ03003   | SD-JWT build failed        | -           | Check claims and disclosure configuration. |

<br>

## 5. Disclosure (04xxx)

| Error Code     | Error Message                          | Description | Action Required                              |
|----------------|----------------------------------------|-------------|----------------------------------------------|
| MSDKSDJ04001   | Invalid disclosure format              | -           | Check disclosure Base64URL encoding.         |
| MSDKSDJ04002   | Disclosure parse failed                | -           | Verify disclosure JSON structure.            |
| MSDKSDJ04003   | Disclosure create failed               | -           | Check salt, claim name, and value.           |
| MSDKSDJ04004   | Disclosure hash mismatch               | -           | Verify hash algorithm and disclosure data.   |
| MSDKSDJ04005   | Invalid disclosure salt                | -           | Provide a valid salt value.                  |
| MSDKSDJ04006   | Invalid disclosure claim name          | -           | Use a valid, non-reserved claim name.        |
| MSDKSDJ04007   | Array element disclosure not supported in this context | -  | Use object property disclosure instead. |

<br>

## 6. Disclosure Frame (05xxx)

| Error Code     | Error Message                          | Description | Action Required                              |
|----------------|----------------------------------------|-------------|----------------------------------------------|
| MSDKSDJ05001   | Invalid disclosure frame format        | -           | Check disclosure frame structure.            |
| MSDKSDJ05002   | Disclosure frame parse failed          | -           | Verify frame JSON format.                    |
| MSDKSDJ05003   | Invalid field in disclosure frame      | -           | Check field name in disclosure frame.        |
| MSDKSDJ05004   | Reserved field name in disclosure frame | -          | Do not use reserved fields (_sd, _sd_alg).   |
| MSDKSDJ05005   | Disclosure frame references non-existent claim | -   | Ensure claim exists in the payload.          |
| MSDKSDJ05006   | Invalid nested type in disclosure frame | -          | Use correct nested disclosure frame type.    |

<br>

## 7. Key Binding (06xxx)

| Error Code     | Error Message                  | Description | Action Required                          |
|----------------|--------------------------------|-------------|------------------------------------------|
| MSDKSDJ06001   | Invalid key binding JWT format | -           | Check Key Binding JWT structure.         |
| MSDKSDJ06002   | Key binding JWT build failed   | -           | Check holder key and claims.             |
| MSDKSDJ06003   | Invalid holder key             | -           | Verify holder private key.               |

<br>

## 8. JSON Processing (07xxx)

| Error Code     | Error Message                  | Description | Action Required                          |
|----------------|--------------------------------|-------------|------------------------------------------|
| MSDKSDJ07001   | JSON processing failed         | -           | Check JSON data structure.               |
| MSDKSDJ07002   | JSON mapping failed            | -           | Verify JSON field types and names.       |
| MSDKSDJ07003   | JSON serialization failed      | -           | Check object for serialization issues.   |
| MSDKSDJ07004   | JSON deserialization failed    | -           | Verify JSON string format.               |

<br>

## 9. Encoding (08xxx)

| Error Code     | Error Message                  | Description | Action Required                          |
|----------------|--------------------------------|-------------|------------------------------------------|
| MSDKSDJ08001   | Base64 encoding/decoding failed | -          | Check input data for encoding.           |
| MSDKSDJ08002   | Unsupported encoding           | -           | Use a supported encoding format.         |
| MSDKSDJ08003   | Invalid encoding format        | -           | Verify encoding format.                  |

<br>

## 10. Hash (09xxx)

| Error Code     | Error Message                      | Description | Action Required                              |
|----------------|------------------------------------|-------------|----------------------------------------------|
| MSDKSDJ09001   | Hash algorithm not available       | -           | Install required hash algorithm provider.    |
| MSDKSDJ09002   | Unsupported hash algorithm         | -           | Use sha-256, sha-384, or sha-512.            |
| MSDKSDJ09003   | Hash computation failed            | -           | Check input data and algorithm.              |

<br>

## 11. Salt (10xxx)

| Error Code     | Error Message                  | Description | Action Required                          |
|----------------|--------------------------------|-------------|------------------------------------------|
| MSDKSDJ10001   | Invalid salt length            | -           | Use salt length between 8 and 64 bytes.  |
| MSDKSDJ10002   | Salt generation failed         | -           | Check SecureRandom availability.         |

<br>

## 12. Validation (11xxx)

| Error Code     | Error Message                  | Description | Action Required                          |
|----------------|--------------------------------|-------------|------------------------------------------|
| MSDKSDJ11001   | Validation failed              | -           | Check SD-JWT structure and disclosures.  |
| MSDKSDJ11002   | Required claim not found       | -           | Verify claim exists in the credential.   |

<br>

## 13. OID4VCI (12xxx)

| Error Code     | Error Message                  | Description | Action Required                          |
|----------------|--------------------------------|-------------|------------------------------------------|
| MSDKSDJ12001   | Credential issuance failed     | -           | Check issuance configuration.            |
| MSDKSDJ12002   | Credential signing failed      | -           | Verify signing key and algorithm.        |

<br>
