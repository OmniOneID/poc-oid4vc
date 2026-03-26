# SD-JWT VC Client SDK AOS Guide

## Overview
This is a guide for the OpenDID SD-JWT VC Client SDK for Android. It provides functionality for issuing, presenting, and verifying SD-JWT (Selective Disclosure JWT) based Verifiable Credentials according to OID4VCI/OID4VP protocols.

- **OID4VCI** — SD-JWT Verifiable Credential issuance with selective disclosure
- **OID4VP** — SD-JWT Verifiable Presentation with Key Binding JWT

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
poc-client-sd-jwt-vc-sdk-aos/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── release/                                    # Build artifacts
└── src/main/java/org/omnione/did/sdk/sdjwt/
    ├── core/
    │   ├── oid4vp/                             # Credential presentation & verification
    │   │   ├── OID4VPHandler.java              # VP token creation
    │   │   ├── SDJWTVerifier.java              # SD-JWT verification
    │   │   └── SelectiveDisclosureProcessor.java
    │   ├── builder/                            # SD-JWT building
    │   │   ├── SDJWTBuilder.java               # Fluent SD-JWT builder
    │   │   ├── KeyBindingJWTBuilder.java        # Key Binding JWT builder
    │   │   └── SDObjectBuilder.java            # SD object construction
    │   └── validator/
    │       └── SDJWTValidator.java             # Structure validation
    ├── crypto/                                 # Cryptographic operations
    │   ├── JWSSigner.java                      # Signing interface
    │   ├── JWSVerifier.java                    # Verification interface
    │   ├── SignedJWT.java                      # Signed JWT representation
    │   └── impl/
    │       ├── ECDSASigner.java                # ECDSA signing (P-256/384/521)
    │       ├── ECDSAVerifier.java              # ECDSA verification
    │       ├── RSASSASigner.java               # RSA-PSS signing
    │       └── RSASSAVerifier.java             # RSA-PSS verification
    ├── datamodel/                              # Data models
    │   ├── SDJWT.java                          # SD-JWT structure
    │   ├── Disclosure.java                     # Selective disclosure
    │   └── DisclosureFrame.java                # Structured disclosure frame
    ├── exception/                              # Error handling
    │   ├── SDJWTException.java
    │   ├── SDJWTErrorCode.java
    │   └── SDJWTErrorCodeInterface.java
    └── util/                                   # Utilities
        ├── Base64UrlUtils.java                 # Base64URL encoding/decoding
        ├── HashUtils.java                      # Hash digest computation
        ├── SaltGenerator.java                  # Secure salt generation
        └── SimpleJWTDecoder.java               # JWT parsing
```

## Features

### OID4VP (Credential Presentation & Verification)
- VP token creation with selective claim disclosure
- Key Binding JWT with sd_hash verification
- DCQL (Digital Credentials Query Language) identifier support
- SD-JWT signature verification (issuer and holder)
- Disclosure hash integrity validation
- X.509 certificate chain validation with Root CA trust
- Temporal claims validation (exp, iat, nbf) with configurable clock skew

## Libraries
| Library            | Version | Purpose                              |
| ------------------ | ------- | ------------------------------------ |
| Bouncy Castle      | 1.79    | Cryptographic operations             |
| Jackson            | 2.15.2  | JSON processing                      |
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
