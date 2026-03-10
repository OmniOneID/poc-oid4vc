# OID4VC (OpenID for Verifiable Credentials) PoC

Welcome to the OID4VC PoC repository.
This repository contains a Proof of Concept (PoC) project for testing the OID4VC (OID4VCI/OID4VP) standards in integration with OpenDID.

## Project Goals

* Understand the issuance and verification flows of the OID4VC protocol
* Evaluate the feasibility of building a DID-based VC (Verifiable Credential) ecosystem
* Test integration between Spring Boot-based servers and native mobile applications (Android/iOS)

## 🇪🇺🤝 EUDI Wallet Interoperability Demo

https://github.com/user-attachments/assets/d5637ebb-a11f-45dc-a161-28a8d50f5d2e

This project has successfully completed internal interoperability testing with the [EUDI Wallet](https://github.com/eu-digital-identity-wallet/eudi-app-android-wallet-ui).
It has been verified that Open DID's Issuer and Verifier servers interoperate with the EUDI Wallet based on the OID4VCI/OID4VP standards.

- **Test Wallet Version**: [EUDI Wallet 2026.02.35-Demo](https://github.com/eu-digital-identity-wallet/eudi-app-android-wallet-ui/releases/tag/Wallet%2FDemo_Version%3D2026.02.35-Demo_Build%3D35) ([commit](https://github.com/eu-digital-identity-wallet/eudi-app-android-wallet-ui/commit/bb008698fe48fcd3f7224d516aca0748fb1566f3))

The demo video above demonstrates the following:

| Category | Method | Description |
|:-----|:-----|:-----|
| Credential | SD-JWT VC | Issued in PID (Person Identification Data) format |
| Issuance | Pre-Authorized Code Flow | VC issuance via pre-authorized code |
| Verification | direct_post | VP Token submitted and verified via direct_post |

In addition to the SD-JWT VC flow shown in the demo, the project also supports **mDoc-based credential issuance and verification**, including **PID (Person Identification Data)** and **mDL (Mobile Driving License)** formats for EUDI Wallet interoperability.

### Sequence Diagram

```mermaid
%%{init: {
  'theme': 'base',
  'themeVariables': {
    'background': '#ffffff',
    'mainBkg': '#ffffff',
    'noteBkgColor': '#fff9e6',
    'noteTextColor': '#333333',
    'noteBorderColor': '#cccccc',
    'actorBkg': '#e8eef4',
    'actorBorder': '#7a8ea0',
    'actorTextColor': '#2c3e50',
    'signalColor': '#444444',
    'signalTextColor': '#333333',
    'sequenceNumberColor': '#ffffff',
    'labelBoxBkgColor': '#ffffff',
    'labelTextColor': '#333333'
  }
}}%%
sequenceDiagram
    participant Issuer as Open DID Issuer 🟠
    participant Wallet as EUDI Wallet 🇪🇺
    participant Verifier as Open DID Verifier 🟠
    rect rgb(230, 245, 255)
        Note over Issuer, Wallet: Credential Issuance - OID4VCI
        Issuer->>Wallet: Credential Offer (pre-authorized code)
        Wallet->>Issuer: Token Request
        Issuer-->>Wallet: Access Token
        Wallet->>Issuer: Credential Request
        Issuer-->>Wallet: SD-JWT VC (PID) Issuance
    end
    rect rgb(245, 255, 230)
        Note over Wallet, Verifier: Credential Presentation - OID4VP
        Verifier->>Wallet: Authorization Request
        Wallet->>Verifier: Fetch Request Object (JAR)
        Verifier-->>Wallet: Signed Request Object (DCQL)
        Wallet->>Verifier: Authorization Response (VP Token)
        Verifier->>Verifier: VP Token Verification (SD-JWT)
    end
```

## Folder Structure

An overview of the main folders and documents in the project directory.

```
poc-oid4vc
├── source
│   ├── apps
│   │   ├── android-app
│   │   └── ios-app
│   └── servers
│       ├── issuer-server
│       └── verifier-server
└── docs
    ├── api
    │   ├── issuer-server
    │   └── verifier-server
    └── installation
```

Description of each folder:

| Name | Description |
| :--- | :--- |
| **`source/servers`** | Contains server implementations for the OID4VC flow. |
| ┖ `issuer-server` | Issues VC (Verifiable Credentials) according to the OID4VCI standard. |
| ┖ `verifier-server` | Verifies VC according to the OID4VP standard. |
| **`source/apps`** | Contains sample mobile wallet applications. |
| ┖ `android-app` | Sample Android wallet application for storing and submitting VCs. |
| ┖ `ios-app` | Sample iOS wallet application for storing and submitting VCs. |
| **`docs`** | Contains project documentation. |
| ┖ `api` | SDK integration guides and API documentation for each server. |
| ┖ `installation` | Installation and running guides. |

## Supported Versions

| Standard   | Version | Link |
|------------|---------|------|
| OID4VCI    | OpenID for Verifiable Credential Issuance 1.0 | [Specification](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html) |
| OID4VP     | OpenID for Verifiable Presentations 1.0 | [Specification](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html) |

## Feature List
* **OID4VCI**

| Category | Feature | Status |
|:------|:------|:------|
|Authorization & Flows| Authorization Code Flow | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Pre-Authorized Code Flow | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|Credential Formats| SD-JWT VC |  ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Open DID VC | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| mDoc Format | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| W3C VC DM(JWT, JSON-LD) | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
|Endpoints| Token Endpoint | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Credential Endpoint | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Nonce Endpoint | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Deferred Endpoint | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Notification Endpoint | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|Credential Offer & Response| Credential Offer with authorization_code |  ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Credential Offer with pre-authorized_code |  ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Credential Issuer Metadata |  ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Pushed Authorization Request	| ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
|| Credential Response Encryption | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
|Security| Proof(JWT) | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| PKCE(Proof Key for Code Exchange) | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| JWE(JSON Web Encryption) | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
|| DPoP(Demonstration of Proof-of-Possession) | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |

* **OID4VP**

| Category | Feature | Status |
|:------|:------|:------|
|Authorization & Flows| Same-Device Flow | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Cross-Device Flow | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|Credential Formats| SD-JWT VC | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Open DID VC | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| mDoc Format | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| W3C VC DM(JWT, JSON-LD)  | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
|Authorization Request| Verifiable Presentations Authorization Requests | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Scoped Authorization Requests | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Self-Issued OpenID Provider Authorization Requests | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
|| JWT Secured Authorization Request(JAR) | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|Authorization Response| direct_post | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| query | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| fragment | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
|| *.jwt | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
|Query & Metadata| DCQL(Digital Credentials Query Language) | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Client Metadata | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Wallet Metadata | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
|| Request URI Methods - GET | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Request URI Methods - POST | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|Additional Features| Transaction Data | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
|Security| Proof(JWT) | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| JWE(JSON Web Encryption) | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |

## Getting Started

Please refer to the Installation and Operation Guide below to get started with OID4VC.

*   [OID4VC PoC Project Installation and Operation Guide](docs/installation/oid4vc_Installation_Guide.md)


For information on each subproject, please refer to the `README.md` file in the respective directory.

* [Issuer Server README](source/servers/issuer-server/README.md)
* [Verifier Server README](source/servers/verifier-server/README.md)
* [Android App README](source/apps/android-app/README.md)
* [iOS App README](source/apps/ios-app/README.md)

## API Reference Documentation

API documentation for each component can be found in the `docs/api` directory.

* [Issuer Server API](docs/api/issuer-server/issuer_server_API.md)
* [Verifier Server API](docs/api/verifier-server/verifier_server_API.md)

## Contributing

For detailed information on contribution procedures and the code of conduct, please refer to [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## License

[Apache 2.0](LICENSE)
