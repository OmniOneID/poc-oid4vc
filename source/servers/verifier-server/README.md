# Verifier Server Source Code

Welcome to the Verifier Server source code repository. This directory contains the core source code and build configurations for the Verifier Server.

## Directory Structure

Here's an overview of the directory structure.

```
verifier-server
├── did-oid4vp-sdk-server              # OID4VP SDK (Core Logic)
├── did-oid4vc-formatter-sdk-server    # OID4VC Formatter SDK
├── libs                               # Shared libraries
├── src                                # Verifier Example Application source code
│   ├── main
│   │   ├── java                       # Java source code (com.example.did.oid4vc.verifier)
│   │   └── resources                  # Configuration files (application.yml, DB scripts)
├── build.gradle                       # Project build configuration
└── README.md
```

## Directory Details

| Name | Description |
|------|-------------|
| `did-oid4vp-sdk-server` | Subproject containing the core OID4VP SDK logic |
| `did-oid4vc-formatter-sdk-server` | Subproject for OID4VC Formatter SDK |
| `libs` | Contains `did-wallet-sdk-server-2.0.0.jar`, `did-crypto-sdk-server-2.0.0.jar` used for signing |
| `src/main/resources` | Contains application properties and UI templates |

## Libraries

### 1. Open DID Libraries
These core libraries are used by the SDKs and the application:
- `did-wallet-sdk-server-2.0.0.jar` (in `libs/`)
- `did-crypto-sdk-server-2.0.0.jar` (in `libs/`)
- `did-sd-jwt-vc-sdk-server-3.0.0.jar` (in `did-oid4vp-sdk-server/libs/`)
- `opendid-vc-sdk-1.0.0.jar` (in `did-oid4vp-sdk-server/libs/`)

### 2. Third-Party Libraries
Key dependencies managed via Gradle:
- Spring Boot 3.2.4
- Spring Data JPA & PostgreSQL
- Liquibase (DB Schema Management)
- OpenFeign (HTTP Client)
- Bouncy Castle (Cryptography)
- Nimbus JOSE+JWT

## Documentation

Refer to the following documents for more detailed information:

- [Verifier Server API Reference](../../../docs/api/verifier-server/verifier_server_API.md)
  Guide for the reference implementation of the Verifier Server's API.
- [OID4VP SDK Integration Guide](../../../docs/api/verifier-server/OID4VP_SDK-INTEGRATION_GUIDE.md)
- [OID4VP SDK API Reference](../../../docs/api/verifier-server/OID4VP_SDK-SERVER_API.md)
- [OID4VP SDK Error Codes](../../../docs/api/verifier-server/OID4VPSDKError.md)
- [Formatter SDK Error Codes](../../../docs/api/verifier-server/FormatterSDKError.md)

## Contributing

Please read `CONTRIBUTING.md` and `CODE_OF_CONDUCT.md` in the root directory for details on our code of conduct, and the process for submitting pull requests to us.

## License
This project is licensed under the Apache License 2.0.

## Contact
For questions or support, please contact `maintainers` in the root directory.
