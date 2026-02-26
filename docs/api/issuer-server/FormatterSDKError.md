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

OID4VC Formatter SDK Error
==

- Subject: FormatterSDKError
- Author: Suhyun Forten Lee
- Date: 2025-01-30
- Version: v1.0.0

| Version  | Date       | Changes         |
| -------- |------------| --------------- |
| v1.0.0   | 2025-01-30 | Initial version |

<div style="page-break-after: always;"></div>

# Table of Contents
- [Model](#model)
    - [Error Response](#error-response)
- [Error Code](#error-code)
    - [1. VC Generation (01xxx)](#1-vc-generation-01xxx)
    - [2. VP Token Processing (02xxx)](#2-vp-token-processing-02xxx)
    - [3. Crypto/Key (03xxx)](#3-cryptokey-03xxx)
    - [4. General (99xxx)](#4-general-99xxx)

# Model
## Error Response

### Description
```
Error struct for OID4VC Formatter SDK. It has code and message pair.
Code starts with SSFMT.
```

### Declaration
```java
public class FormatterException extends Exception {
    protected final String errorCode;
    protected final String errorMsg;
    protected final String errorReason;
}
```

### Property

| Name        | Type   | Description                            | **M/O** | **Note** |
|-------------|--------|----------------------------------------|---------|----------|
| errorCode   | String | Error code. It starts with SSFMT      | M       |          |
| errorMsg    | String | Error message                          | M       |          |
| errorReason | String | Additional error reason/details        | O       |          |

<br>

# Error Code
## 1. VC Generation (01xxx)

| Error Code  | Error Message                                    | Description | Action Required                                      |
|-------------|--------------------------------------------------|-------------|------------------------------------------------------|
| SSFMT01000  | VC generation failed                             | -           | Check VC generation parameters and issuer configuration. |
| SSFMT01001  | Invalid parameter provided for VC generation     | -           | Verify all required parameters are provided correctly. |
| SSFMT01002  | Unsupported VC format                            | -           | Use a supported VC format.                           |

<br>

## 2. VP Token Processing (02xxx)

| Error Code  | Error Message                          | Description | Action Required                                     |
|-------------|----------------------------------------|-------------|-----------------------------------------------------|
| SSFMT02000  | VP Token cannot be null or empty       | -           | Provide a valid VP Token.                           |
| SSFMT02001  | Failed to parse VP Token               | -           | Check VP Token format and structure.                |
| SSFMT02002  | Invalid credential format              | -           | Verify the credential format is supported.          |
| SSFMT02003  | No suitable verifier found             | -           | Register a VPTokenVerifier for the credential format. |
| SSFMT02004  | VP verification failed                 | -           | Check credential signatures and public keys.        |
| SSFMT02005  | X.509 certificate chain validation is not supported for this format | -           | Verify the credential format is supported.          |

<br>

## 3. Crypto/Key (03xxx)

| Error Code  | Error Message                          | Description | Action Required                                      |
|-------------|----------------------------------------|-------------|------------------------------------------------------|
| SSFMT03000  | Cryptographic algorithm not available  | -           | Install required cryptographic provider (e.g., BouncyCastle). |
| SSFMT03001  | Invalid key                            | -           | Verify key format and validity.                      |
| SSFMT03002  | Failed to decode data                  | -           | Check data encoding format (Base64, etc.).           |

<br>

## 4. General (99xxx)

| Error Code  | Error Message                          | Description | Action Required                                      |
|-------------|----------------------------------------|-------------|------------------------------------------------------|
| SSFMT99000  | Invalid parameter                      | -           | Check parameter value and format.                    |
| SSFMT99001  | Parameter cannot be null               | -           | Provide a non-null value for the parameter.          |
| SSFMT99999  | Unexpected error occurred              | -           | Check logs for detailed error information.           |

<br>
