# Changelog

## v1.0.0 (2025-11-10)

### 🚀 New Features

- Authorization Server

    - Supports custom authorization_code and pre-authorized_code grant types for the OID4VCI standard.

    - Provides user authentication features via form login and Google social login.

- Issuer Server

    - Supports VC issuance requests based on both authorization_details and scope.

    - Provides credential issuance capabilities for SD-JWT and JWT-VC formats, integrated with the SD-JWT SDK.

- Verifier Server

    - Supports QR code-based cross-device flows and generates DCQL.

    - Provides functionality to receive and verify vp_token from a Wallet.

- Android App (Sample Wallet)

    - Supports pre-authorized_code and authorization_code issuance flows via QR code scanning.

    - Supports the OID4VP verification flow by parsing Presentation Definitions and submitting a vp_token.

- iPhone App (Sample Wallet)

    - Supports authorization_code flow using ASWebAuthenticationSession and VC issuance via QR scan.

    - Implemented secure VC storage and the ability to respond to OID4VP requests by presenting a VC.

- SD-JWT SDK

    - Provides logic for creating Disclosures and generating Presentations with selective disclosure.

    - Designed for common use across server (Java) and mobile (Android) environments.

