# SD-JWT SDK 소스 코드

SD-JWT SDK 소스 코드 리포지토리에 오신 것을 환영합니다. 이 디렉토리에는 SD-JWT SDK의 핵심 소스 코드와 빌드 구성이 포함되어 있습니다.

## 디렉토리 구조

디렉토리 구조에 대한 개요는 다음과 같습니다.

```
sd-jwt-sdk
├── gradle
├── libs
    └── did-crypto-sdk-server-2.0.0.jar
    └── did-wallet-sdk-server-2.0.0.jar
├── src
└── build.gradle
└── README.md
```

<br/>

아래는 디렉토리의 각 폴더 및 파일에 대한 설명입니다.

| 이름                    | 설명                                            |
| ----------------------- | ----------------------------------------------- |
| sd-jwt-sdk           | SD-JWT SDK 소스 코드 및 빌드 파일            |
| ┖ gradle                | Gradle 빌드 구성 및 스크립트                    |
| ┖ libs                  | 외부 라이브러리 및 종속성                       |
| ┖ src                   | 메인 소스 코드 디렉토리                         |
| ┖ build.gradle          | Gradle 빌드 구성 파일                           |
| ┖ README.md             | 소스 코드에 대한 개요 및 지침                   |


## 라이브러리

이 프로젝트에서 사용되는 라이브러리는 두 가지 주요 범주로 구성됩니다.

1. **Open DID 라이브러리**: 이 라이브러리들은 Open DID 프로젝트에 의해 개발되었으며 [libs 폴더](libs)에서 사용할 수 있습니다. 여기에는 다음이 포함됩니다.

    - `did-crypto-sdk-server-2.0.0.jar`
    - `did-wallet-sdk-server-2.0.0.jar`

2. **타사 라이브러리**: 이 라이브러리들은 [build.gradle](build.gradle) 파일을 통해 관리되는 오픈 소스 종속성입니다. 타사 라이브러리 및 해당 라이선스의 자세한 목록은 루트 디렉토리의 `dependencies-license.md` 파일을 참조하십시오.


## 문서

자세한 내용은 다음 문서를 참조하십시오.

- [SD-JWT SDK API 참조](../../../docs/api/sd-jwt-sdk/sd-jwt-sdk_API_ko.md)
  SD-JWT SDK API의 참조 구현에 대한 가이드입니다.

## 기여

우리의 행동 강령 및 우리에게 풀 리퀘스트를 제출하는 절차에 대한 자세한 내용은 루트 디렉토리의 `CONTRIBUTING.md` 및 `CODE_OF_CONDUCT.md`를 읽어보십시오.

## 라이선스
이 프로젝트는 Apache License 2.0에 따라 라이선스가 부여됩니다.

## 연락처
질문이나 지원이 필요한 경우 루트 디렉토리의 `maintainers`에게 문의하십시오.
