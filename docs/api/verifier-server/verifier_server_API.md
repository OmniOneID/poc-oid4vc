# OID4VP Verifier API Documentation

- Subject: OID4VP Verifier API Document
- Writer: Suhyun Forten Lee
- Date: 2025-11-13
- Version: v1.0.0

| Version | Date | History |
| --- | --- | --- |
| v1.0.0 | 2025-11-13 | Initial Draft |

## Table of Contents

1. [Overview](#1-overview)
2. [API List](#2-api-list)
3. [Detailed API Specifications](#3-detailed-api-specifications)
    - [3.1. Initiate Verification Session](#31-initiate-verification-session)
    - [3.2. Retrieve Authorization Request](#32-retrieve-authorization-request)
    - [3.3. Receive VP Token](#33-receive-vp-token)
    - [3.4. Validate DCQL](#34-validate-dcql)
4. [Error Codes](#4-error-codes)
5. [Test API](#5-test-api)

---

## 1. Overview

This document defines the API for a Verifier service that complies with the [OpenID for Verifiable Presentations (OID4VP) 1.0](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html) standard. The Verifier is the entity that receives and validates Verifiable Presentation (VP Token) from a Wallet.

This Verifier service supports the following functionalities:

- **Verification Session Management**: Create verification sessions based on DCQL (Digital Credentials Query Language) or OAuth2 scope
- **Authorization Request Handling**: Generate OpenID4VP authorization requests via Request URI or inline parameters
- **Response Mode Support**: Support for `direct_post` and `query` response modes
- **VP Token Validation**: Validate the structure, signature, and Holder Binding of Verifiable Presentation tokens
- **DCQL Validation**: Validate the requested credential query (based on sdjwt SDK)
- **Session Management**: Track state through temporary session storage (Redis/DB recommended for production environments)

---

## 2. API List

| API | Method | URL | Description |
| --- | --- | --- | --- |
| `Initiate Verification Session` | POST | `/oid4vp/initiate` | Create a verification session based on DCQL or scope and return the OpenID4VP authorization request URI |
| `Retrieve Authorization Request` | GET | `/oid4vp/request/{request_id}` | Retrieve the authorization request object requested via Request URI (By Reference method) |
| `Receive VP Token` | POST/GET | `/oid4vp/response` | Receive and validate VP Token from Wallet |
| `Validate DCQL` | POST | `/oid4vp/validate-dcql` | Validate the format and rule validity of DCQL queries |
| `Test Page` | GET | `/oid4vp/test` | Provide a web page for testing Verifier functionality |

---

## 3. Detailed API Specifications

### 3.1. Initiate Verification Session

- **URL**: `/oid4vp/initiate`
- **Method**: `POST`
- **Description**: Create a session for verification and return OpenID4VP authorization request information. Provide either a DCQL query or OAuth2 scope to specify what credentials the Wallet should provide. Depending on the `use_request_uri` parameter, the request can be delivered via By Reference (using request_uri) or By Value (inline parameters).

#### Request Example (Using DCQL)

```shell
curl -X POST "http://${Host}:8080/oid4vp/initiate" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d 'dcql_query={"credentials":[{"id":"StudentID"}]}&response_mode=direct_post&use_request_uri=false'
```

#### Request Example (Using Scope)

```shell
curl -X POST "http://${Host}:8080/oid4vp/initiate" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d 'scope=StudentID&response_mode=direct_post&use_request_uri=true'
```

#### Response Example (By Value Method)

```json
{
  "authorization_request_uri": "openid4vp://?response_type=vp_token&client_id=redirect_uri%3Ahttp%3A%2F%2Flocalhost%3A8080%2Foid4vp%2Fcallback&response_mode=direct_post&nonce=550e8400-e29b-41d4-a716-446655440000&state=a1b2c3d4e5f6g7h8&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Foid4vp%2Fcallback&dcql_query=%7B%22credentials%22%3A%5B%7B%22id%22%3A%22StudentID%22%7D%5D%7D&client_metadata=%7B%22client_name%22%3A%22OID4VP%20Verifier%22%7D",
  "method": "by_value",
  "request_id": "550e8400-e29b-41d4-a716-446655440001",
  "state": "a1b2c3d4e5f6g7h8",
  "nonce": "550e8400-e29b-41d4-a716-446655440000",
  "response_mode": "direct_post",
  "dcql_validated": true,
  "credential_count": 1,
  "query_source": "direct",
  "use_request_uri": false,
  "client_id": "redirect_uri:http://localhost:8080/oid4vp/callback",
  "client_metadata": {
    "client_name": "OID4VP Verifier",
    "client_id": "redirect_uri:http://localhost:8080/oid4vp/callback",
    "response_types_supported": [
      "vp_token"
    ],
    "response_modes_supported": [
      "direct_post",
      "query",
      "fragment"
    ]
  }
}
```

#### Response Example (By Reference Method)

```json
{
  "authorization_request_uri": "openid4vp://?request_uri=http%3A%2F%2Flocalhost%3A8080%2Foid4vp%2Frequest%2F550e8400-e29b-41d4-a716-446655440001&client_id=redirect_uri%3Ahttp%3A%2F%2Flocalhost%3A8080%2Foid4vp%2Fcallback",
  "request_uri": "http://localhost:8080/oid4vp/request/550e8400-e29b-41d4-a716-446655440001",
  "method": "by_reference",
  "request_id": "550e8400-e29b-41d4-a716-446655440001",
  "state": "a1b2c3d4e5f6g7h8",
  "nonce": "550e8400-e29b-41d4-a716-446655440000",
  "response_mode": "direct_post",
  "dcql_validated": true,
  "credential_count": 1,
  "query_source": "scope",
  "use_request_uri": true,
  "scope": "StudentID",
  "mapped_dcql": "{\"credentials\":[{\"id\":\"StudentID\"}]}",
  "client_id": "redirect_uri:http://localhost:8080/oid4vp/callback",
  "client_metadata": {
    "client_name": "OID4VP Verifier",
    "client_id": "redirect_uri:http://localhost:8080/oid4vp/callback",
    "response_types_supported": [
      "vp_token"
    ],
    "response_modes_supported": [
      "direct_post",
      "query",
      "fragment"
    ]
  }
}
```

---

### 3.2. Retrieve Authorization Request

- **URL**: `/oid4vp/request/{request_id}`
- **Method**: `GET`
- **Description**: Retrieve the authorization request object requested via Request URI (By Reference method). Through this, the Wallet can dynamically retrieve complete authorization request information using only the `request_uri` parameter. Requests expire according to the `request_uri_ttl` configuration value (default 5 minutes).

#### Request Example

```shell
curl -X GET "http://${Host}:8080/oid4vp/request/550e8400-e29b-41d4-a716-446655440001"
```

#### Response Example

```json
{
  "response_type": "vp_token",
  "client_id": "redirect_uri:http://localhost:8080/oid4vp/callback",
  "response_mode": "direct_post",
  "nonce": "550e8400-e29b-41d4-a716-446655440000",
  "state": "a1b2c3d4e5f6g7h8",
  "response_uri": "http://localhost:8080/oid4vp/response",
  "dcql_query": "{\"credentials\":[{\"id\":\"StudentID\"}]}",
  "client_metadata": {
    "client_name": "OID4VP Verifier",
    "client_id": "redirect_uri:http://localhost:8080/oid4vp/callback",
    "response_types_supported": [
      "vp_token"
    ],
    "response_modes_supported": [
      "direct_post",
      "query",
      "fragment"
    ]
  }
}
```

---

### 3.3. Receive VP Token

- **URL**: `/oid4vp/response`
- **Method**: `POST`, `GET`
- **Description**: Receive and validate VP Token from Wallet. In `direct_post` response mode, it receives via POST request, and in `query` response mode, it receives via GET request. During the validation process, the structure, signature, Holder Binding, etc. of the token are validated.

#### Request Example (Direct Post - POST)

```shell
curl -X POST "http://${Host}:8080/oid4vp/response" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d 'vp_token=eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9...&state=a1b2c3d4e5f6g7h8'
```

#### Request Example (Query Mode - GET)

```shell
curl -X GET "http://${Host}:8080/oid4vp/response?vp_token=eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9...&state=a1b2c3d4e5f6g7h8"
```

#### Response Example (Success)

```json
// 200 Response
```

#### Response Example (Failure)

```json
{
  "error": "invalid_token",
  "error_description": "VP Token signature validation failed",
  "state": "a1b2c3d4e5f6g7h8",
  "verification_result": {
    "valid": false,
    "message": "VP Token signature is invalid",
    "errors": [
      "Signature verification failed for credential StudentID"
    ],
    "warnings": []
  }
}
```

---

### 3.4. Validate DCQL

- **URL**: `/oid4vp/validate-dcql`
- **Method**: `POST`
- **Description**: Validate the format and rule validity of DCQL (Digital Credentials Query Language) queries. Using the DCQLQueryValidator from the sdjwt SDK to verify standard compliance. This endpoint is provided so clients can pre-validate queries.

#### Request Body

Pass the DCQL query JSON object as the request body.

```json
{
  "credentials": [
    {
      "id": "national_id",
      "format": "vc+sd-jwt",
      "meta": {
        "vct_values": [
          "https://credentials.gov.kr/identity_credential"
        ]
      }
    }
  ]
}
```

#### Request Example

```shell
curl -X POST "http://${Host}:8080/oid4vp/validate-dcql" \
-H "Content-Type: application/json" \
-d '{
  "credentials": [
    {
      "id": "national_id",
      "format": "vc+sd-jwt",
      "meta": {
        "vct_values": [
          "https://credentials.gov.kr/identity_credential"
        ]
      }
    }
  ]
}'
```

#### Response Example (Valid)

```json
{
  "valid": true,
  "success": true,
  "message": "DCQL query is valid",
  "credential_count": 1,
  "errors": [],
  "warnings": []
}
```

#### Response Example (Invalid)

```json
{
  "valid": false,
  "success": false,
  "message": "Invalid DCQL query: Missing required field 'credentials'",
  "credential_count": 0,
  "errors": [
    "Missing required field 'credentials'",
    "Invalid credential structure"
  ],
  "warnings": []
}
```

---

## 4. Error Codes

| HTTP Status | Error Code (JSON Body) | Description |
| --- | --- | --- |
| `400 Bad Request` | `invalid_request` | The request is malformed or required parameters are missing. |
| `400 Bad Request` | `invalid_request_uri` | The request_uri is invalid or has expired. |
| `400 Bad Request` | `invalid_scope` | There is no DCQL mapping for the provided scope. |
| `400 Bad Request` | `invalid_token` | The VP Token format is incorrect or validation failed. |
| `401 Unauthorized` | - | Authentication is required. (depending on the API) |
| `403 Forbidden` | - | The request has been denied. |
| `404 Not Found` | - | The requested resource could not be found. (e.g., request_id session has expired) |
| `500 Internal Server Error` | `server_error` | An internal server error has occurred. |
| `500 Internal Server Error` | `processing_error` | An error occurred while processing the request. |

#### Error Response Example

```json
{
  "error": "invalid_request",
  "error_description": "Either dcql_query or scope parameter is required",
  "state": "a1b2c3d4e5f6g7h8"
}
```

---

## 5. Test API

**The APIs in this section should only be used for development and testing purposes.**

### 5.1. Test Page

- **URL**: `/oid4vp/test`
- **Method**: `GET`
- **Description**: Provides a web page for testing the Verifier's verification functionality. You can create DCQL queries, initiate sessions, and more.
