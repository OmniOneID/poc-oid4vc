# OID4VP Verifier API 문서

- Subject: OID4VP Verifier API Document
- Writer: 이수현
- Date: 2025-11-13
- Version: v1.0.0

| Version | Date | History |
| --- | --- | --- |
| v1.0.0 | 2025-11-13 | 초기 작성 |

## 목차

1. [개요](#1-개요)
2. [API 목록](#2-api-목록)
3. [API 상세 설명](#3-api-상세-설명)
    - [3.1. 검증 세션 시작](#31-검증-세션-시작)
    - [3.2. 인가 요청 조회](#32-인가-요청-조회)
    - [3.3. VP Token 수신](#33-vp-token-수신)
    - [3.4. DCQL 검증](#34-dcql-검증)
4. [에러 코드](#4-에러-코드)
5. [테스트 API](#5-테스트-api)

---

## 1. 개요

본 문서는 [OpenID for Verifiable Presentations (OID4VP) 1.0](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html) 표준을 준수하는 Verifier 서비스의 API를 정의합니다. Verifier는 Wallet으로부터 Verifiable Presentation (VP Token)을 수신하고 검증하는 주체입니다.

본 Verifier 서비스는 다음 기능을 지원합니다:

- **검증 세션 관리**: DCQL(Digital Credentials Query Language) 또는 OAuth2 scope를 기반으로 검증 세션 생성
- **인가 요청 처리**: Request URI 또는 인라인 파라미터를 통한 OpenID4VP 인가 요청 생성
- **응답 모드 지원**: `direct_post`, `query` 응답 모드 지원
- **VP Token 검증**: Verifiable Presentation 토큰의 구조, 서명 및 Holder Binding 검증
- **DCQL 검증**: 요청된 자격증명 쿼리의 유효성 검증 (sdjwt SDK 기반)
- **세션 관리**: 임시 세션 저장소를 통한 상태 추적 (프로덕션 환경에서는 Redis/DB 권장)

---

## 2. API 목록

| API | Method | URL | 설명 |
| --- | --- | --- | --- |
| `검증 세션 시작` | POST | `/oid4vp/initiate` | DCQL 또는 scope를 기반으로 검증 세션을 생성하고 OpenID4VP 인가 요청 URI를 반환 |
| `인가 요청 조회` | GET | `/oid4vp/request/{request_id}` | Request URI를 통해 요청된 인가 요청 객체(Authorization Request)를 조회 (By Reference 방식) |
| `VP Token 수신` | POST/GET | `/oid4vp/response` | Wallet으로부터 VP Token을 수신하고 검증 |
| `DCQL 검증` | POST | `/oid4vp/validate-dcql` | DCQL 쿼리의 형식 및 규칙 유효성을 검증 |
| `테스트 페이지` | GET | `/oid4vp/test` | Verifier 기능을 테스트하기 위한 웹 페이지 제공 |

---

## 3. API 상세 설명

### 3.1. 검증 세션 시작

- **URL**: `/oid4vp/initiate`
- **Method**: `POST`
- **설명**: 검증을 위한 세션을 생성하고 OpenID4VP 인가 요청 정보를 반환합니다. DCQL 쿼리 또는 OAuth2 scope 중 하나를 제공하여 Wallet이 어떤 자격증명을 제공해야 하는지 지정합니다. `use_request_uri` 파라미터에 따라 By Reference(request_uri 사용) 또는 By Value(인라인 파라미터) 방식으로 요청을 전달할 수 있습니다.

#### 요청 예시 (DCQL을 사용한 경우)

```shell
curl -X POST "http://${Host}:8081/oid4vp/initiate" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d 'dcql_query={"credentials":[{"id":"StudentID"}]}&response_mode=direct_post&use_request_uri=false'
```

#### 요청 예시 (Scope를 사용한 경우)

```shell
curl -X POST "http://${Host}:8081/oid4vp/initiate" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d 'scope=StudentID&response_mode=direct_post&use_request_uri=true'
```

#### 응답 예시 (By Value 방식)

```json
{
  "authorization_request_uri": "openid4vp://?response_type=vp_token&client_id=redirect_uri%3Ahttp%3A%2F%2Flocalhost%3A8081%2Foid4vp%2Fcallback&response_mode=direct_post&nonce=550e8400-e29b-41d4-a716-446655440000&state=a1b2c3d4e5f6g7h8&redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Foid4vp%2Fcallback&dcql_query=%7B%22credentials%22%3A%5B%7B%22id%22%3A%22StudentID%22%7D%5D%7D&client_metadata=%7B%22client_name%22%3A%22OID4VP%20Verifier%22%7D",
  "method": "by_value",
  "request_id": "550e8400-e29b-41d4-a716-446655440001",
  "state": "a1b2c3d4e5f6g7h8",
  "nonce": "550e8400-e29b-41d4-a716-446655440000",
  "response_mode": "direct_post",
  "dcql_validated": true,
  "credential_count": 1,
  "query_source": "direct",
  "use_request_uri": false,
  "client_id": "redirect_uri:http://localhost:8081/oid4vp/callback",
  "client_metadata": {
    "client_name": "OID4VP Verifier",
    "client_id": "redirect_uri:http://localhost:8081/oid4vp/callback",
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

#### 응답 예시 (By Reference 방식)

```json
{
  "authorization_request_uri": "openid4vp://?request_uri=http%3A%2F%2Flocalhost%3A8081%2Foid4vp%2Frequest%2F550e8400-e29b-41d4-a716-446655440001&client_id=redirect_uri%3Ahttp%3A%2F%2Flocalhost%3A8081%2Foid4vp%2Fcallback",
  "request_uri": "http://localhost:8081/oid4vp/request/550e8400-e29b-41d4-a716-446655440001",
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
  "client_id": "redirect_uri:http://localhost:8081/oid4vp/callback",
  "client_metadata": {
    "client_name": "OID4VP Verifier",
    "client_id": "redirect_uri:http://localhost:8081/oid4vp/callback",
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

### 3.2. 인가 요청 조회

- **URL**: `/oid4vp/request/{request_id}`
- **Method**: `GET`
- **설명**: Request URI를 통해 요청된 인가 요청 객체를 조회합니다(By Reference 방식). Wallet은 이를 통해 `request_uri` 파라미터만으로 전체 인가 요청 정보를 동적으로 조회할 수 있습니다. 요청은 `request_uri_ttl` 설정값(기본 5분)에 따라 만료됩니다.

#### 요청 예시

```shell
curl -X GET "http://${Host}:8081/oid4vp/request/550e8400-e29b-41d4-a716-446655440001"
```

#### 응답 예시

```json
{
  "response_type": "vp_token",
  "client_id": "redirect_uri:http://localhost:8081/oid4vp/callback",
  "response_mode": "direct_post",
  "nonce": "550e8400-e29b-41d4-a716-446655440000",
  "state": "a1b2c3d4e5f6g7h8",
  "response_uri": "http://localhost:8081/oid4vp/response",
  "dcql_query": "{\"credentials\":[{\"id\":\"StudentID\"}]}",
  "client_metadata": {
    "client_name": "OID4VP Verifier",
    "client_id": "redirect_uri:http://localhost:8081/oid4vp/callback",
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

### 3.3. VP Token 수신

- **URL**: `/oid4vp/response`
- **Method**: `POST`, `GET`
- **설명**: Wallet으로부터 VP Token을 수신하고 검증합니다. `direct_post` 응답 모드에서는 POST 요청으로, `query` 응답 모드에서는 GET 요청으로 수신합니다. 검증 과정에서 토큰의 구조, 서명, Holder Binding 등을 검증합니다.

#### 요청 예시 (Direct Post - POST)

```shell
curl -X POST "http://${Host}:8081/oid4vp/response" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d 'vp_token=eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9...&state=a1b2c3d4e5f6g7h8'
```

#### 요청 예시 (Query Mode - GET)

```shell
curl -X GET "http://${Host}:8081/oid4vp/response?vp_token=eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9...&state=a1b2c3d4e5f6g7h8"
```

#### 응답 예시 (성공)

```json
// 200 응답
```

#### 응답 예시 (실패)

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

### 3.4. DCQL 검증

- **URL**: `/oid4vp/validate-dcql`
- **Method**: `POST`
- **설명**: DCQL(Digital Credentials Query Language) 쿼리의 형식 및 규칙 유효성을 검증합니다. sdjwt SDK의 DCQLQueryValidator를 사용하여 표준 준수 여부를 확인합니다. 이 엔드포인트는 클라이언트가 쿼리를 미리 검증할 수 있도록 제공됩니다.

#### 요청 본문

DCQL 쿼리 JSON 객체를 요청 본문으로 전달합니다.

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

#### 요청 예시

```shell
curl -X POST "http://${Host}:8081/oid4vp/validate-dcql" \
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

#### 응답 예시 (유효함)

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

#### 응답 예시 (유효하지 않음)

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

## 4. 에러 코드

| HTTP Status | 에러 코드 (JSON Body) | 설명 |
| --- | --- | --- |
| `400 Bad Request` | `invalid_request` | 요청이 잘못되었거나 필수 파라미터가 누락되었습니다. |
| `400 Bad Request` | `invalid_request_uri` | request_uri가 유효하지 않거나 만료되었습니다. |
| `400 Bad Request` | `invalid_scope` | 제공된 scope에 대한 DCQL 매핑이 없습니다. |
| `400 Bad Request` | `invalid_token` | VP Token의 형식이 잘못되었거나 검증에 실패했습니다. |
| `401 Unauthorized` | - | 인증이 필요합니다. (해당 API에 따라) |
| `403 Forbidden` | - | 요청이 거부되었습니다. |
| `404 Not Found` | - | 요청한 리소스를 찾을 수 없습니다. (예: request_id 세션 만료) |
| `500 Internal Server Error` | `server_error` | 서버 내부 오류가 발생했습니다. |
| `500 Internal Server Error` | `processing_error` | 요청 처리 중 오류가 발생했습니다. |

#### 에러 응답 예시

```json
{
  "error": "invalid_request",
  "error_description": "Either dcql_query or scope parameter is required",
  "state": "a1b2c3d4e5f6g7h8"
}
```

---

## 5. 테스트 API

**본 항목의 API들은 개발 및 테스트 목적으로만 사용되어야 합니다.**

### 5.1. 테스트 페이지

- **URL**: `/oid4vp/test`
- **Method**: `GET`
- **설명**: Verifier의 검증 기능을 테스트할 수 있는 웹 페이지를 제공합니다. DCQL 쿼리 생성, 세션 시작 등을 할 수 있습니다.