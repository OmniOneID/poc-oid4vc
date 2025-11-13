# Issuer Server Source Code

Welcome to the Issuer Server source code repository. This directory contains the core source code and build configurations for the Issuer Server.

## Directory Structure

Here's an overview of the directory structure.

```
issuer-server
├── gradle
├── libs
    └── did-crypto-sdk-server-2.0.0.jar
    └── did-datamodel-sdk-server-2.0.0.jar
    └── did-sdk-common-2.0.0.jar
    └── did-wallet-sdk-server-2.0.0.jar
    └── sd-jwt-sdk-1.0.0.jar
├── src
└── build.gradle
└── README.md
```

<br/>

Below is a description of each folder and file in the directory:

| Name                    | Description                                     |
| ----------------------- | ----------------------------------------------- |
| issuer-server           | Issuer Server source code and build files       |
| ┖ gradle                | Gradle build configurations and scripts         |
| ┖ libs                  | External libraries and dependencies             |
| ┖ src                   | Main source code directory                      |
| ┖ build.gradle          | Gradle build configuration file                 |
| ┖ README.md             | Overview and instructions for the source code   |


## Libraries

Libraries used in this project are organized into two main categories:

1. **Open DID Libraries**: These libraries are developed by the Open DID project and are available in the [libs folder](libs). They include:

    - `did-crypto-sdk-server-2.0.0.jar`
    - `did-datamodel-sdk-server-2.0.0.jar`
    - `did-sdk-common-2.0.0.jar`
    - `did-wallet-sdk-server-2.0.0.jar`

2. **Third-Party Libraries**: These libraries are open-source dependencies managed via the [build.gradle](build.gradle) file. For a detailed list of third-party libraries and their licenses, please refer to the `dependencies-license.md` file in the root directory.


## Documentation

Refer to the following documents for more detailed information:

- [Issuer Server API Reference](../../../docs/api/issuer-server/issuer_server_API.md)
  Guide for the reference implementation of the Issuer Server's API.

## Contributing

Please read `CONTRIBUTING.md` and `CODE_OF_CONDUCT.md` in the root directory for details on our code of conduct, and the process for submitting pull requests to us.

## License
This project is licensed under the Apache License 2.0.

## Contact
For questions or support, please contact `maintainers` in the root directory.
