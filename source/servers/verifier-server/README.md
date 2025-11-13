# Verifier Server Source Code

Welcome to the Verifier Server source code repository. This directory contains the core source code and build configuration for the Verifier Server.

## Directory Structure

Here is an overview of the directory structure:
```
verifier-server
├── gradle
├── libs
    └── did-crypto-sdk-server-2.0.0.jar
    └── did-wallet-sdk-server-2.0.0.jar
    └── sd-jwt-sdk-1.0.0.jar
├── src
└── build.gradle
└── README.md
```

<br/>

Below is a description of each folder and file in the directory.

| Name                    | Description                                            |
| ----------------------- | ------------------------------------------------------- |
| verifier-server         | Verifier Server source code and build files            |
| ┖ gradle                | Gradle build configuration and scripts                 |
| ┖ libs                  | External libraries and dependencies                    |
| ┖ src                   | Main source code directory                             |
| ┖ build.gradle          | Gradle build configuration file                        |
| ┖ README.md             | Overview and guidelines for the source code            |


## Libraries

The libraries used in this project are organized into two main categories:

1. **Open DID Libraries**: These libraries are developed by the Open DID project and are available in the [libs folder](libs). These include:

    - `did-crypto-sdk-server-2.0.0.jar`
    - `did-wallet-sdk-server-2.0.0.jar`

2. **Third-Party Libraries**: These are open source dependencies managed through the [build.gradle](build.gradle) file. For a detailed list of third-party libraries and their licenses, refer to the `dependencies-license.md` file in the root directory.


## Documentation

For more information, please refer to the following documentation:

- [Verifier Server API Reference](../../../docs/api/verifier-server/verifier_server_API.md)
  A guide to the reference implementation of the Verifier Server API.

## Contributing

For details about our code of conduct and the procedure for submitting pull requests, please read `CONTRIBUTING.md` and `CODE_OF_CONDUCT.md` in the root directory.

## License
This project is licensed under the Apache License 2.0.

## Contact
If you have any questions or need support, please contact the `maintainers` in the root directory.