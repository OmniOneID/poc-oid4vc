# OID4VCI Issuer API 문서

- Subject: OID4VCI Issuer API Document
- Writer: 김상준
- Date: 2025-10-23
- Version: v1.0.0

| Version | Date | History |
| --- | --- | --- |
| v1.0.0 | 2025-10-23 | 초기 작성 |

## 목차

1. [개요](#1-개요)
2. [API 목록](#2-api-목록)
3. [API 상세 설명](#3-api-상세-설명)
    - [3.1. Credential Offer 생성](#31-credential-offer-생성)
    - [3.2. Issuer Metadata 조회](#32-issuer-metadata-조회)
    - [3.3. Access Token 발급](#33-access-token-발급)
    - [3.4. Credential 발급](#34-credential-발급)
    - [3.5. Deferred Credential 조회](#35-deferred-credential-조회)
    - [3.6. Credential 상태 알림](#36-credential-상태-알림)
    - [3.7. Nonce 발급](#37-nonce-발급)
    - [3.8. Credential Identifier 조회](#38-credential-identifier-조회)
4. [에러 코드](#4-에러-코드)
5. [테스트 API](#5-테스트-api)

---

## 1. 개요

본 문서는 [OpenID for Verifiable Credential Issuance (OID4VCI)](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html) 표준을 준수하는 Issuer 서비스의 API를 정의합니다. Issuer는 자격증명(Credential)을 생성하고 발급하는 주체입니다.

---

## 2. API 목록

| API | Method | URL | 설명 |
| --- | --- | --- | --- |
| `Credential Offer 생성` | GET | `/credential-offer/{request_id}` | 자격증명 발급 제안(Credential Offer)을 생성하여 반환 |
| `Issuer Metadata 조회` | GET | `/.well-known/openid-credential-issuer` | Issuer의 설정 정보(Metadata)를 조회 |
| `Access Token 발급` | POST | `/token` | Pre-Authorized Code를 Access Token으로 교환 (인가서버 미사용시) |
| `Credential 발급` | POST | `/credential` | Access Token을 사용하여 자격증명을 발급 |
| `Deferred Credential 조회`| POST | `/deferred_credential` | 지연된 자격증명 발급의 경우, 트랜잭션 ID를 사용하여 자격증명을 조회 |
| `Credential 상태 알림` | POST | `/notification` | Wallet으로부터 Credential의 저장 상태(성공/실패)를 알림 |
| `Nonce 발급` | POST | `/nonce` | Credential 발급 요청 시 사용할 Nonce 값을 발급 |
| `Credential Identifier 조회` | GET | `/get-credential-identifier` | `credentialConfigurationId`에 해당하는 Credential Identifier 목록을 조회 |

---

## 3. API 상세 설명

### 3.1. Credential Offer 생성

- **URL**: `/credential-offer/{request_id}`
- **Method**: `GET`
- **설명**: `request_id`에 해당하는 자격증명 발급 제안(Credential Offer) 정보를 반환합니다. `request_id`의 접두사에 따라 `pre-authorized_code` (`p`로 시작) 또는 `authorization_code` (`a`로 시작) 흐름으로 분기됩니다.

#### 요청 예시

```shell
# Pre-Authorized Code Flow
curl -X GET "http://${Host}:8080/credential-offer/pGkurKxf5T0Y-mnPFCHqWOMiZi4VS138cQO_V7PZHAdM"

# Authorization Code Flow
curl -X GET "http://${Host}:8080/credential-offer/aGkurKxf5T0Y-mnPFCHqWOMiZi4VS138cQO_V7PZHAdM"
```

#### 응답 예시 (Pre-Authorized Code Flow)

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

### 3.2. Issuer Metadata 조회

- **URL**: `/.well-known/openid-credential-issuer`
- **Method**: `GET`
- **설명**: OID4VCI 표준에 따라 Issuer의 기능 및 정책에 대한 메타데이터를 제공합니다. Wallet은 이 정보를 사용하여 Issuer와 상호작용하는 방법을 결정합니다.

#### 요청 예시

```shell
curl -X GET "http://${Host}:8080/.well-known/openid-credential-issuer"
```

#### 응답 예시

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


### 3.3. Access Token 발급

- **URL**: `/token`
- **Method**: `POST`
- **설명**: Wallet이 Credential Offer를 통해 얻은 `pre-authorized_code`를 사용하여 Access Token을 요청하고 발급받습니다. 별도의 인가서버를 사용하지 않을 경우 해당 endpoint를
통하여 발급 받습니다.

#### 요청 본문

```json
{
  "grant_type": "urn:ietf:params:oauth:grant-type:pre-authorized_code",
  "pre-authorized_code": "oaKazRN8I0IbtZ0C7JuMn5",
  "tx_code": "1234"
}
```

#### 요청 예시

```shell
curl -X POST "http://${Host}:8080/token" \
-H "Content-Type: application/json" \
-d '{
  "grant_type": "urn:ietf:params:oauth:grant-type:pre-authorized_code",
  "pre-authorized_code": "oaKazRN8I0IbtZ0C7JuMn5",
  "tx_code": "1234"
}'
```

#### 응답 예시

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


### 3.4. Credential 발급

- **URL**: `/credential`
- **Method**: `POST`
- **설명**: 발급받은 Access Token을 사용하여 Issuer에게 자격증명(Credential) 발급을 요청합니다. OID4VCI 표준에 따라, 인가 과정에서 `authorization_details`를 사용했는지, `scope`를 사용했는지에 따라 요청 본문의 내용이 달라집니다. 즉시 발급 또는 지연 발급(Deferred) 응답을 받을 수 있습니다.

#### 요청 본문 (Case 1: `authorization_details`를 사용한 경우)

Token 응답에 `authorization_details`가 포함된 경우, 해당 응답의 `credential_identifiers` 값을 사용하여 요청합니다.

```json
{
  "credential_identifier": "UniversityDegreeCredential-2023",
  "proof": {
    "proof_type": "jwt",
    "jwt": "eyJhbGciOiJFUzI1NiIsImtpZCI6ImRpZDpleGF...In0.eyJhdWQiOiJodHRwczovL2NyZWRlbnRpYWwtaXNzdWVyLmV4YW1wbGUuY29tIiwiaWF0IjoxNzAxOTYwNDQ0LCJub25jZSI6InRadWdubnNGYnAifQ.SIGNATURE"
  }
}
```

#### 요청 본문 (Case 2: `scope`를 사용한 경우)

Token 응답에 `authorization_details`가 포함되지 않은 경우(예: `scope` 파라미터를 통해 Access Token을 획득한 경우), `format` 파라미터를 사용하여 요청합니다. `format` 값에 따라 `credential_definition` 또는 `doctype`과 같은 추가 파라미터가 포함될 수 있습니다.

**예시 1: `jwt_vc_json` 형식 요청**

```json
{
  "credential_configuration_ids": "UniversityDegree_JWT",
  "proof": {
    "proof_type": "jwt",
    "jwt": "eyJhbGciOiJFUzI1NiIsImtpZCI6ImRpZDpleGF...In0.eyJhdWQiOiJodHRwczovL2NyZWRlbnRpYWwtaXNzdWVyLmV4YW1wbGUuY29tIiwiaWF0IjoxNzAxOTYwNDQ0LCJub25jZSI6InRadWdubnNGYnAifQ.SIGNATURE"
  }
}
```

**예시 2: `mso_mdoc` 형식 요청 (ISO mDL)**

```json
{
  "credential_configuration_ids": "mso_mdoc",
  "proof": {
    "proof_type": "jwt",
    "jwt": "eyJraWQiOiJkaWQ6ZXhhbXBsZTplYmZlYjFmNz...In0.ew...jM"
  }
}
```

#### 요청 예시

```shell
# credential_identifier를 사용하는 경우
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

#### 응답 예시 (즉시 발급)

```json
{
  "credentials": [
    {
      "credential": "LUpixVCWJk0eOt4CXQe1NXK....WZwmhmn9OQp6YxX0a2L"
    }
  ]
}
```

#### 응답 예시 (지연 발급)

```json
{
  "transaction_id": "8xL0xBtZp8"
}
```

---


### 3.5. Deferred Credential 조회

- **URL**: `/deferred_credential`
- **Method**: `POST`
- **설명**: 지연 발급(`202 Accepted`) 응답을 받은 경우, `transaction_id`를 사용하여 주기적으로 Credential 발급 완료 여부를 확인하고 Credential을 조회합니다.

#### 요청 본문

```json
{
  "transaction_id": "8xL0xBtZp8"
}
```

#### 요청 예시

```shell
curl -X POST "http://${Host}:8080/deferred_credential" \
-H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ2YyI6...sHQ" \
-H "Content-Type: application/json" \
-d '{"transaction_id": "8xL0xBtZp8"}'
```

#### 응답 예시

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


### 3.6. Credential 상태 알림

- **URL**: `/notification`
- **Method**: `POST`
- **설명**: Wallet이 Credential을 성공적으로 저장했거나, 저장에 실패한 경우 그 상태를 Issuer에게 알립니다.

#### 요청 본문

```json
{
  "notification_id": "3fwe98js",
  "event": "credential_accepted"
}
```

#### 요청 예시

```shell
curl -X POST "http://${Host}:8080/notification" \
-H "Content-Type: application/json" \
-d '{
  "notification_id": "3fwe98js",
  "event": "credential_accepted"
}'
```

#### 응답 예시

- 성공 시 `200 OK` 또는 `204 No Content`

---

### 3.8. Nonce 발급

- **URL**: `/nonce`
- **Method**: `POST`
- **설명**: Credential 발급 요청 시 `proof`에 포함될 `nonce` 값을 발급합니다. (Holder Binding)

#### 요청 예시

```shell
curl -X POST "http://${Host}:8080/nonce"
```

#### 응답 예시

```json
{
    "nonce": "uG5F23h4Yg",
    "nonce_expires_in": 86400
}
```

---

### 3.9. Credential Identifier 조회

- **URL**: `/get-credential-identifier`
- **Method**: `GET`
- **설명**: `credentialConfigurationId`에 해당하는 `credential_identifier` 목록을 조회합니다. 이 API는 `authorization_details`를 사용하는 경우, Wallet이 어떤 `credential_identifier`를 요청해야 하는지 동적으로 결정하는 데 사용될 수 있습니다.

#### 요청 파라미터

| 이름 | 위치 | 필수 | 설명 |
| --- | --- | --- | --- |
| `credentialConfigurationId` | Query | Y | Issuer Metadata에 명시된 `credential_configurations_supported`의 키 값 (예: `StudentID`) |

#### 요청 예시

```shell
curl -X GET "http://${Host}:8080/get-credential-identifier?credentialConfigurationId=StudentID"
```

#### 응답 예시

```json
[
    "TEC",
    "UCR"
]
```

---

## 4. 에러 코드

| HTTP Status | 에러 코드 (JSON Body) | 설명 |
| --- | --- | --- |
| `400 Bad Request` | `invalid_request` | 요청이 잘못되었거나 필수 파라미터가 누락되었습니다. |
| `400 Bad Request` | `invalid_token` | 제공된 Access Token이 유효하지 않습니다. |
| `400 Bad Request` | `unsupported_credential_format` | 요청된 Credential 형식을 지원하지 않습니다. |
| `400 Bad Request` | `invalid_proof` | 제공된 Proof(JWT 등)가 유효하지 않습니다. |
| `401 Unauthorized` | - | 인증이 필요합니다. (예: `Authorization` 헤더 누락) |
| `403 Forbidden` | `credential_request_denied` | 요청이 거부되었습니다. |
| `500 Internal Server Error` | - | 서버 내부 오류가 발생했습니다. |
| `503 Service Unavailable`| - | 외부 서비스(예: 인가 서버)와 통신에 실패했습니다. |

---

## 5. 테스트 API

**본 항목의 API들은 개발 및 테스트 목적으로만 사용되어야 합니다.**

### 5.1. 테스트 페이지

- **URL**: `/oid4vci/test`
- **Method**: `GET`
- **설명**: Credential 발급 흐름을 테스트할 수 있는 웹 페이지를 제공합니다. QR 코드 생성 및 발급 과정을 시각적으로 확인할 수 있습니다.

