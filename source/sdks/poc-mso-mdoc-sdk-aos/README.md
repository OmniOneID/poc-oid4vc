# mDoc Client SDK AOS Guide

## Overview
This is a guide for the OpenDID mDoc Client SDK for Android. It provides unified functionality for handling mobile documents (mDL, etc) through two ISO/IEC standards:

- **ISO/IEC 18013-7 (OID4VC)** — OID4VCI credential issuance and OID4VP credential presentation/verification
- **ISO/IEC 18013-5 (Proximity)** — BLE/NFC based proximity verification with session encryption

## S/W Specifications
| Category         | Details                                                 |
| ---------------- | ------------------------------------------------------- |
| OS               | Android 14                                              |
| Language         | Java 21                                                 |
| IDE              | Android Studio Hedgehog (2023.1.1) or higher            |
| Build System     | Gradle 8.2                                              |
| Compatibility    | Android API level 34 or higher                          |
| Test Environment | Minimum Requirements: Android 8.0 (Oreo, API Level 26)  |
|                  | Recommended Requirements: Android 14 (API Level 34)     |

## Project Structure
```
poc-mso-mdoc-sdk-aos/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── release/                                  # Build artifacts
├── docs/api/                                 # API documentation
└── src/main/java/org/omnione/did/sdk/mdoc/
    ├── oid4vc/                               # ISO 18013-7 (OID4VC)
    │   ├── core/
    │   │   └── oid4vp/                       # Credential presentation & verification
    │   ├── constant/                         # mDoc/mDL/PID constants
    │   ├── datamodel/                        # IssuerSigned/DeviceSigned documents
    │   ├── exception/                        # Error codes & exceptions
    │   └── util/                             # COSE signature & key utilities
    └── proximity/                            # ISO 18013-5 (Proximity)
        ├── reader/                           # Reader (Verifier) role
        │   ├── core/                         # Transfer, engagement, session, verification
        │   ├── communication/                # BLE transport & GATT management
        │   ├── datamodel/                    # Document, claim, transfer models
        │   ├── exception/                    # Error codes & exceptions
        │   └── utility/                      # Certificate, parsing, logging utilities
        └── holder/                           # Holder role
            ├── core/                         # Transfer, engagement, session management
            ├── ble/                          # BLE transport
            ├── nfc/                          # NFC transport
            └── wifi/                         # Wi-Fi Aware transport
```

## Features

### OID4VC (ISO 18013-7)
- OID4VP credential presentation with selective disclosure
- mDoc verification (IssuerAuth, DeviceAuth, digest validation)
- X.509 certificate chain validation (PKIX)
- MSO (Mobile Security Object) generation and parsing
- Support for EUDI PID and ISO mDL document types
- Wallet/HSM-based signing via CompactSigner interface

### Proximity (ISO 18013-5)
- QR code based device engagement
- NFC based device engagement
- BLE Central Client / Peripheral Server mode
- ISO 18013-5 session encryption (ECDH + HKDF + AES-256-GCM)
- DeviceRequest building and DeviceResponse parsing
- Issuer certificate chain verification (PKIX)
- DeviceAuth verification (COSE_Sign1 / COSE_Mac0)
- MSO data integrity verification

## Libraries
| Library            | Version | Purpose                              |
| ------------------ | ------- | ------------------------------------ |
| CBOR (upokecenter) | 4.5.2   | CBOR encoding/decoding               |
| COSE-JAVA          | 1.1.0   | COSE_Sign1 operations                |
| Bouncy Castle      | 1.79    | Cryptographic operations             |
| Jackson             | 2.15.2  | JSON processing                      |
| Gson               | 2.11.0  | JSON processing                      |
| AndroidX Core      | 1.13.1  | Android core utilities               |

## Build
```bash
# Build release AAR
./gradlew assembleRelease

# Export JAR to release/
./gradlew exportJar
```

## License
Apache License 2.0
