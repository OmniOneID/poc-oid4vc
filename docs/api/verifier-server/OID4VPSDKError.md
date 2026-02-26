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

OID4VP SDK Error
==

- Subject: OID4VPSDKError
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
    - [1. Authorization (01xxx)](#1-authorization-01xxx)
    - [2. Initiation (02xxx)](#2-initiation-02xxx)
    - [3. VP Token Processing (03xxx)](#3-vp-token-processing-03xxx)
    - [4. JWS/JWT (04xxx)](#4-jwsjwt-04xxx)
    - [5. Configuration (05xxx)](#5-configuration-05xxx)
    - [6. Scope Mapping (06xxx)](#6-scope-mapping-06xxx)
    - [7. Crypto (07xxx)](#7-crypto-07xxx)
    - [8. DCQL (08xxx)](#8-dcql-08xxx)
    - [9. General (99xxx)](#9-general-99xxx)

# Model
## Error Response

### Description
```
Error struct for OID4VP SDK. It has code and message pair.
Code starts with SSOID4VP.
```

### Declaration
```java
public class OID4VPException extends Exception {
    protected final String errorCode;
    protected final String errorMsg;
    protected final String errorReason;
}
```

### Property

| Name        | Type   | Description                            | **M/O** | **Note** |
|-------------|--------|----------------------------------------|---------|----------|
| errorCode   | String | Error code. It starts with SSOID4VP    | M       |          |
| errorMsg    | String | Error message                          | M       |          |
| errorReason | String | Additional error reason/details        | O       |          |

<br>

# Error Code
## 1. Authorization (01xxx)

| Error Code    | Error Message                          | Description | Action Required                                      |
|---------------|----------------------------------------|-------------|------------------------------------------------------|
| SSOID4VP01000 | Invalid or expired request_uri         | -           | Check if the request_uri is valid and not expired.   |
| SSOID4VP01001 | Request URI already used               | -           | Generate a new request_uri for the verification.     |
| SSOID4VP01002 | Session has expired                    | -           | Initiate a new verification session.                 |
| SSOID4VP01003 | Invalid session state                  | -           | Check session state or start a new session.          |
| SSOID4VP01004 | CompactSigner is required              | -           | Provide a valid CompactSigner implementation.        |
| SSOID4VP01005 | Failed to sign authorization request   | -           | Check signer configuration and key availability.     |
| SSOID4VP01006 | Session status is not COMPLETED        | -           | Wait for session completion or check session status. |

<br>

## 2. Initiation (02xxx)

| Error Code    | Error Message                              | Description | Action Required                                      |
|---------------|--------------------------------------------|-------------|------------------------------------------------------|
| SSOID4VP02000 | Either dcql_query or scope is required     | -           | Provide either dcql_query or scope parameter.        |
| SSOID4VP02001 | Cannot specify both dcql_query and scope   | -           | Use only one of dcql_query or scope parameter.       |
| SSOID4VP02002 | No DCQL mapping found for scope            | -           | Register DCQL mapping for the requested scope.       |
| SSOID4VP02003 | Invalid DCQL JSON format                   | -           | Verify the DCQL query JSON syntax.                   |
| SSOID4VP02004 | DCQL query validation failed               | -           | Check DCQL query structure and required fields.      |

<br>

## 3. VP Token Processing (03xxx)

| Error Code    | Error Message                          | Description | Action Required                                          |
|---------------|----------------------------------------|-------------|----------------------------------------------------------|
| SSOID4VP03000 | VP Token cannot be null or empty       | -           | Provide a valid VP Token.                                |
| SSOID4VP03001 | Failed to parse VP Token               | -           | Check VP Token format and structure.                     |
| SSOID4VP03002 | Invalid credential format              | -           | Verify the credential format is supported.               |
| SSOID4VP03003 | No suitable verifier found             | -           | Register a VPTokenVerifier for the credential format.    |
| SSOID4VP03004 | VP verification failed                 | -           | Check credential signatures and public keys.             |
| SSOID4VP03005 | Insufficient public keys provided      | -           | Provide all required issuer/holder public keys.          |
| SSOID4VP03006 | DCQL ID not found in query             | -           | Ensure VP Token contains credentials matching DCQL.      |
| SSOID4VP03007 | DCQL Query has no credentials defined  | -           | Add credential definitions to the DCQL query.            |
| SSOID4VP03008 | Credential format mismatch             | -           | Verify credential format matches DCQL query.             |
| SSOID4VP03009 | Credential count mismatch              | -           | Ensure VP Token contains expected number of credentials. |
| SSOID4VP03010 | Credential meta condition not satisfied| -           | Verify credential meta matches DCQL query.               |

<br>

## 4. JWS/JWT (04xxx)

| Error Code    | Error Message                          | Description | Action Required                                      |
|---------------|----------------------------------------|-------------|------------------------------------------------------|
| SSOID4VP04000 | Failed to sign JWT                     | -           | Check signer and key configuration.                  |
| SSOID4VP04001 | JWT has not been signed                | -           | Sign the JWT before serialization.                   |
| SSOID4VP04002 | Failed to serialize JWT                | -           | Verify JWT structure and content.                    |
| SSOID4VP04003 | Invalid signature format               | -           | Check signature encoding and format.                 |
| SSOID4VP04004 | Unsupported key algorithm              | -           | Use a supported algorithm (e.g., ES256).             |
| SSOID4VP04005 | Invalid key                            | -           | Verify key format and validity.                      |
| SSOID4VP04006 | Failed to convert DER to P1363         | -           | Check signature data format.                         |

<br>

## 5. Configuration (05xxx)

| Error Code    | Error Message                          | Description | Action Required                                      |
|---------------|----------------------------------------|-------------|------------------------------------------------------|
| SSOID4VP05000 | Failed to load configuration           | -           | Check database connection and config table.          |
| SSOID4VP05001 | Failed to save configuration           | -           | Verify database write permissions.                   |
| SSOID4VP05002 | Configuration not found                | -           | Initialize OID4VP configuration in database.         |
| SSOID4VP05003 | Configuration already exists           | -           | Use update instead of create for existing config.    |

<br>

## 6. Scope Mapping (06xxx)

| Error Code    | Error Message                          | Description | Action Required                                      |
|---------------|----------------------------------------|-------------|------------------------------------------------------|
| SSOID4VP06000 | Failed to load scope mappings          | -           | Check database connection and scope mapping table.   |
| SSOID4VP06001 | Failed to save scope mapping           | -           | Verify database write permissions.                   |
| SSOID4VP06002 | Failed to delete scope mapping         | -           | Check if scope mapping exists.                       |
| SSOID4VP06003 | Scope mapping not found                | -           | Register the requested scope mapping.                |
| SSOID4VP06004 | Duplicate credential ID                | -           | Use unique credential IDs in DCQL query.             |
| SSOID4VP06005 | Duplicate claim ID                     | -           | Use unique claim IDs in DCQL query.                  |
| SSOID4VP06006 | Failed to update scope mapping         | -           | Check scope mapping data and retry.                  |
| SSOID4VP06007 | Scope already exists                   | -           | Use update instead of create for existing scope.     |

<br>

## 7. Crypto (07xxx)

| Error Code    | Error Message                          | Description | Action Required                                      |
|---------------|----------------------------------------|-------------|------------------------------------------------------|
| SSOID4VP07000 | Encryption key not configured          | -           | Set vpTokenEncryptionKey in OID4VP configuration.    |
| SSOID4VP07001 | Invalid encryption key length          | -           | Use a 32-byte Base64-encoded key for AES-256.        |
| SSOID4VP07002 | Failed to encrypt                      | -           | Check encryption key and input data.                 |
| SSOID4VP07003 | Failed to decrypt                      | -           | Verify encryption key matches and data is valid.     |
| SSOID4VP07004 | Failed to initialize encryptor         | -           | Check encryption key configuration.                  |
| SSOID4VP07005 | Cryptographic algorithm not available  | -           | Install required cryptographic provider.             |
| SSOID4VP07006 | Failed to decode data                  | -           | Check data encoding format (Base64, etc.).           |
| SSOID4VP07007 | Unsupported format                     | -           | Use a supported data format.                         |

<br>

## 8. DCQL (08xxx)

| Error Code    | Error Message                              | Description | Action Required                                      |
|---------------|--------------------------------------------|-------------|------------------------------------------------------|
| SSOID4VP08000 | Failed to parse DCQL                       | -           | Verify DCQL JSON syntax and structure.               |
| SSOID4VP08001 | No adapter found for credential format     | -           | Register adapter for the credential format.          |

<br>

## 9. General (99xxx)

| Error Code    | Error Message                          | Description | Action Required                                      |
|---------------|----------------------------------------|-------------|------------------------------------------------------|
| SSOID4VP99000 | Invalid parameter                      | -           | Check parameter value and format.                    |
| SSOID4VP99001 | Parameter cannot be null               | -           | Provide a non-null value for the parameter.          |
| SSOID4VP99002 | Parameter cannot be empty              | -           | Provide a non-empty value for the parameter.         |
| SSOID4VP99999 | Unexpected error occurred              | -           | Check logs for detailed error information.           |

<br>