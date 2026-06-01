# OID4VCI 메타데이터와 보유 증명

| 항목 | 내용 |
|------|------|
| 주제 | Issuer 메타데이터, Proof of Possession, 자격증명 요청/응답 구조 |
| 작성 | 오픈소스개발팀 |
| 일자 | 2026-06-01 |
| 버전 | v1.0.0 |

## 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|-----------|
| v1.0.0 | 2026-06-01 | 초기 작성 |

## 목차

1. [Issuer Metadata](#1-issuer-metadata)
2. [Credential Configuration](#2-credential-configuration)
3. [Proof of Possession](#3-proof-of-possession)
4. [Credential Request](#4-credential-request)
5. [Credential Response](#5-credential-response)
6. [지원 포맷](#6-지원-포맷)

---

## 1. Issuer Metadata

지갑은 자격증명을 요청하기 전에, 먼저 Issuer가 "무엇을, 어디서, 어떻게" 발급하는지 알아야 한다.
이 정보는 **Issuer Metadata**로 공개된다. 표준화된 well-known URL에서 조회할 수 있다.

```
GET https://issuer.example.com/.well-known/openid-credential-issuer
```

메타데이터에는 다음과 같은 핵심 정보가 담긴다.

| 필드 | 설명 |
|------|------|
| `credential_issuer` | Issuer 식별자(기준 URL) |
| `credential_endpoint` | 자격증명 발급 엔드포인트 |
| `nonce_endpoint` | c_nonce 발급 엔드포인트 |
| `deferred_credential_endpoint` | 지연 발급 엔드포인트 |
| `notification_endpoint` | 통지 엔드포인트 |
| `credential_configurations_supported` | 발급 가능한 자격증명 종류 정의(아래 참고) |

> Authorization Server에 대한 정보(`token_endpoint`, 지원 grant 등)는
> 별도의 well-known 메타데이터(`/.well-known/oauth-authorization-server`)로 제공된다.

---

## 2. Credential Configuration

`credential_configurations_supported`는 발급 가능한 자격증명을 종류별로 기술한 목록이다.
각 항목은 고유한 식별자(예: `org.iso.18013.5.1.mDL`)를 키로 가진다.

```jsonc
{
  "credential_configurations_supported": {
    "eu.europa.ec.eudi.pid_vc_sd_jwt": {
      "format": "dc+sd-jwt",          // 자격증명 포맷
      "vct": "...",                    // SD-JWT VC 타입
      "cryptographic_binding_methods_supported": ["jwk"],
      "credential_signing_alg_values_supported": ["ES256"],
      "proof_types_supported": { "jwt": { "proof_signing_alg_values_supported": ["ES256"] } },
      "claims": [ /* 발급 가능한 클레임 정의 */ ]
    }
  }
}
```

지갑은 이 정의를 보고 다음을 판단한다.
- 어떤 **포맷**(SD-JWT VC, mDoc 등)으로 발급되는가
- 어떤 **서명 알고리즘**을 지원하는가
- proof를 만들 때 어떤 **proof 타입/알고리즘**을 써야 하는가
- 어떤 **클레임**이 포함될 수 있는가

---

## 3. Proof of Possession

발급의 핵심 보안 장치는 **보유 증명(Proof of Possession)** 이다.
지갑은 자신의 키쌍을 보유하고 있음을, 그 키로 서명한 **proof(JWT)** 로 증명한다.
발급된 자격증명은 이 키에 바인딩되어, 이후 제시 단계에서 정당한 보유자임을 증명할 수 있다.

proof로 사용되는 JWT의 구조는 다음과 같다.

```jsonc
// Header
{
  "typ": "openid4vci-proof+jwt",
  "alg": "ES256",
  "jwk": { /* 지갑의 공개키 */ }   // 또는 kid
}
// Payload
{
  "iss": "<wallet client id>",     // 선택
  "aud": "https://issuer.example.com",  // 대상 Issuer
  "iat": 1711843200,
  "nonce": "<c_nonce>"             // Issuer가 발급한 일회성 nonce
}
```

핵심 포인트
- **`nonce`(c_nonce)**: Issuer가 발급한 일회성 난수를 포함해야 재전송 공격을 막을 수 있다.
- **`aud`**: 이 proof가 어느 Issuer를 향한 것인지 명시하여, 다른 곳에 재사용되지 못하게 한다.
- **공개키(`jwk`)**: 발급될 자격증명이 바인딩될 키다.

```mermaid
sequenceDiagram
    participant W as Wallet
    participant I as Issuer
    W->>I: Nonce 요청
    I-->>W: c_nonce
    Note over W: 개인키로 proof(JWT) 서명<br/>(nonce, aud 포함)
    W->>I: Credential 요청 (proof)
    Note over I: proof 서명 검증 + nonce 유효성 확인
    I-->>W: 공개키에 바인딩된 VC
```

---

## 4. Credential Request

지갑이 자격증명을 요청할 때 Credential Endpoint로 보내는 요청이다.
Access Token을 `Authorization` 헤더에 담고, 본문에 발급 대상과 proof를 포함한다.

```http
POST /credential HTTP/1.1
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "credential_configuration_id": "eu.europa.ec.eudi.pid_vc_sd_jwt",
  "proofs": {
    "jwt": ["<proof-jwt>"]
  }
}
```

| 필드 | 설명 |
|------|------|
| `credential_configuration_id` | 발급받을 자격증명 종류 식별자 |
| `proofs` | 보유 증명 묶음. proof 타입별로 하나 이상 제출 가능 |

---

## 5. Credential Response

발급이 성공하면 Issuer는 자격증명을 반환한다.

```jsonc
{
  "credentials": [
    { "credential": "<발급된 자격증명>" }
  ]
}
```

즉시 발급이 불가능하면, 자격증명 대신 `transaction_id`를 반환하고
지갑은 이후 [Deferred Endpoint](oid4vci_issuance_flow.md#5-비동기-발급-deferred)로 재요청한다.

```jsonc
{
  "transaction_id": "8xLOxBtZp8"
}
```

---

## 6. 지원 포맷

OID4VCI는 자격증명 포맷에 독립적이며, 동일한 발급 절차로 여러 포맷을 다룰 수 있다.
대표적인 포맷은 다음과 같다.

| 포맷 식별자 | 설명 | 상세 문서 |
|------------|------|----------|
| `dc+sd-jwt` | SD-JWT 기반 VC. 선택적 공개 지원 | [SD-JWT VC 개요](../sdjwt/sdjwt_overview.md) |
| `mso_mdoc` | ISO 18013-5 mDoc. CBOR/COSE 기반 | [mDoc 개요](../mdoc/mdoc_overview.md) |

포맷이 달라도 지갑–Issuer 간의 토큰 발급, proof 제출, Credential 요청/응답 절차의 **뼈대는 동일**하다.
달라지는 것은 발급되는 자격증명의 **내부 표현 방식**뿐이다.

> 발급된 자격증명을 검증자에게 "제시"하는 절차는 [OID4VP](../oid4vp/oid4vp_overview.md)에서 다룬다.
