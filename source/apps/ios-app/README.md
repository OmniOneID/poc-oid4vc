# OID4VC iOS App Guide

## Overview
This guide is for using the OID4VC (OpenID for Verifiable Credentials) iOS sample app. This app is an implementation of a digital wallet that complies with the OID4VC standard. It demonstrates the entire process of scanning a QR code to receive a Credential Offer, obtaining a Verifiable Credential (VC) from an Issuer, securely storing it, and submitting a Verifiable Presentation (VP) to a Verifier.

## S/W Specifications
| Category | Content |
|---|---|
| OS | iOS 15.0 |
| Language | Swift 5.8 |
| IDE | Xcode 16.2 |
| Build System | Xcode Basic build system |
| Compatibility | iOS 15 or higher |
| Test Environment | iPhone 15 (17.5) Simulator |

## Used Libraries (SDK)
This project uses the following open-source libraries via Swift Package Manager (SPM):

-   **CodeScanner**
    -   **URL**: [https://github.com/twostraws/CodeScanner](https://github.com/twostraws/CodeScanner)
    -   **Description**: A library for scanning QR codes and barcodes in a SwiftUI environment. Used to scan QR codes containing Credential Offers.

## App Key Feature Flow
1.  **Scan QR Code**: Users tap the 'Scan QR' button on the app's main screen to activate the camera and scan a QR code presented by an Issuer or Verifier.
2.  **Receive Credential Offer**: If the scanned QR contains Credential Offer information, the app parses this information and displays to the user which Credential can be issued.
3.  **Request VC Issuance**: If the user agrees, the app requests VC issuance from the Issuer based on the Credential Offer information.
4.  **Store VC**: The VC received from the Issuer is securely stored within the app.
5.  **Submit VP**: Upon request from a Verifier, a Verifiable Presentation (VP) is generated based on the stored VC and submitted.

## License
For the project's own license, please refer to the `LICENSE` file in the repository. The licenses for each open-source library can be found in their respective repositories.