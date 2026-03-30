# OID4VC iOS App Guide

## 개요
OID4VC(OpenID for Verifiable Credentials) iOS 샘플 앱을 사용하기 위한 가이드입니다. 이 앱은 OID4VC 표준을 준수하는 전자지갑(Wallet)의 구현체로, QR 코드를 스캔하여 Credential Offer를 받고, 발급자(Issuer)로부터 Verifiable Credential(VC)을 발급받아 안전하게 저장하며, 검증자(Verifier)에게 Verifiable Presentation(VP)을 제출하는 전체 과정을 시연합니다.

## S/W 사양
| 구분              | 내용                            |
|-------------------|-------------------------------|
| OS                | iOS 15.0                      |
| Language          | Swift 5.8                     |
| IDE               | Xcode 16.2                    |
| Build System      | Xcode 기본 빌드 시스템            |
| Compatibility     | iOS 15 or higher              |
| Test Environment  | iPhone 17 (26.2.1)            |


## 사용된 라이브러리 (SDK)
본 프로젝트는 Swift Package Manager(SPM)를 통해 다음의 오픈소스 라이브러리를 사용합니다.

-   **CodeScanner**
    -   **URL**: [https://github.com/twostraws/CodeScanner](https://github.com/twostraws/CodeScanner)
    -   **설명**: SwiftUI 환경에서 QR 코드 및 바코드를 스캔하기 위한 라이브러리입니다. Credential Offer가 담긴 QR을 스캔하는 데 사용됩니다.

## 앱 주요 기능 흐름
1.  **QR 코드 스캔**: 사용자는 앱의 메인 화면에서 'Scan QR' 버튼을 눌러 카메라를 활성화하고, 발급자 또는 검증자가 제시하는 QR 코드를 스캔합니다.
2.  **Credential Offer 수신**: 스캔한 QR에 Credential Offer 정보가 포함된 경우, 앱은 해당 정보를 파싱하여 사용자에게 어떤 Credential을 발급받을 수 있는지 표시합니다.
3.  **VC 발급 요청**: 사용자가 동의하면, 앱은 Credential Offer의 정보를 바탕으로 발급자에게 VC 발급을 요청합니다.
4.  **VC 저장**: 발급자로부터 받은 VC는 앱 내의 안전한 공간에 저장됩니다.
5.  **VP 제출**: 검증자의 요구에 따라, 저장된 VC를 기반으로 Verifiable Presentation(VP)을 생성하여 제출합니다.

## 라이선스
프로젝트 자체의 라이선스는 저장소의 `LICENSE` 파일을 참고하십시오. 각 오픈소스 라이브러리의 라이선스는 해당 라이브러리의 저장소에서 확인하실 수 있습니다.