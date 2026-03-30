# mDoc Reader iOS App Guide

## Overview
This is a guide for using the mDoc Reader (mobile Document Reader) iOS sample app. This app is an implementation of a verifier (Verifier/Reader) complying with the ISO 18013-5 standard. It demonstrates the process of performing Device Engagement by scanning a QR code presented by a Holder and securely collecting and verifying mobile identification information, such as mDL (mobile Driver's License) or PID (Personal Identification Data), via short-range communication (BLE).

## S/W Specifications
| Category | Content |
| :--- | :--- |
| OS | iOS 15.0 |
| Language | Swift 5.8 |
| IDE | Xcode 16.2 |
| Build System | Xcode Default Build System |
| Compatibility | iOS 15 or higher |
| Test Environment | iPhone 17 (26.2.1) |

## Used Libraries (SDK)
This project uses the following open-source libraries via Swift Package Manager (SPM).

- **CodeScanner**
    - **URL**: [https://github.com/twostraws/CodeScanner](https://github.com/twostraws/CodeScanner)
    - **Description**: A library for scanning QR codes and barcodes in a SwiftUI environment. Used to scan the Holder's Device Engagement QR code.

- **SwiftCBOR**
    - **URL**: [https://github.com/unrelentingtech/SwiftCBOR](https://github.com/unrelentingtech/SwiftCBOR)
    - **Description**: A library for handling the CBOR (Concise Binary Object Representation) format used for data serialization in the ISO 18013-5 standard.

## Main Application Flow
1. **Document and Attribute Selection**: The user selects the document type (PID, mDL) and the scope of attributes (Full, Custom) to request on the app's main screen.
2. **QR Code Scanning**: Activate the camera by pressing the 'SCAN QR CODE' button and scan the Device Engagement QR code presented by the Holder app.
3. **Device Engagement and Session Connection**: Identify the Holder device based on the scanned QR data and establish a Proximity Communication session.
4. **Data Request and Reception**: Request claim data for the selected document through the established session and receive an encrypted response from the Holder.
5. **Verification and Result Display**: Verify the digital signature and data integrity of the received document, and display the extracted claim information (photo, name, date of birth, etc.) and verification results on the screen.

## License
Refer to the `LICENSE` file in the repository for the license of the project itself. The licenses for each open-source library can be found in their respective repositories.
