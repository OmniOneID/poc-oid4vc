# Issuer Server Source Code

Welcome to the Issuer Server source code repository. This directory contains the core source code and build configurations for the Issuer Server.

## Directory Structure

Here's an overview of the directory structure.

```
issuer-server
├── did-oid4vci-sdk-server           # OID4VCI SDK (Core Logic)
├── did-oid4vc-authorization-sdk-server # Authorization SDK
├── libs                             # Shared libraries
├── metadata                         # Issuer Metadata files (JSON)
├── src                              # Issuer Example Application source code
│   ├── main
│   │   ├── java                     # Java source code (com.example.did.oid4vc.issuer)
│   │   └── resources                # Configuration files (application.yml, DB scripts)
├── build.gradle                     # Project build configuration
└── README.md
```

## Directory Details

| Name | Description |
|------|-------------|
| `did-oid4vci-sdk-server` | Subproject containing the core OID4VCI SDK logic |
| `did-oid4vc-authorization-sdk-server` | Subproject for Authorization Server integration |
| `libs` | Contains `did-wallet-sdk-server-2.0.0.jar` used for signing |
| `metadata` | Directory for various Issuer Metadata configurations (local, dev, etc.) |
| `src/main/resources` | Contains application properties and UI templates |

## Libraries

### 1. Open DID Libraries
These core libraries are bundled in the `libs/` directory:
- `did-wallet-sdk-server-2.0.0.jar`
- `did-crypto-sdk-server-2.0.0.jar`
- `did-datamodel-sdk-server-2.0.0.jar`
- `did-sdk-common-2.0.0.jar`
- `did-oid4vci-sdk-server-3.0.0.jar`
- `did-oid4vc-authorization-sdk-server-3.0.0.jar`
- `did-oid4vc-formatter-sdk-server-3.0.0.jar`
- `did-mso-mdoc-sdk-server-3.0.0.jar`
- `did-sd-jwt-vc-sdk-server-3.0.0.jar`
- `opendid-vc-sdk-1.0.0.jar`

### 2. Third-Party Libraries
Key dependencies managed via Gradle:
- Spring Boot 3.2.4
- Spring Security & OAuth2 Authorization Server
- Spring Data JPA & PostgreSQL
- Liquibase (DB Schema Management)
- OpenFeign (HTTP Client)
- Google ZXing (QR Code Generation)

## Documentation

Refer to the following documents for more detailed information:

- [Issuer Server API Reference](../../../docs/api/issuer-server/issuer_server_API.md)
  Guide for the reference implementation of the Issuer Server's API.
- [OID4VCI SDK Integration Guide](../../../docs/api/issuer-server/OID4VCI_SDK-INTEGRATION_GUIDE.md)
- [OID4VCI SDK API Reference](../../../docs/api/issuer-server/OID4VCI_SDK-SERVER_API.md)
- [OID4VCI SDK Error Codes](../../../docs/api/issuer-server/OID4VCISDKError.md)
- [Formatter SDK Error Codes](../../../docs/api/issuer-server/FormatterSDKError.md)

## Contributing

Please read `CONTRIBUTING.md` and `CODE_OF_CONDUCT.md` in the root directory for details on our code of conduct, and the process for submitting pull requests to us.

## License
This project is licensed under the Apache License 2.0.

## Contact
For questions or support, please contact `maintainers` in the root directory.
