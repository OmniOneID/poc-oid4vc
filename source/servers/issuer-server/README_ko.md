# Issuer Server 소스 코드

Issuer Server 소스 코드 리포지토리에 오신 것을 환영합니다. 이 디렉토리에는 Issuer Server의 핵심 소스 코드와 빌드 구성이 포함되어 있습니다.

## 디렉토리 구조

디렉토리 구조에 대한 개요는 다음과 같습니다.

```
issuer-server
├── did-oid4vci-sdk-server           # OID4VCI SDK (핵심 로직)
├── did-oid4vc-authorization-sdk-server # 인가(Authorization) SDK
├── libs                             # 공통 라이브러리
├── metadata                         # 발급자 메타데이터 파일 (JSON)
├── src                              # 발급자 예제 애플리케이션 소스 코드
│   ├── main
│   │   ├── java                     # Java 소스 코드 (com.example.did.oid4vc.issuer)
│   │   └── resources                # 설정 파일 (application.yml, DB 스크립트 등)
├── build.gradle                     # 프로젝트 빌드 설정
└── README.md
```

## 디렉토리 상세 설명

| 이름 | 설명 |
|------|-------------|
| `did-oid4vci-sdk-server` | 핵심 OID4VCI SDK 로직을 포함하는 서브 프로젝트 |
| `did-oid4vc-authorization-sdk-server` | 인가 서버 연동을 위한 서브 프로젝트 |
| `libs` | 서명에 사용되는 `did-wallet-sdk-server-2.0.0.jar` 포함 |
| `metadata` | 다양한 발급자 메타데이터 설정(local, dev 등) 디렉토리 |
| `src/main/resources` | 애플리케이션 프로퍼티 및 UI 템플릿 포함 |

## 라이브러리

### 1. Open DID 라이브러리
SDK 및 애플리케이션에서 사용하는 핵심 라이브러리입니다:
- `did-wallet-sdk-server-2.0.0.jar` (`libs/` 폴더)
- `did-crypto-sdk-server-2.0.0.jar` (`did-oid4vci-sdk-server/libs/` 폴더)
- `did-datamodel-sdk-server-2.0.0.jar` (`did-oid4vci-sdk-server/libs/` 폴더)
- `did-sdk-common-2.0.0.jar` (`did-oid4vci-sdk-server/libs/` 폴더)
- `sd-jwt-sdk-vc-1.0.0.jar` (`did-oid4vci-sdk-server/libs/` 폴더)

### 2. 외부 라이브러리
Gradle을 통해 관리되는 주요 종속성입니다:
- Spring Boot 3.2.4
- Spring Security & OAuth2 Authorization Server
- Spring Data JPA & PostgreSQL
- Liquibase (DB 스키마 관리)
- OpenFeign (HTTP 클라이언트)
- Google ZXing (QR 코드 생성)

## 문서

자세한 내용은 다음 문서를 참조하십시오.

- [Issuer Server API 참조](../../../docs/api/issuer-server/issuer_server_API_ko.md)
  Issuer Server API의 참조 구현에 대한 가이드입니다.

## 기여

우리의 행동 강령 및 우리에게 풀 리퀘스트를 제출하는 절차에 대한 자세한 내용은 루트 디렉토리의 `CONTRIBUTING.md` 및 `CODE_OF_CONDUCT.md`를 읽어보십시오.

## 라이선스
이 프로젝트는 Apache License 2.0에 따라 라이선스가 부여됩니다.
## 연락처
질문이나 지원이 필요한 경우 루트 디렉토리의 `maintainers`에게 문의하십시오.
