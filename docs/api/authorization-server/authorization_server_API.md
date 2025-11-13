# OID4VCI Authorization Server API Document

- Subject: OID4VCI Authorization Server API Document
- Writer: Sangjun Kim
- Date: 2025-10-24
- Version: v1.0.0

| Version | Date | History |
| --- | --- | --- |
| v1.0.0 | 2025-10-24 | Initial draft |

## Table of Contents

1. [Overview](#1-overview)
2. [API List](#2-api-list)
3. [Custom API Detailed Description](#3-custom-api-detailed-description)
    - [3.1. Get Login Page](#31-get-login-page)
    - [3.2. Issue Pre-Authorized Code](#32-issue-pre-authorized-code)
4. [OAuth 2.0 Standard Endpoints](#4-oauth-20-standard-endpoints)
    - [4.1. Authorization Endpoint](#41-authorization-endpoint)
    - [4.2. Token Endpoint](#42-token-endpoint)
    - [4.3. JWK Set Endpoint](#43-jwk-set-endpoint)
    - [4.4. OpenID Provider Configuration](#44-openid-provider-configuration)
5. [Error Codes](#5-error-codes)
6. [Notes](#6-notes)

---

## 1. Overview

This document defines the API of an Authorization Server that supports the Pre-Authorized Code Flow of the [OpenID for Verifiable Credential Issuance (OID4VCI)](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html) standard.

This server is implemented based on `spring-security-oauth2-authorization-server` and provides both standard OAuth 2.0 endpoints and custom endpoints for OID4VCI.

---

## 2. API List

### Custom API
| API | Method | URL | Description |
| --- | --- | --- | --- |
| `Get Login Page` | GET | `/login` | Returns a login page for user authentication. |
| `Handle Social Login Callback` | GET | `/auth/callback` | A callback endpoint that is called upon successful login through an external OAuth2 provider such as Google. |
| `Issue Pre-Authorized Code` | POST | `/pre-authorize` | Issues a Pre-Authorized Code for an authenticated user. |

### OAuth 2.0 Standard API
| API | Method | URL | Description |
| --- | --- | --- | --- |
| `Authorization Endpoint` | GET | `/oauth2/authorize` | Authenticates the user and obtains consent to issue an authorization code. |
| `Token Endpoint` | POST | `/oauth2/token` | Issues an access token using an authorization code or other grant types. |
| `JWK Set` | GET | `/oauth2/jwks` | Provides a list of public keys (JSON Web Key Set) required for token signature verification. |
| `OIDC Configuration` | GET | `/.well-known/openid-configuration` | Provides endpoint and server configuration information related to OpenID Connect. |

---

## 3. Custom API Detailed Description

### 3.1. Get Login Page

- **URL**: `/login`
- **Method**: `GET`
- **Description**: Provides a web page where users can log in to the system by entering their ID and password. If the login is successful, the server stores the user's authentication status in the session. Login is done through /oauth2/authorize.


### 3.2. Handle Social Login Callback

- **URL**: `/auth/callback`
- **Method**: `GET`
- **Description**: This is a callback endpoint to which the user is redirected after a social login using an external OAuth 2.0 provider like Google is successfully completed. This API operates as part of the `oauth2Login` flow set up in `SecurityConfig`, processes the authentication result, and finally returns the `auth-callback.html` view to the user to guide subsequent processing.


### 3.3. Issue Pre-Authorized Code

- **URL**: `/pre-authorize`
- **Method**: `POST`
- **Description**: When a credential offer is created in the Issuer and issuance is performed through the pre-authorized code flow, a request is made through this endpoint to generate and return a Pre-Authorized Code. The request uses the identifier of the credential required for issuance.

#### Request Body

```json
{
  [
    "UniversityDegree"
  ]
}
```

#### Request Example

```shell
curl -X POST "http://${Host}:8080/pre-authorize" \
-H "Content-Type: application/json" \
-d '{
  [
    "UniversityDegree"
  ]
}'
```

#### Expected Success Response (200 OK)

```json
{
    "preAuthorizedCode": "H4s...k9w",
    "expiresIn": 600,
    "userPin": "1234"
}
```

---

## 4. OAuth 2.0 Standard Endpoints

### 4.1. Authorization Endpoint

- **URL**: `/oauth2/authorize`
- **Method**: `GET`
- **Description**: This endpoint starts the Authorization Code Grant flow of OAuth 2.0. The user logs in through this endpoint and proceeds with consent for the scope requested by the client. On success, it redirects to the registered `redirect_uri` with an authorization code.

#### Key Request Parameters (Query Parameters)

| Name | Required | Description |
| --- | --- | --- |
| `response_type` | Yes | Must be fixed to `code`. |
| `client_id` | Yes | Client ID |
| `scope` | Yes | The scope of the requested permissions (space-separated) |
| `redirect_uri` | Yes | The client's URI to receive and process the authorization code |
| `state` | Recommended | A random string to prevent CSRF attacks |
| `code_challenge`| Recommended | Code challenge for PKCE support |
| `code_challenge_method` | Recommended | Hash algorithm for PKCE support (usually `S256`) |
| `authorization_details` | Optional | The `authorization_details` object defined in OID4VCI. Specifies the type and format of the credential to be requested by including `credential_configuration_id`. |


### 4.2. Token Endpoint

- **URL**: `/oauth2/token`
- **Method**: `POST`
- **Description**: This endpoint exchanges an authorization code or other grant types for an access token, refresh token, etc. This server supports both the standard `authorization_code` grant type and the custom `urn:ietf:params:oauth:grant-type:pre-authorized_code` grant type for OID4VCI.

#### Request Parameters (Form-urlencoded)

**Case 1: `authorization_code` Grant Type**

| Name | Required | Description |
| --- | --- | --- |
| `grant_type` | Yes | Set to `authorization_code` |
| `code` | Yes | The authorization code issued through `/oauth2/authorize` |
| `redirect_uri` | Yes | The `redirect_uri` used in the authorization code request |
| `client_id` | Yes | Client ID |
| `client_secret` | Yes | Client secret (for Confidential Clients) |
| `code_verifier` | If used | The original value corresponding to `code_challenge` when using PKCE |

**Case 2: `pre-authorized_code` (Custom) Grant Type**

| Name | Required | Description |
| --- | --- | --- |
| `grant_type` | Yes | Set to `urn:ietf:params:oauth:grant-type:pre-authorized_code` |
| `pre-authorized_code` | Yes | The pre-authorized code issued through `/pre-authorize` |
| `user_pin` | Yes | If a PIN was issued with the pre-authorized code, the corresponding PIN value |
| `authorization_details` | Optional | The `authorization_details` object defined in OID4VCI. Specifies the type and format of the credential to be requested by including `credential_configuration_id`. |

#### Success Response (200 OK)

```json
{
   "access_token":"eyJra...emis50GOxLzafA",
   "token_type":"Bearer",
   "expires_in":299
}
```

**Note:** If the `authorization_details` parameter was included in the initial authorization code request, the token response may include an `authorization_details` field with added `credential_identifiers`.

*Example of a successful response including `authorization_details`:*
```json
{
    "access_token": "eyJra...emis50GOxLzafA",
    "token_type": "Bearer",
    "expires_in": 299,
    "authorization_details":[
        {
          "credential_configuration_id":"VerifiableIdSD",
          "credential_identifiers":[
              "NationalID",
              "mDL"
          ],
          "type":"openid_credential"
        },
        {
          "credential_configuration_id":"StudentID",
          "credential_identifiers":[
              "TEC",
              "UCR"
          ],
          "type":"openid_credential"
        }
    ]
}
```

#### Access Token Claim Information

The issued JWT format access token contains different claims depending on the `grant_type`.

**1. For Standard Grant Type (`authorization_code`)**

In the standard flow that goes through user authentication, the token includes the following main claims:

- `sub`: The ID of the authenticated user (e.g., `user`)
- `iss`: The issuer URL (e.g., `http://localhost:8080`)

*Example (Decoded JWT Payload):*
```json
{
  "sub": "user",
  "aud": "oid4vci-client",
  "nbf": 1726640000,
  "iss": "http://localhost:8080",
  "exp": 1726640300,
  "iat": 1726640000,
}
```

**2. For `pre-authorized_code` Grant Type**

In the OID4VCI pre-authorized code flow, the token includes the following main claims:

- `sub`: The client ID (e.g., `oid4vci-client`)
- `iss`: The issuer URL (e.g., `http://localhost:8080`)

*Example (Decoded JWT Payload):*
```json
{
  "sub": "oid4vci-client",
  "aud": "oid4vci-client",
  "nbf": 1726640100,
  "iss": "http://localhost:8080",
  "exp": 1726640400,
  "iat": 1726640100,
}
```

### 4.3. JWK Set Endpoint

- **URL**: `/oauth2/jwks`
- **Method**: `GET`
- **Description**: Provides a list of public keys in JWK Set format required to verify the signature of the JWT format access token issued by the authorization server. A resource server or client can use this information to verify the validity of the token.

### 4.4. OpenID Provider Configuration

- **URL**: `/.well-known/openid-configuration`
- **Method**: `GET`
- **Description**: Provides metadata so that clients that follow the OpenID Connect spec can dynamically discover the authorization server's various endpoints (authorization, token, user info, etc.) and supported features (algorithms, scope, etc.).

---

## 5. Error Codes

| HTTP Status | Description |
| --- | --- |
| `400 Bad Request` | Occurs when the request body or parameters are incorrect or required values are missing (e.g., `invalid_request`, `invalid_grant`). |
| `401 Unauthorized` | Occurs when client authentication fails (e.g., `invalid_client`). |
| `403 Forbidden` | Occurs when authenticated but does not have permission to access the resource. |
| `500 Internal Server Error` | Occurs when an error occurs during internal server logic processing. |

---

## 6. Notes

- `client_id`, `scope`, etc. required for API calls must be registered with the authorization server in advance.