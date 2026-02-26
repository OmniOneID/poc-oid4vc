# OID4VC PoC 프로젝트 설치 및 구동 가이드

본 문서는 OID4VC PoC 프로젝트를 설치하고 구동하는 방법을 설명합니다. 아래 단계를 따라 진행하시면 됩니다.

---

## 1. 시스템 요구 사항

OID4VC PoC 프로젝트를 설치 및 구동하기 위해서는 아래 요구사항이 충족되어야 합니다.

- **MacOS or Linux**
- **Java 21**
- **Gradle 7.0 이상** (Gradle Wrapper 포함)
- **Git 설치**
- **Bash 지원**

### 선택 사항 (모바일 앱 테스트 시)
- **Android Studio 및 Android SDK** (Android 앱 테스트 시)
- **Xcode 및 iOS SDK** (iOS 앱 테스트 시)

---

## 2. 프로젝트 Clone

OID4VC PoC 프로젝트를 로컬 환경에 클론합니다. 아래 명령어를 터미널에 입력하여 프로젝트를 다운로드하세요.

```bash
git clone https://github.com/OmniOneID/poc-oid4vc.git
cd poc-oid4vc
```

---

## 3. 프로젝트 구조

OID4VC PoC 프로젝트는 다음과 같은 구조로 구성되어 있습니다:

```
poc-oid4vc
├── source/
│   ├── apps/                          # 테스트용 모바일 앱
│   │   ├── android-app/               # Android 지갑 앱
│   │   └── ios-app/                   # iOS 지갑 앱
│   └── servers/                       # 백엔드 서버
│       ├── issuer-server/             # Issuer 서버 (Authorization 포함)
│       └── verifier-server/           # Verifier 서버
├── docs/                              # 문서 폴더
│   └── api/
│       ├── issuer-server/             # Issuer SDK 연동 가이드 및 API 문서
│       └── verifier-server/           # Verifier SDK 연동 가이드 및 API 문서
```

---

## 4. 백엔드 서버 구동

OID4VC PoC 프로젝트는 2개의 백엔드 서버로 구성되어 있습니다. 각 서버는 Spring Boot 기반으로 구현되어 있으며, SDK를 서브모듈로 포함합니다.

| 서버 | 역할 | 포함 SDK |
| :--- | :--- | :--- |
| **Issuer Server** | OID4VCI 표준에 따른 VC 발급 및 OAuth 2.0 인가 | `did-oid4vci-sdk-server`, `did-oid4vc-authorization-sdk-server`, `did-oid4vc-formatter-sdk-server` |
| **Verifier Server** | OID4VP 표준에 따른 VP 검증 | `did-oid4vp-sdk-server`, `did-oid4vc-formatter-sdk-server` |

> **참고**: 각 서버의 상세 설정(application.yml, 메타데이터, DB 구성, SDK 연동 등)은 아래 **SDK 연동 가이드**를 참조하세요.
> - Issuer Server: [OID4VCI SDK 적용 가이드](../api/issuer-server/OID4VCI_SDK-INTEGRATION_GUIDE_ko.md)
> - Verifier Server: [OID4VP SDK 연동 가이드](../api/verifier-server/OID4VP_SDK-INTEGRATION_GUIDE_ko.md)

### 4.1. 공통 구성 요소

모든 서버를 구동하기 전에 소스 폴더로 이동합니다:

```bash
cd source/servers
```

### 4.2. Issuer Server 구동

**역할**: OID4VCI 표준에 따라 VC(Verifiable Credential)를 발급하고, OAuth 2.0 기반의 인증/인가를 관리합니다.

#### 4.2.1. IDE를 사용하여 구동 (권장)

1. IntelliJ IDEA 또는 Eclipse 등의 IDE에서 `source/servers/issuer-server` 폴더를 프로젝트로 엽니다.
2. IDE의 Gradle 빌드 시스템이 자동으로 의존성을 다운로드합니다.
3. `application.yml`의 프로파일 설정 및 상세 구성을 확인합니다.
   - 상세 설정 방법은 [OID4VCI SDK 적용 가이드](../api/issuer-server/OID4VCI_SDK-INTEGRATION_GUIDE_ko.md)를 참조하세요.
4. 실행 구성(Run Configuration)을 다음과 같이 설정합니다:
   - Main Class: `com.example.did.oid4vc.issuer.IssuerApplication`
   - Working directory: `source/servers/issuer-server`
5. 실행 버튼을 클릭하여 서버를 시작합니다.
6. 콘솔에서 "Started IssuerApplication in ... seconds" 메시지를 확인하면 정상 구동입니다.

#### 4.2.2. 콘솔 명령어로 구동

```bash
cd issuer-server

# Gradle Wrapper 실행 권한 부여
chmod 755 ./gradlew

# 프로젝트 빌드
./gradlew clean build

# JAR 파일 실행
java -jar build/libs/issuer-server-1.0.0.jar
```

**기본 포트**: `8080`

#### 4.2.3. 브라우저에서 접속

Issuer Server가 정상적으로 구동되면, 브라우저에서 아래 주소로 접속합니다.

```
http://<현재_IP>:8080/oid4vci/test
```

예를 들어, 로컬 환경에서 실행 중이라면:

```
http://localhost:8080/oid4vci/test
```

아래와 같이 초기 테스트 페이지를 확인할 수 있습니다.

<img src="./images/issuer-initial.png" width="500"/>

### 4.3. Verifier Server 구동

**역할**: OID4VP 표준에 따라 VP(Verifiable Presentation)를 검증합니다.

#### 4.3.1. IDE를 사용하여 구동 (권장)

1. 별도의 IDE 창 또는 창 탭에서 `source/servers/verifier-server` 폴더를 프로젝트로 엽니다.
2. IDE의 Gradle 빌드 시스템이 자동으로 의존성을 다운로드합니다.
3. `application.yml`의 프로파일 설정 및 상세 구성을 확인합니다.
   - 상세 설정 방법은 [OID4VP SDK 연동 가이드](../api/verifier-server/OID4VP_SDK-INTEGRATION_GUIDE_ko.md)를 참조하세요.
4. 실행 구성(Run Configuration)을 다음과 같이 설정합니다:
   - Main Class: `com.example.did.oid4vc.verifier.VerifierApplication`
   - Working directory: `source/servers/verifier-server`
5. 실행 버튼을 클릭하여 서버를 시작합니다.
6. 콘솔에서 "Started VerifierApplication in ... seconds" 메시지를 확인하면 정상 구동입니다.

#### 4.3.2. 콘솔 명령어로 구동

```bash
cd verifier-server

# Gradle Wrapper 실행 권한 부여
chmod 755 ./gradlew

# 프로젝트 빌드
./gradlew clean build

# JAR 파일 실행
java -jar build/libs/verifier-example-server-3.0.0.jar
```

**기본 포트**: `8081`

#### 4.3.3. 브라우저에서 접속

Verifier Server가 정상적으로 구동되면, 브라우저에서 아래 주소로 접속합니다.

```
http://<현재_IP>:8081/oid4vp/test
```

예를 들어, 로컬 환경에서 실행 중이라면:

```
http://localhost:8081/oid4vp/test
```

아래와 같이 초기 테스트 페이지를 확인할 수 있습니다.

<img src="./images/verifier-initial.png" width="500"/>

---

## 5. 테스트 앱 구동

### 5.1. Android 앱 구동

#### 사전 요구 사항
- Android Studio 설치
- Android SDK 설치 (최소 API 레벨 21 이상)
- Android 에뮬레이터 또는 실제 Android 디바이스

#### 구동 방법

1. Android Studio를 실행합니다.
2. **File → Open**을 선택하여 `source/apps/android-app` 폴더를 엽니다.
3. Android Studio가 Gradle 프로젝트를 로드하고 의존성을 다운로드할 때까지 기다립니다.
4. 상단 메뉴에서 초록색 ▶ 버튼을 클릭합니다.
5. 에뮬레이터 또는 연결된 디바이스를 선택합니다.
6. 앱이 설치되고 실행됩니다.

#### 초기 화면
아래와 같이 초기 앱 화면을 확인할 수 있습니다.

<img src="./images/android-app-initial.png" width="200"/>

#### API 테스트 서버 연결 설정

Android 앱에서 API 테스트를 위해 백엔드 서버에 연결하려면:

일반적으로 다음과 같이 설정합니다:
   - Issuer Server: `http://10.0.2.2:8080`

> **팁**: 에뮬레이터에서 호스트 머신의 localhost에 접근하려면 `10.0.2.2`를 사용합니다.

> **주의사항**: 백엔드 서버의 설정에서 서버들의 기본 아이피를 `10.0.2.2`로 변경해야 합니다.

### 5.2. iOS 앱 구동

#### 사전 요구 사항
- Xcode 설치 (macOS에서만 가능)
- iOS SDK 설치
- iOS 시뮬레이터 또는 실제 iOS 디바이스

#### 구동 방법

1. Xcode를 실행합니다.
2. **File → Open**을 선택하여 `source/apps/ios-app` 폴더를 엽니다.
3. Xcode 창에서 프로젝트를 열 때 `.xcodeproj` 또는 `.xcworkspace` 파일을 선택합니다.
4. 상단 스킴(Scheme) 선택 영역에서 대상 시뮬레이터 또는 디바이스를 선택합니다.
5. **Product → Run**을 선택하거나 Command + R을 누릅니다.
6. 앱이 빌드되어 시뮬레이터 또는 디바이스에서 실행됩니다.

#### 초기 화면
아래와 같이 초기 앱 화면을 확인할 수 있습니다.

<img src="./images/ios-app-initial.png" width="200"/>

#### API 테스트 서버 연결 설정

iOS 앱에서 API 테스트를 위해 백엔드 서버에 연결하려면:

일반적으로 다음과 같이 설정합니다:
   - Issuer Server: `http://localhost:8080`

> **팁**: iOS 시뮬레이터에서 호스트 머신의 localhost에 접근하려면 `localhost` 또는 `127.0.0.1`을 직접 사용할 수 있습니다.

> **주의사항**: 백엔드 서버의 설정에서 서버들의 기본 아이피를 `localhost` 또는 `127.0.0.1`로 변경해야 합니다.

---

## 6. OID4VC 테스트 절차

백엔드 서버 및 테스트 앱의 구동이 완료되었으면 이제 OID4VC 테스트를 수행할 수 있습니다.
Android 앱을 기준으로 아래와 같은 절차로 테스트를 수행할 수 있습니다.

### 6.1. OID4VCI 테스트 절차

OID4VCI는 크게 `Authorization Code`와 `Pre-Authorized Code` Flow를 통해 동작됩니다. `Pre-Authorized Code` Flow 기준으로 아래와 같은 절차를 통해 OID4VCI 테스트를 수행할 수 있습니다.

#### 6.1.1. Issuer Server에서 Credential Offer 생성

1. 브라우저에서 아래 주소로 접속합니다.

```groovy
http://<현재_IP>:8080/oid4vci/test

// 예를 들어 로컬 환경에서 실행 중이라면 http://localhost:8080/oid4vci/test
```

2. 아래와 같이 초기 테스트 페이지가 출력되며, `Credential Offer` 생성을 위해 `User ID`를 임의로 입력한 후 `Generate QR Code`를 누릅니다.

<img src="./images/oid4vci-generate-qr-code.png" width="700"/>

3. 이후 아래와 같이 `Credential Offer`에 해당하는 QR Code가 생성됩니다. 단, 테스트 앱이 애뮬레이터에서 실행 시 QR 스캔이 원활하지 않을 수 있으므로, 하단의 `Full Offer URI (qrData)` 아래에 있는 데이터를 복사합니다.

<img src="./images/oid4vci-credential-offer.png" width="700"/>

#### 6.1.2. Start Issuance

1. 안드로이드 애뮬레이터에서 실행되고 있는 테스트 앱 맨 하단의 `Enter QR Code Data` 아래에 복사한 데이터를 붙여넣기 한 후 `Issue`를 누릅니다.

<img src="./images/oid4vci-enter-qr-code-data.png" width="300"/>

2. `Please register a PIN` 화면이 출력되면, Issuer 테스트 페이지에 있는 4자리의 `Tx Code`를 그대로 입력합니다.

<img src="./images/oid4vci-enter-pin.png" width="300"/>

3. 이후 `Select Issued Credential` 팝업이 출력되면, `NationalID`를 선택한 후 `CONFIRM`을 누릅니다. (`SD-JWT VC` 발급)

<img src="./images/oid4vci-select-issued-credential.png" width="300"/>

4. 이후 VC 발급이 완료됩니다.

<img src="./images/oid4vci-issuance-completed.png" width="300"/>

#### 6.1.3. View VC

1. 테스트 앱에서 `View VC`를 클릭합니다.

<img src="./images/android-app-initial.png" width="300"/>

2. 아래와 같이 발급된 VC에 대한 클레임 등의 정보가 정상적으로 출력됩니다.

<img src="./images/oid4vci-view-vc.png" width="300"/>

3. VC 발급이 정상적으로 되지 않았거나, VC를 발급하기 이전에 `View VC`를 누르면 아래와 같은 알림 및 오류 화면이 출력됩니다.

<img src="./images/oid4vci-error-view-vc.png" width="300"/>

### 6.2. OID4VP 테스트 절차

위와 같이 OID4VCI 테스트 절차를 통해 `SD-JWT VC`를 정상적으로 발급을 하였고, 이후 이어서 OID4VP 테스트를 수행할 수 있습니다.
OID4VP는 크게 `Same Device`와 `Cross Device` Flow를 통해 동작됩니다. 
`Cross Device` Flow 기준으로 아래와 같은 절차를 통해 OID4VP 테스트를 수행할 수 있습니다.

#### 6.2.1. Verifier Server에서 Authorization Request 생성

1. 브라우저에서 아래 주소로 접속합니다.

```groovy
http://<현재_IP>:8081/oid4vp/test

// 예를 들어 로컬 환경에서 실행 중이라면 http://localhost:8081/oid4vp/test
```

2. 아래와 같이 초기 테스트 페이지가 출력되며, `Authorization Request` 생성을 위해 `Initiate Verification Session`을 누릅니다.

<img src="./images/verifier-initial.png" width="1000"/>

3. 이후 아래와 같이 `Authorization Request`에 해당하는 QR Code가 생성됩니다. 단,  테스트 앱이 애뮬레이터에서 실행 시 QR 스캔이 원활하지 않을 수 있으므로, 하단의 `Authorization Request URL` 아래에 있는 데이터를 복사합니다.

<img src="./images/oid4vp-authorization-request.png" width="700"/>

#### 6.2.2. View and Submit VC

1. 안드로이드 에뮬레이터에서 실행되고 있는 테스트 앱에서 `View VC`를 클릭합니다.

<img src="./images/android-app-initial.png" width="300"/>

2. 아래와 같이 발급된 VC에 대한 정보가 정상적으로 출력되며, 테스트 앱 맨 하단의 `Enter QR Code Data` 아래에 복사한 데이터를 붙여넣기 한 후 `Submit`을 누릅니다.

<img src="./images/oid4vp-enter-qr-code-data.png" width="300"/>

3. 이후 VP 제출이 정상적으로 완료됩니다. 아래 화면이 출력되었다는 것은 제출된 `SD-JWT VP Toekn`에 대한 서명 검증 등이 Verifier 서버에서 정상적으로 완료되었다는 의미입니다.

<img src="./images/oid4vp-submission-completed.png" width="300"/>

---

## 7. 상세 문서 참조

OID4VC 프로젝트의 기본 설치 및 구동이 완료되었습니다. 각 컴포넌트에 대한 상세한 설정과 운영을 위해 다음 문서들을 참조하세요.

### 7.1. SDK 연동 가이드

| 서버 | 가이드 | 설명 |
| :--- | :--- | :--- |
| **Issuer Server** | [OID4VCI SDK 적용 가이드](../api/issuer-server/OID4VCI_SDK-INTEGRATION_GUIDE_ko.md) | build.gradle, application.yml, 메타데이터, DB 구성, Provider 등 상세 설정 |
| **Verifier Server** | [OID4VP SDK 연동 가이드](../api/verifier-server/OID4VP_SDK-INTEGRATION_GUIDE_ko.md) | build.gradle, application.yml, 저장소 모드 설정, Repository, Controller 등 상세 설정 |

### 7.2. 백엔드 서버 문서

| 서버 | 설명 | 문서 |
| :--- | :--- | :--- |
| **Issuer Server** | OID4VCI 표준에 따른 VC 발급 서버 (Authorization 포함) | [README](../../source/servers/issuer-server/README_ko.md) |
| **Verifier Server** | OID4VP 표준에 따른 VP 검증 서버 | [README](../../source/servers/verifier-server/README_ko.md) |

### 7.3. API 문서

| 서버 | 문서 | 에러 코드 |
| :--- | :--- | :--- |
| **Issuer Server** | [서버 API](../api/issuer-server/OID4VCI_SDK-SERVER_API_ko.md) | [OID4VCI SDK 에러](../api/issuer-server/OID4VCISDKError.md), [Formatter SDK 에러](../api/issuer-server/FormatterSDKError.md) |
| **Verifier Server** | [서버 API](../api/verifier-server/OID4VP_SDK-SERVER_API_ko.md) | [OID4VP SDK 에러](../api/verifier-server/OID4VPSDKError.md), [Formatter SDK 에러](../api/verifier-server/FormatterSDKError.md) |

### 7.4. 테스트 앱 문서

| 앱 | 설명 | 문서 |
| :--- | :--- | :--- |
| **Android App** | Android 기반 OID4VC 샘플 애플리케이션 | [README](../../source/apps/android-app/README_ko.md) |
| **iOS App** | iOS 기반 OID4VC 샘플 애플리케이션 | [README](../../source/apps/ios-app/README_ko.md) |

---