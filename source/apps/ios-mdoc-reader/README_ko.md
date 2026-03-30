# mDoc Reader iOS App Guide

## 개요
mDoc Reader(mobile Document Reader) iOS 샘플 앱을 사용하기 위한 가이드입니다. 이 앱은 ISO 18013-5 표준을 준수하는 검증기(Verifier/Reader)의 구현체로, 홀더(Holder)가 제시하는 QR 코드를 스캔하여 장치 참여(Device Engagement)를 수행하고, 근거리 통신(BLE)을 통해 mDL(mobile Driver's License) 또는 PID(Personal Identification Data)와 같은 모바일 신분증 정보를 안전하게 수집하여 검증하는 과정을 시연합니다.

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
    -   **설명**: SwiftUI 환경에서 QR 코드 및 바코드를 스캔하기 위한 라이브러리입니다. 홀더의 Device Engagement QR 코드를 스캔하는 데 사용됩니다.

-   **SwiftCBOR**
    -   **URL**: [https://github.com/unrelentingtech/SwiftCBOR](https://github.com/unrelentingtech/SwiftCBOR)
    -   **설명**: ISO 18013-5 표준에서 데이터 직렬화에 사용하는 CBOR(Concise Binary Object Representation) 형식을 처리하기 위한 라이브러리입니다.

## 앱 주요 기능 흐름
1.  **문서 및 속성 선택**: 사용자는 앱 메인 화면에서 요청하고자 하는 문서 유형(PID, mDL)과 속성 범위(Full, Custom)를 선택합니다.
2.  **QR 코드 스캔**: 'SCAN QR CODE' 버튼을 눌러 카메라를 활성화하고, 홀더 앱이 제시하는 Device Engagement QR 코드를 스캔합니다.
3.  **장치 참여 및 세션 연결**: 스캔한 QR 데이터를 기반으로 홀더 장치를 식별하고, 근거리 통신(Proximity Communication) 세션을 확립합니다.
4.  **데이터 요청 및 수신**: 확립된 세션을 통해 선택된 문서의 클레임(Claims) 데이터를 요청하고, 홀더로부터 암호화된 응답을 수신합니다.
5.  **검증 및 결과 표시**: 수신된 문서의 디지털 서명, 데이터 무결성 등을 검증하고, 추출된 클레임 정보(사진, 성명, 생년월일 등)와 검증 결과를 화면에 표시합니다.

## 라이선스
프로젝트 자체의 라이선스는 저장소의 `LICENSE` 파일을 참고하십시오. 각 오픈소스 라이브러리의 라이선스는 해당 라이브러리의 저장소에서 확인하실 수 있습니다.