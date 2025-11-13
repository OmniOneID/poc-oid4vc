# OID4VCI Authorization Server API 문서

- Subject: OID4VCI Authorization Server API Document
- Writer: 김상준
- Date: 2025-10-24
- Version: v1.0.0

| Version | Date | History |
| --- | --- | --- |
| v1.0.0 | 2025-10-24 | 초기 작성 |

## 목차

1. [개요](#1-개요)
2. [API 목록](#2-api-목록)
3. [커스텀 API 상세 설명](#3-커스텀-api-상세-설명)
    - [3.1. 로그인 페이지 조회](#31-로그인-페이지-조회)
    - [3.2. 사전 승인 코드 발급](#32-사전-승인-코드-발급)
4. [OAuth 2.0 표준 엔드포인트](#4-oauth-20-표준-엔드포인트)
    - [4.1. 인가 엔드포인트](#41-인가-엔드포인트)
    - [4.2. 토큰 엔드포인트](#42-토큰-엔드포인트)
    - [4.3. JWK Set 엔드포인트](#43-jwk-set-엔드포인트)
    - [4.4. OpenID Provider Configuration](#44-openid-provider-configuration)
5. [에러 코드](#5-에러-코드)
6. [참고 사항](#6-참고-사항)

---

## 1. 개요

본 문서는 [OpenID for Verifiable Credential Issuance (OID4VCI)](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html) 표준의 사전 승인 코드 플로우(Pre-Authorized Code Flow)를 지원하는 인가 서버(Authorization Server)의 API를 정의합니다.

이 서버는 `spring-security-oauth2-authorization-server`를 기반으로 구현되었으며, 표준 OAuth 2.0 엔드포인트와 OID4VCI를 위한 커스텀 엔드포인트를 모두 제공합니다.

---

## 2. API 목록

### 커스텀 API
| API | Method | URL | 설명 |
| --- | --- | --- | --- |
| `로그인 페이지 조회` | GET | `/login` | 사용자 인증을 위한 로그인 페이지를 반환합니다. |
| `소셜 로그인 콜백 처리` | GET | `/auth/callback` | Google 등 외부 OAuth2 공급자를 통한 로그인 성공 시 호출되는 콜백 엔드포인트입니다. |
| `사전 승인 코드 발급` | POST | `/pre-authorize` | 인증된 사용자에 대해 사전 승인 코드(Pre-Authorized Code)를 발급합니다. |

### OAuth 2.0 표준 API
| API | Method | URL | 설명 |
| --- | --- | --- | --- |
| `인가 엔드포인트` | GET | `/oauth2/authorize` | 사용자를 인증하고 동의를 얻어 인가 코드를 발급합니다. |
| `토큰 엔드포인트` | POST | `/oauth2/token` | 인가 코드나 다른 승인 타입을 사용하여 액세스 토큰을 발급합니다. |
| `JWK Set` | GET | `/oauth2/jwks` | 토큰 서명 검증에 필요한 공개 키 목록(JSON Web Key Set)을 제공합니다. |
| `OIDC 설정` | GET | `/.well-known/openid-configuration` | OpenID Connect 관련 엔드포인트 및 서버 설정 정보를 제공합니다. |

---

## 3. 커스텀 API 상세 설명

### 3.1. 로그인 페이지 조회

- **URL**: `/login`
- **Method**: `GET`
- **설명**: 사용자가 아이디와 비밀번호를 입력하여 시스템에 로그인할 수 있는 웹 페이지를 제공합니다. 로그인이 성공하면, 서버는 사용자의 인증 상태를 세션에 저장합니다. /oauth2/authorize를 통하여 로그인합니다.


### 3.2. 소셜 로그인 콜백 처리

- **URL**: `/auth/callback`
- **Method**: `GET`
- **설명**: Google과 같은 외부 OAuth 2.0 공급자를 사용한 소셜 로그인이 성공적으로 완료된 후, 사용자가 리디렉션되는 콜백(Callback) 엔드포인트입니다. 이 API는 `SecurityConfig`에 설정된 `oauth2Login` 플로우의 일부로 동작하며, 인증 결과를 처리하고 최종적으로 사용자에게 `auth-callback.html` 뷰를 반환하여 후속 처리를 안내합니다.


### 3.3. 사전 승인 코드 발급

- **URL**: `/pre-authorize`
- **Method**: `POST`
- **설명**: Issuer에서 credential offer 생성 시 pre-authorized code flow를 통한 발급이 이루어지는 경우 해당 엔드포인트를 통하여 요청하게 되면 Pre-Authorized Code를 생성하고 반환합니다. 요청은 발급 시 팔요한 해당 자격증명의 식별자를 사용합니다.

#### 요청 본문

```json
{
  [
    "UniversityDegree"
  ]
}
```

#### 요청 예시

```shell
curl -X POST "http://${Host}:8080/pre-authorize" \
-H "Content-Type: application/json" \
-d '{
  [
    "UniversityDegree"
  ]
}'
```

#### 예상 성공 응답 (200 OK)

```json
{
    "preAuthorizedCode": "H4s...k9w",
    "expiresIn": 600,
    "userPin": "1234"
}
```

---

## 4. OAuth 2.0 표준 엔드포인트

### 4.1. 인가 엔드포인트

- **URL**: `/oauth2/authorize`
- **Method**: `GET`
- **설명**: OAuth 2.0의 인가 코드 승인 플로우(Authorization Code Grant)를 시작하는 엔드포인트입니다. 사용자는 이 엔드포인트를 통해 로그인하고, 클라이언트가 요청한 권한(scope)에 대한 동의를 진행합니다. 성공 시, 등록된 `redirect_uri`로 인가 코드와 함께 리디렉션됩니다.

#### 주요 요청 파라미터 (Query Parameters)

| 이름 | 필수 여부 | 설명 |
| --- | --- | --- |
| `response_type` | Yes | `code`로 고정되어야 합니다. |
| `client_id` | Yes | 클라이언트 ID |
| `scope` | Yes | 요청할 권한의 범위 (공백으로 구분) |
| `redirect_uri` | Yes | 인가 코드를 받아 처리할 클라이언트의 URI |
| `state` | Recommended | CSRF 공격 방지를 위한 임의의 문자열 |
| `code_challenge`| Recommended | PKCE 지원을 위한 코드 챌린지 |
| `code_challenge_method` | Recommended | PKCE 지원을 위한 해시 알고리즘 (주로 `S256`) |
| `authorization_details` | Optional | OID4VCI에서 정의된 `authorization_details` 객체. `credential_configuration_id`를 포함하여 요청할 자격증명의 종류와 형식을 지정합니다. |


### 4.2. 토큰 엔드포인트

- **URL**: `/oauth2/token`
- **Method**: `POST`
- **설명**: 인가 코드 또는 다른 승인 타입(Grant Type)을 액세스 토큰, 리프레시 토큰 등으로 교환하는 엔드포인트입니다. 본 서버는 표준 `authorization_code` 승인 타입과 OID4VCI를 위한 커스텀 `urn:ietf:params:oauth:grant-type:pre-authorized_code` 승인 타입을 모두 지원합니다.

#### 요청 파라미터 (Form-urlencoded)

**Case 1: `authorization_code` 승인 타입**

| 이름 | 필수 여부 | 설명 |
| --- | --- | --- |
| `grant_type` | Yes | `authorization_code`로 설정 |
| `code` | Yes | `/oauth2/authorize`를 통해 발급받은 인가 코드 |
| `redirect_uri` | Yes | 인가 코드 요청 시 사용했던 `redirect_uri` |
| `client_id` | Yes | 클라이언트 ID |
| `client_secret` | Yes | 클라이언트 시크릿 (Confidential Client의 경우) |
| `code_verifier` | If used | PKCE 사용 시, `code_challenge`에 대응하는 원본 값 |

**Case 2: `pre-authorized_code` (커스텀) 승인 타입**

| 이름 | 필수 여부 | 설명 |
| --- | --- | --- |
| `grant_type` | Yes | `urn:ietf:params:oauth:grant-type:pre-authorized_code`로 설정 |
| `pre-authorized_code` | Yes | `/pre-authorize`를 통해 발급받은 사전 승인 코드 |
| `user_pin` | Yes | 사전 승인 코드 발급 시 PIN이 함께 발급된 경우, 해당 PIN 값 |
| `authorization_details` | Optional | OID4VCI에서 정의된 `authorization_details` 객체. `credential_configuration_id`를 포함하여 요청할 자격증명의 종류와 형식을 지정합니다. |

#### 성공 응답 (200 OK)

```json
{
   "access_token":"eyJra...emis50GOxLzafA",
   "token_type":"Bearer",
   "expires_in":299
}
```

**참고:** 만약 최초 인가 코드 요청 시 `authorization_details` 파라미터가 포함되었다면, 토큰 응답에는 `credential_identifiers`가 추가된 `authorization_details` 필드가 포함될 수 있습니다.

*`authorization_details`가 포함된 성공 응답 예시:*
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

#### 액세스 토큰 클레임 정보

발급되는 JWT 형식의 액세스 토큰에는 `grant_type`에 따라 다음과 같이 다른 클레임(Claim)이 포함됩니다.

**1. 표준 승인 타입 (`authorization_code`)의 경우**

사용자 인증을 거치는 표준 플로우에서는 토큰에 다음과 같은 주요 클레임이 포함됩니다.

- `sub`: 인증된 사용자의 ID (예: `user`)
- `iss`: 발급자 (issuer) URL (예: `http://localhost:8080`)

*예시 (Decoded JWT Payload):*
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

**2. `pre-authorized_code` 승인 타입의 경우**

OID4VCI의 사전 승인 코드 플로우에서는 토큰에 다음과 같은 주요 클레임이 포함됩니다.

- `sub`: 클라이언트 ID (예: `oid4vci-client`)
- `iss`: 발급자 (issuer) URL (예: `http://localhost:8080`)

*예시 (Decoded JWT Payload):*
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

### 4.3. JWK Set 엔드포인트

- **URL**: `/oauth2/jwks`
- **Method**: `GET`
- **설명**: 인가 서버가 발급한 JWT 형식의 액세스 토큰의 서명을 검증하는 데 필요한 공개 키 목록을 JWK Set 형식으로 제공합니다. 리소스 서버나 클라이언트는 이 정보를 사용하여 토큰의 유효성을 검증할 수 있습니다.

### 4.4. OpenID Provider Configuration

- **URL**: `/.well-known/openid-configuration`
- **Method**: `GET`
- **설명**: OpenID Connect 스펙을 따르는 클라이언트가 인가 서버의 각종 엔드포인트(인가, 토큰, 사용자 정보 등)와 지원 기능(알고리즘, scope 등)을 동적으로 발견할 수 있도록 메타데이터를 제공합니다.

---

## 5. 에러 코드

| HTTP Status | 설명 |
| --- | --- |
| `400 Bad Request` | 요청 본문이나 파라미터가 잘못되었거나 필수 값이 누락된 경우 발생합니다. (예: `invalid_request`, `invalid_grant`) |
| `401 Unauthorized` | 클라이언트 인증에 실패한 경우 발생합니다. (예: `invalid_client`) |
| `403 Forbidden` | 인증은 되었으나 해당 리소스에 접근할 권한이 없는 경우 발생합니다. |
| `500 Internal Server Error` | 서버 내부 로직 처리 중 오류가 발생한 경우입니다. |

---

## 6. 참고 사항

- API 호출 시 필요한 `client_id` 및 `scope` 등은 사전에 인가 서버에 등록되어 있어야 합니다.