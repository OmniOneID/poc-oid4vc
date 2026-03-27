# mDOC Reader Android App Guide

## Overview
This guide is for using the OpenDID mDOC Reader Android application. This app is an ISO/IEC 18013-5 compliant proximity verifier that demonstrates the process of verifying mobile documents (mDL and etc) from Holder devices via BLE communication using QR code or NFC device engagement.

## S/W Specifications
| Category | Content |
|---|---|
| OS | Android 14 |
| Language | Java 21 |
| IDE | Android Studio Hedgehog (2023.1.1) or higher |
| Build System | Gradle 8.2 |
| Compatibility | API 34 or higher |
| Test Environment | Android Emulator |

## Used Libraries (SDK)
This project uses the following key open-source libraries via Gradle:

- **CBOR (com.upokecenter)**
    - **URL**: [https://github.com/peteroupc/CBOR-Java](https://github.com/peteroupc/CBOR-Java)
    - **Description**: A library for CBOR (Concise Binary Object Representation) encoding and decoding. Used to process ISO 18013-5 data structures.

- **COSE-JAVA**
    - **URL**: [https://github.com/cose-wg/COSE-JAVA](https://github.com/cose-wg/COSE-JAVA)
    - **Description**: A library implementing CBOR Object Signing and Encryption (COSE). Used for DeviceAuth verification (COSE_Sign1 / COSE_Mac0).

- **Bouncy Castle**
    - **URL**: [https://www.bouncycastle.org/](https://www.bouncycastle.org/)
    - **Description**: A comprehensive cryptographic library. Used for session encryption (ECDH + HKDF + AES-256-GCM) and issuer certificate chain verification (PKIX).

- **zxing-android-embedded**
    - **URL**: [https://github.com/journeyapps/zxing-android-embedded](https://github.com/journeyapps/zxing-android-embedded)
    - **Description**: A library for easily integrating QR code and barcode scanning functionality into Android apps. Used to scan QR codes for device engagement.

- **Gson & Jackson**
    - **URL**: [https://github.com/google/gson](https://github.com/google/gson) & [https://github.com/FasterXML/jackson](https://github.com/FasterXML/jackson)
    - **Description**: Libraries for processing JSON-formatted data used in configuration and claim parsing.

- **OkHttp**
    - **URL**: [https://square.github.io/okhttp/](https://square.github.io/okhttp/)
    - **Description**: An HTTP client library used for network communication.

## App Key Feature Flow
1.  **Select Documents**: Users select which documents (mDL, PID) and claims to request from the Holder on the main screen.
2.  **Initiate Engagement**: Users tap the 'QR Scan' button to scan a QR code, or use NFC tap to initiate device engagement with the Holder.
3.  **Establish Secure Session**: The app establishes a BLE connection with the Holder device and performs ISO 18013-5 session encryption.
4.  **Send DeviceRequest**: A DeviceRequest is built based on the selected documents and claims, then sent to the Holder.
5.  **Receive & Verify DeviceResponse**: The app receives the DeviceResponse, verifies the issuer certificate chain, DeviceAuth signature, and MSO data integrity.
6.  **Display Results**: The verified document data (claims, validity information, trust status) is displayed to the user.

## License
For the project's own license, please refer to the `LICENSE` file in the repository. The licenses for each open-source library can be found in their respective repositories.
