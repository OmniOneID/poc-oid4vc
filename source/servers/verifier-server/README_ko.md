# Verifier Server 소스 코드

Verifier Server 소스 코드 리포지토리에 오신 것을 환영합니다. 이 디렉토리에는 Verifier Server의 핵심 소스 코드와 빌드 구성이 포함되어 있습니다.

## 디렉토리 구조

디렉토리 구조에 대한 개요는 다음과 같습니다.

```
verifier-server
├── libs                               # SDK JAR 라이브러리
├── src                                # 검증자 예제 애플리케이션 소스 코드
│   ├── main
│   │   ├── java                       # Java 소스 코드 (com.example.did.oid4vc.verifier)
│   │   └── resources                  # 설정 파일 (application.yml, DB 스크립트 등)
├── build.gradle                       # 프로젝트 빌드 설정
└── README.md
```

## 디렉토리 상세 설명

| 이름 | 설명 |
|------|-------------|
| `libs` | 런타임에 `fileTree`로 사용되는 SDK JAR 라이브러리 |
| `src/main/resources` | 애플리케이션 프로퍼티 및 UI 템플릿 포함 |

## 라이브러리

### 1. Open DID 라이브러리
`libs/` 디렉토리에 포함된 핵심 라이브러리입니다:
- `did-wallet-sdk-server-2.0.0.jar`
- `did-crypto-sdk-server-2.0.0.jar`
- `did-oid4vp-sdk-server-3.0.0.jar`

### 2. 외부 라이브러리
Gradle을 통해 관리되는 주요 종속성입니다:
- Spring Boot 3.2.4
- Spring Data JPA & PostgreSQL
- Liquibase (DB 스키마 관리)
- OpenFeign (HTTP 클라이언트)
- Bouncy Castle (암호화)
- Nimbus JOSE+JWT

## 문서

자세한 내용은 다음 문서를 참조하십시오.

- [Verifier Server API 참조](../../../docs/api/verifier-server/verifier_server_API_ko.md)
  Verifier Server API의 참조 구현에 대한 가이드입니다.
- [OID4VP SDK 통합 가이드](../../../docs/api/verifier-server/OID4VP_SDK-INTEGRATION_GUIDE_ko.md)
- [OID4VP SDK API 참조](../../../docs/api/verifier-server/OID4VP_SDK-SERVER_API_ko.md)
- [OID4VP SDK 에러 코드](../../../docs/api/verifier-server/OID4VPSDKError.md)
- [Formatter SDK 에러 코드](../../../docs/api/verifier-server/FormatterSDKError.md)

## 기여

우리의 행동 강령 및 우리에게 풀 리퀘스트를 제출하는 절차에 대한 자세한 내용은 루트 디렉토리의 `CONTRIBUTING.md` 및 `CODE_OF_CONDUCT.md`를 읽어보십시오.

## 라이선스
이 프로젝트는 Apache License 2.0에 따라 라이선스가 부여됩니다.

## 연락처
질문이나 지원이 필요한 경우 루트 디렉토리의 `maintainers`에게 문의하십시오.
