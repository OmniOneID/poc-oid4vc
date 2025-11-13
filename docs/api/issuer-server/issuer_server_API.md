# OID4VCI Issuer API Document

- Subject: OID4VCI Issuer API Document
- Writer: Sangjun Kim
- Date: 2025-10-23
- Version: v1.0.0

| Version | Date | History |
| --- | --- | --- |
| v1.0.0 | 2025-10-23 | Initial draft |

## Table of Contents

1. [Overview](#1-overview)
2. [API List](#2-api-list)
3. [API Detailed Description](#3-api-detailed-description)
    - [3.1. Create Credential Offer](#31-create-credential-offer)
    - [3.2. Get Issuer Metadata](#32-get-issuer-metadata)
    - [3.3. Issue Access Token](#33-issue-access-token)
    - [3.4. Issue Credential](#34-issue-credential)
    - [3.5. Get Deferred Credential](#35-get-deferred-credential)
    - [3.6. Credential Status Notification](#36-credential-status-notification)
    - [3.7. Issue Nonce](#37-issue-nonce)
    - [3.8. Get Credential Identifier](#38-get-credential-identifier)
4. [Error Codes](#4-error-codes)
5. [Test API](#5-test-api)

---

## 1. Overview

This document defines the API for an Issuer service that complies with the [OpenID for Verifiable Credential Issuance (OID4VCI)](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html) standard. The Issuer is the entity that creates and issues credentials.

---

## 2. API List

| API | Method | URL | Description |
| --- | --- | --- | --- |
| `Create Credential Offer` | GET | `/credential-offer/{request_id}` | Creates and returns a Credential Offer. |
| `Get Issuer Metadata` | GET | `/.well-known/openid-credential-issuer` | Retrieves the Issuer's configuration information (Metadata). |
| `Issue Access Token` | POST | `/token` | Exchanges a Pre-Authorized Code for an Access Token (when not using an authorization server). |
| `Issue Credential` | POST | `/credential` | Issues a credential using an Access Token. |
| `Get Deferred Credential`| POST | `/deferred_credential` | In the case of deferred credential issuance, retrieves the credential using a transaction ID. |
| `Credential Status Notification` | POST | `/notification` | Notifies the Issuer of the credential's storage status (success/failure) from the Wallet. |
| `Issue Nonce` | POST | `/nonce` | Issues a Nonce value to be used in a credential issuance request. |
| `Get Credential Identifier` | GET | `/get-credential-identifier` | Retrieves a list of Credential Identifiers corresponding to a `credentialConfigurationId`. |

---

## 3. API Detailed Description

### 3.1. Create Credential Offer

- **URL**: `/credential-offer/{request_id}`
- **Method**: `GET`
- **Description**: Returns the Credential Offer information corresponding to the `request_id`. The flow branches to either `pre-authorized_code` (starts with `p`) or `authorization_code` (starts with `a`) based on the prefix of the `request_id`.

#### Request Example

```shell
# Pre-Authorized Code Flow
curl -X GET "http://${Host}:8080/credential-offer/pGkurKxf5T0Y-mnPFCHqWOMiZi4VS138cQO_V7PZHAdM"

# Authorization Code Flow
curl -X GET "http://${Host}:8080/credential-offer/aGkurKxf5T0Y-mnPFCHqWOMiZi4VS138cQO_V7PZHAdM"
```

#### Response Example (Pre-Authorized Code Flow)

```json
{
  "credential_issuer": "http://localhost:8080",
  "credential_configuration_ids": [
    "UniversityDegree_JWT"
  ],
  "grants": {
    "urn:ietf:params:oauth:grant-type:pre-authorized_code": {
      "pre-authorized_code": "oaKazRN8I0IbtZ0C7JuMn5",
      "tx_code": {
        "input_mode": "numeric",
        "length": 4,
        "description": "Please provide the one-time code that was sent via e-mail"
      }
    }
  }
}
```

---

### 3.2. Get Issuer Metadata

- **URL**: `/.well-known/openid-credential-issuer`
- **Method**: `GET`
- **Description**: Provides metadata about the Issuer's capabilities and policies in accordance with the OID4VCI standard. The Wallet uses this information to determine how to interact with the Issuer.

#### Request Example

```shell
curl -X GET "http://${Host}:8080/.well-known/openid-credential-issuer"
```

#### Response Example

```json
{
  "credential_issuer": "http://localhost:8080",
  "credential_endpoint": "http://localhost:8080/credential",
  "deferred_credential_endpoint": "http://localhost:8080/deferred_credential",
  "credential_configurations_supported": {
    "UniversityDegree_JWT": {
      "format": "jwt_vc_json",
      "scope": "UniversityDegree",
      "cryptographic_binding_methods_supported": [
        "did:example"
      ],
      "credential_signing_alg_values_supported": [
        "ES256"
      ],
      "proof_types_supported": {
        "jwt": {
          "proof_signing_alg_values_supported": [
            "ES256"
          ]
        }
      },
      "display": [
        {
          "name": "University Credential",
          "locale": "en-US"
        }
      ],
      "credential_definition": {
        "type": [
          "VerifiableCredential",
          "UniversityDegreeCredential"
        ]
      }
    }
  },
  "display": [
    {
      "name": "Example University",
      "locale": "en-US"
    }
  ]
}
```

---


### 3.3. Issue Access Token

- **URL**: `/token`
- **Method**: `POST`
- **Description**: The Wallet requests and receives an Access Token using the `pre-authorized_code` obtained from the Credential Offer. This endpoint is used for issuance when a separate authorization server is not used.

#### Request Body

```json
{
  "grant_type": "urn:ietf:params:oauth:grant-type:pre-authorized_code",
  "pre-authorized_code": "oaKazRN8I0IbtZ0C7JuMn5",
  "tx_code": "1234"
}
```

#### Request Example

```shell
curl -X POST "http://${Host}:8080/token" \
-H "Content-Type: application/json" \
-d '{
  "grant_type": "urn:ietf:params:oauth:grant-type:pre-authorized_code",
  "pre-authorized_code": "oaKazRN8I0IbtZ0C7JuMn5",
  "tx_code": "1234"
}'
```

#### Response Example

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ2YyI6...sHQ",
  "token_type": "bearer",
  "expires_in": 86400,
  "c_nonce": "tZignsnFbp",
  "c_nonce_expires_in": 86400
}
```

---


### 3.4. Issue Credential

- **URL**: `/credential`
- **Method**: `POST`
- **Description**: Requests the issuance of a credential from the Issuer using the issued Access Token. According to the OID4VCI standard, the content of the request body differs depending on whether `authorization_details` or `scope` was used in the authorization process. You can receive an immediate issuance or a deferred issuance response.

#### Request Body (Case 1: Using `authorization_details`)

If the Token response includes `authorization_details`, the request is made using the `credential_identifiers` value from that response.

```json
{
  "credential_identifier": "UniversityDegreeCredential-2023",
  "proof": {
    "proof_type": "jwt",
    "jwt": "eyJhbGciOiJFUzI1NiIsImtpZCI6ImRpZDpleGF...In0.eyJhdWQiOiJodHRwczovL2NyZWRlbnRpYWwtaXNzdWVyLmV4YW1wbGUuY29tIiwiaWF0IjoxNzAxOTYwNDQ0LCJub25jZSI6InRadWdubnNGYnAifQ.SIGNATURE"
  }
}
```

#### Request Body (Case 2: Using `scope`)

If the Token response does not include `authorization_details` (e.g., if the Access Token was obtained via the `scope` parameter), the request is made using the `format` parameter. Depending on the `format` value, additional parameters such as `credential_definition` or `doctype` may be included.

**Example 1: `jwt_vc_json` format request**

```json
{
  "credential_configuration_ids": "UniversityDegree_JWT",
  "proof": {
    "proof_type": "jwt",
    "jwt": "eyJhbGciOiJFUzI1NiIsImtpZCI6ImRpZDpleGF...In0.eyJhdWQiOiJodHRwczovL2NyZWRlbnRpYWwtaXNzdWVyLmV4YW1wbGUuY29tIiwiaWF0IjoxNzAxOTYwNDQ0LCJub25jZSI6InRadWdubnNGYnAifQ.SIGNATURE"
  }
}
```

**Example 2: `mso_mdoc` format request (ISO mDL)**

```json
{
  "credential_configuration_ids": "mso_mdoc",
  "proof": {
    "proof_type": "jwt",
    "jwt": "eyJraWQiOiJkaWQ6ZXhhbXBsZTplYmZlYjFmNz...In0.ew...jM"
  }
}
```

#### Request Example

```shell
# Using credential_identifier
curl -X POST "http://${Host}:8080/credential" \
-H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6Ikp...sHQ" \
-H "Content-Type: application/json" \
-d '{
  "credential_identifier": "UniversityDegreeCredential-2023",
  "proof": {
    "proof_type": "jwt",
    "jwt": "eyJhbGciOiJFUzI1NiIsImtpZCI6ImRpZDpleGF...In0.eyJhdWQiOiJodHRwczovL2NyZWRlbnRpYWwtaXNzdWVyLmV4YW1wbGUuY29tIiwiaWF0IjoxNzAxOTYwNDQ0LCJub25jZSI6InRadWdubnNGYnAifQ.SIGNATURE"
  }
}'
```

#### Response Example (Immediate Issuance)

```json
{
  "credentials": [
    {
      "credential": "LUpixVCWJk0eOt4CXQe1NXK....WZwmhmn9OQp6YxX0a2L"
    }
  ]
}
```

#### Response Example (Deferred Issuance)

```json
{
  "transaction_id": "8xL0xBtZp8"
}
```

---


### 3.5. Get Deferred Credential

- **URL**: `/deferred_credential`
- **Method**: `POST`
- **Description**: If a deferred issuance (`202 Accepted`) response is received, periodically check the credential issuance completion status and retrieve the credential using the `transaction_id`.

#### Request Body

```json
{
  "transaction_id": "8xL0xBtZp8"
}
```

#### Request Example

```shell
curl -X POST "http://${Host}:8080/deferred_credential" \
-H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ2YyI6...sHQ" \
-H "Content-Type: application/json" \
-d '{"transaction_id": "8xL0xBtZp8"}'
```

#### Response Example

```json
{
  "credentials": [
    {
      "credential": "LUpixVCWJk0eOt4CXQe1NXK....WZwmhmn9OQp6YxX0a2L"
    }
  ]
}
```

---


### 3.6. Credential Status Notification

- **URL**: `/notification`
- **Method**: `POST`
- **Description**: Notifies the Issuer when the Wallet has successfully stored or failed to store the credential.

#### Request Body

```json
{
  "notification_id": "3fwe98js",
  "event": "credential_accepted"
}
```

#### Request Example

```shell
curl -X POST "http://${Host}:8080/notification" \
-H "Content-Type: application/json" \
-d '{
  "notification_id": "3fwe98js",
  "event": "credential_accepted"
}'
```

#### Response Example

- `200 OK` or `204 No Content` on success

---

### 3.8. Issue Nonce

- **URL**: `/nonce`
- **Method**: `POST`
- **Description**: Issues a `nonce` value to be included in the `proof` for a credential issuance request (Holder Binding).

#### Request Example

```shell
curl -X POST "http://${Host}:8080/nonce"
```

#### Response Example

```json
{
    "nonce": "uG5F23h4Yg",
    "nonce_expires_in": 86400
}
```

---

### 3.9. Get Credential Identifier

- **URL**: `/get-credential-identifier`
- **Method**: `GET`
- **Description**: Retrieves a list of `credential_identifier`s corresponding to a `credentialConfigurationId`. This API can be used by the Wallet to dynamically determine which `credential_identifier` to request when using `authorization_details`.

#### Request Parameters

| Name | Location | Required | Description |
| --- | --- | --- | --- |
| `credentialConfigurationId` | Query | Y | The key value of `credential_configurations_supported` specified in the Issuer Metadata (e.g., `StudentID`) |

#### Request Example

```shell
curl -X GET "http://${Host}:8080/get-credential-identifier?credentialConfigurationId=StudentID"
```

#### Response Example

```json
[
    "TEC",
    "UCR"
]
```

---

## 4. Error Codes

| HTTP Status | Error Code (JSON Body) | Description |
| --- | --- | --- |
| `400 Bad Request` | `invalid_request` | The request is malformed or a required parameter is missing. |
| `400 Bad Request` | `invalid_token` | The provided Access Token is invalid. |
| `400 Bad Request` | `unsupported_credential_format` | The requested Credential format is not supported. |
| `400 Bad Request` | `invalid_proof` | The provided Proof (e.g., JWT) is invalid. |
| `401 Unauthorized` | - | Authentication is required (e.g., missing `Authorization` header). |
| `403 Forbidden` | `credential_request_denied` | The request was denied. |
| `500 Internal Server Error` | - | An internal server error occurred. |
| `503 Service Unavailable`| - | Failed to communicate with an external service (e.g., authorization server). |

---

## 5. Test API

**The APIs in this section should only be used for development and testing purposes.**

### 5.1. Test Page

- **URL**: `/oid4vci/test`
- **Method**: `GET`
- **Description**: Provides a web page for testing the credential issuance flow. You can visually check the QR code generation and issuance process.
