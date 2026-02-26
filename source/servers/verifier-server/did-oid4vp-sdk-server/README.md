# OID4VP SDK Server

A Java SDK library implementing the OpenID for Verifiable Presentations (OID4VP) 1.0 standard. This SDK enables developers to easily build Verifier servers for VP (Verifiable Presentation) verification.

## Overview

The OID4VP SDK Server provides core functionality for:

- **Verification Session Management**: Create, manage, and expire verification sessions
- **DCQL Support**: Define credential requests using Digital Credentials Query Language
- **Multiple VP Formats**: Verify SD-JWT, OpenDID VC, and other credential formats
- **JAR (JWT-Secured Authorization Request)**: Generate signed Authorization Requests per RFC 9101
- **Flexible Storage**: Support for In-Memory, File, and JPA repository modes

## Project Structure
```
did-oid4vp-sdk-server
├── libs/                           # Bundled SDK dependencies (includes did-oid4vc-formatter-sdk-server JAR)
├── src/
│   └── main/
│       ├── java/
│       │   └── org/omnione/did/oid4vc/
│       │       ├── dcql/           # DCQL query processing
│       │       └── oid4vp/         # Core OID4VP implementation
│       │           ├── config/     # Auto-configuration
│       │           ├── dto/        # Data transfer objects
│       │           ├── exception/  # Error handling
│       │           ├── repository/ # Repository interfaces & in-memory impl
│       │           ├── service/    # Business logic services
│       │           └── util/       # Utilities (crypto, JAR, etc.)
│       └── resources/
│           └── META-INF/           # Spring auto-configuration
└── build.gradle
```

## Build
```bash
./gradlew :did-oid4vp-sdk-server:build
```

## Usage

Include the SDK as a dependency in your project:

**Option 1: As a subproject**

`settings.gradle`:
```groovy
include 'did-oid4vp-sdk-server'
```

`build.gradle`:
```groovy
dependencies {
    implementation project(':did-oid4vp-sdk-server')
}
```

**Option 2: As a JAR file**

After building the SDK, copy the JAR file to your project's `libs` directory:
```groovy
dependencies {
    implementation files('libs/did-oid4vp-sdk-server-3.0.0.jar')
}
```

## Documentation

For detailed integration instructions and API references, please refer to the following documents:

| Document | Description |
|----------|-------------|
| [Integration Guide](../../../../docs/api/verifier-server/OID4VP_SDK-INTEGRATION_GUIDE.md) | Step-by-step SDK integration guide |
| [API Reference](../../../../docs/api/verifier-server/OID4VP_SDK-SERVER_API.md) | SDK API endpoints and usage |
| [OID4VP SDK Errors](../../../../docs/api/verifier-server/OID4VPSDKError.md) | OID4VP SDK error codes |
| [Formatter SDK Errors](../../../../docs/api/verifier-server/FormatterSDKError.md) | OID4VC Formatter SDK error codes |

## License

[Apache 2.0](../../../../LICENSE)