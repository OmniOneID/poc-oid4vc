# OID4VC (OpenID for Verifiable Credentials) PoC

Welcome to the OID4VC PoC repository.
This repository contains a Proof of Concept (PoC) project for testing the OID4VC (OID4VCI/OID4VP) standards in integration with OpenDID.

## Project Goals

* Understand the issuance and verification flows of the OID4VC protocol
* Evaluate the feasibility of building a DID-based VC (Verifiable Credential) ecosystem
* Test integration between Spring Boot-based servers and native mobile applications (Android/iOS)

## Folder Structure

An overview of the main folders and documents in the project directory.

```
poc-oid4vc
├── apps
│   ├── android-app
│   └── ios-app
├── docs
│   └── api
│       ├── authorization-server
│       ├── issuer-server
│       ├── sd-jwt-sdk
│       └── verifier-server
├── sdks
│   └── sd-jwt-sdk
└── servers
    ├── authorization-server
    ├── issuer-server
    └── verifier-server
```

Description of each folder:

| Name | Description |
| :--- | :--- |
| **`servers`** | Contains server implementations for the OID4VC flow. |
| ┖ `authorization-server` | Manages authentication and authorization based on OAuth 2.0 and OIDC. |
| ┖ `issuer-server` | Issues VC (Verifiable Credentials) according to the OID4VCI standard. |
| ┖ `verifier-server` | Verifies VC according to the OID4VP standard. |
| **`apps`** | Contains sample mobile wallet applications. |
| ┖ `android-app` | Sample Android wallet application for storing and submitting VCs. |
| ┖ `ios-app` | Sample iOS wallet application for storing and submitting VCs. |
| **`sdks`** | Contains SDKs for core functionality. |
| ┖ `sd-jwt-sdk` | SDK for creating, signing, and verifying SD-JWT VCs. |
| **`docs`** | Contains project documentation. |
| ┖ `api` | API documentation for each server and SDK. |

## Supported Versions
| Standard   | Version | Link |
|------------|---------|------|
| OID4VCI    | OpenID for Verifiable Credential Issuance 1.0 | [Specification](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html) |
| OID4VP     | OpenID for Verifiable Presentations 1.0 | [Specification](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html) |
| SD-JWT     | RFC 9901 | [Specification](https://datatracker.ietf.org/doc/rfc9901/) |

## Feature List
* **OID4VCI**

| Category | Feature | Status |
|:------|:------|:------|
|Authorization & Flows| Authorization Code Flow | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Pre-Authorized Code Flow | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|Credential Formats| SD-JWT VC |  ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| W3C VC DM(JWT, JSON-LD) | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
|| mDoc Format | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
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
|| W3C VC DM(JWT, JSON-LD)  | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
|| mDoc Format | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
|Authorization Request| Verifiable Presentations Authorization Requests | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Scoped Authorization Requests | ![Supported](https://img.shields.io/badge/Supported-✅-brightgreen) |
|| Self-Issued OpenID Provider Authorization Requests | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
|| JWT Secured Authorization Request(JAR) | ![Planned](https://img.shields.io/badge/Planned-📅-blue) |
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

* [Authorization Server README](source/servers/authorization-server/README.md)
* [Issuer Server README](source/servers/issuer-server/README.md)
* [Verifier Server README](source/servers/verifier-server/README.md)
* [SD-JWT SDK README](source/sdks/sd-jwt-sdk/README.md)
* [Android App README](source/apps/android-app/README.md)
* [iOS App README](source/apps/ios-app/README.md)

## API Reference Documentation

API documentation for each component can be found in the `docs/api` directory.

* [Authorization Server API](docs/api/authorization-server/authorization_server_API.md)
* [Issuer Server API](docs/api/issuer-server/issuer_server_API.md)
* [Verifier Server API](docs/api/verifier-server/verifier_server_API.md)
* [SD-JWT SDK API](docs/api/sd-jwt-sdk/sd-jwt-sdk_API.md)

## Contributing

For detailed information on contribution procedures and the code of conduct, please refer to [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## License

[Apache 2.0](LICENSE)
