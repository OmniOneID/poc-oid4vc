# OID4VC Android App Guide

## Overview
This guide is for using the OID4VC (OpenID for Verifiable Credentials) Android sample app. This app is an implementation of a digital wallet that complies with the OID4VC standard. It demonstrates the entire process of scanning a QR code to receive a Credential Offer, obtaining a Verifiable Credential (VC) from an Issuer, securely storing it, and submitting a Verifiable Presentation (VP) to a Verifier.

## S/W Specifications
| Category | Content |
|---|---|
| OS | Android 14 |
| Language | Java 21 |
| IDE | Android Studio 4 |
| Build System | Gradle 8.2 |
| Compatibility | API 34 or higher |
| Test Environment | Android Emulator |

## Used Libraries (SDK)
This project uses the following key open-source libraries via Gradle:

- **zxing-android-embedded**
    - **URL**: [https://github.com/journeyapps/zxing-android-embedded](https://github.com/journeyapps/zxing-android-embedded)
    - **Description**: A library for easily integrating QR code and barcode scanning functionality into Android apps. Used to scan QR codes containing Credential Offers.

- **Retrofit & OkHttp**
    - **URL**: [https://square.github.io/retrofit/](https://square.github.io/retrofit/)
    - **Description**: HTTP client libraries used for network communication with Issuers and Verifiers.

- **Gson & Jackson**
    - **URL**: [https://github.com/google/gson](https://github.com/google/gson) & [https://github.com/FasterXML/jackson](https://github.com/FasterXML/jackson)
    - **Description**: Libraries for processing JSON-formatted data such as Credentials and Presentations.

## App Key Feature Flow
1.  **Scan QR Code**: Users tap the 'Scan QR' button on the app's main screen to activate the camera and scan a QR code presented by an Issuer or Verifier.
2.  **Receive Credential Offer**: If the scanned QR contains Credential Offer information, the app parses this information and displays to the user which Credential can be issued.
3.  **Request VC Issuance**: If the user agrees, the app requests VC issuance from the Issuer based on the Credential Offer information.
4.  **Store VC**: The VC received from the Issuer is securely stored within the app.
5.  **Submit VP**: Upon request from a Verifier, a Verifiable Presentation (VP) is generated based on the stored VC and submitted.

## License
For the project's own license, please refer to the `LICENSE` file in the repository. The licenses for each open-source library can be found in their respective repositories.