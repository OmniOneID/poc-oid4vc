---
puppeteer:
    pdf:
        format: A4
        displayHeaderFooter: true
        landscape: false
        scale: 0.8
        margin:
            top: 1.2cm
            right: 1cm
            bottom: 1cm
            left: 1cm
---

OID4VCI SDK Server API
==

- Subject: OID4VCI SDK Server API
- Author: Sangjun Kim
- Date: 2026-02-02
- Version: v1.0.0

| Version | Date       | Changes                   |
| ------- | ---------- | ------------------------- |
| v1.0.0  | 2026-02-02 | Initial release           |


<div style="page-break-after: always;"></div>

# Table of Contents
- [1. APIs](#1-apis)
    - [1.1 CredentialService](#11-credentialservice)
        - [1.1.1 processCredentialOffer](#111-processcredentialoffer)
        - [1.1.2 createTestCredentialOffer](#112-createtestcredentialoffer)
        - [1.1.3 issueCredential](#113-issuecredential)
        - [1.1.4 getDeferredCredential](#114-getdeferredcredential)
        - [1.1.5 getIssuerMetadata](#115-getissuermetadata)
        - [1.1.6 handleNonce](#116-handlenonce)
        - [1.1.7 handleNotification](#117-handlenotification)
    - [1.2 IssuanceGatewayService](#12-issuancegatewayservice)
        - [1.2.1 generateCredentialOfferUri](#121-generatecredentialofferuri)
- [2. Interfaces (SPI)](#2-interfaces-spi)
    - [2.1 UserDataProvider](#21-userdataprovider)
    - [2.2 KeyDataProvider](#22-keydataprovider)
    - [2.3 ProtocolIssuer](#23-protocolissuer)
    - [2.4 Store Interfaces](#24-store-interfaces)
- [3. Data Classes](#3-data-classes)
    - [3.1 CredentialRequest](#31-credentialrequest)
    - [3.2 Proofs](#32-proofs)
    - [3.3 CredentialResponse](#33-credentialresponse)
    - [3.4 IssuerMetadataResponse](#34-issuermetadataresponse)
    - [3.5 PreAuthorizeResponse](#35-preauthorizeresponse)
    - [3.6 DeferredIssuanceResponse](#36-deferredissuanceresponse)
    - [3.7 NotificationRequest](#37-notificationrequest)

<div style="page-break-after: always;"></div>

## Overview

This document defines the Server SDK API for implementing the **OpenID for Verifiable Credential Issuance (OID4VCI)** protocol. It is designed to help Issuer server developers efficiently implement VC (Verifiable Credential) issuance functionality.

### Key Features
- Credential Offer generation and management (Pre-Authorized & Authorization Code Flow)
- Processing of credential issuance requests and Proof validation
- Support for Deferred Issuance
- Provision of Issuer Metadata and dynamic endpoint management
- Support for various VC formats (SD-JWT, OpenDID VC, etc.)

<br>

<div style="page-break-after: always;"></div>

# 1. APIs

## 1.1 CredentialService

### 1.1.1 processCredentialOffer

### Class Name
`CredentialService`

### Function Name
`processCredentialOffer`

### Function Introduction
`Processes a client's request for a Credential Offer. Determines the appropriate Grant type based on the request ID and returns the Offer information.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| requestId | String | Request identifier (Starts with 'p': Pre-Auth, 'a': Auth Code) | M | |
| request | CredentialOfferRequest | Offer request information | M | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| CredentialOfferResponse | Generated Credential Offer information | M | |

### Function Declaration

```java
public CredentialOfferResponse processCredentialOffer(String requestId, CredentialOfferRequest request) throws OID4VCIException
```

<br>

### 1.1.2 createTestCredentialOffer

### Class Name
`CredentialService`

### Function Name
`createTestCredentialOffer`

### Function Introduction
`Generates a Credential Offer for testing purposes. Immediately obtains a Pre-Authorized Code from the Authorization Server to set up a test environment.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| userId | String | User identifier | M | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| TestCredentialOfferResponse | Test Offer information and PIN code | M | |

<br>

### 1.1.3 issueCredential

### Class Name
`CredentialService`

### Function Name
`issueCredential`

### Function Introduction
`Processes a credential issuance request. Validates the Access Token and Proof, then generates and returns the actual VC.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| request | CredentialRequest | Issuance request details (Format, Proof, etc.) | M | |
| accessToken | Jwt | Access Token issued by the Authorization Server | M | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| Object | Issued VC or DeferredIssuanceResponse | M | Immediate or deferred issuance result |

### Function Declaration

```java
public Object issueCredential(CredentialRequest request, Jwt accessToken) throws OID4VCIException
```

<br>

### 1.1.4 getDeferredCredential

### Class Name
`CredentialService`

### Function Name
`getDeferredCredential`

### Function Introduction
`Returns the actual VC for a previously deferred credential issuance request.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| transactionId | String | Deferred issuance transaction ID | M | |
| accessToken | Jwt | Valid Access Token | M | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| CredentialResponse | Issued VC information | M | |

<br>

### 1.1.5 getIssuerMetadata

### Class Name
`CredentialService`

### Function Name
`getIssuerMetadata`

### Function Introduction
`Returns the Issuer Metadata information.`

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| IssuerMetadataResponse | Issuer Metadata object | M | |

<br>

### 1.1.6 handleNonce

### Class Name
`CredentialService`

### Function Name
`handleNonce`

### Function Introduction
`Generates and returns a new c_nonce to be used for Proof validation.`

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| NonceResponse | Generated Nonce information | M | |

<br>

### 1.1.7 handleNotification

### Class Name
`CredentialService`

### Function Name
`handleNotification`

### Function Introduction
`Processes a notification from the wallet (e.g., successful VC storage).`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| request | NotificationRequest | Notification details | M | |

### Function Declaration

```java
public void handleNotification(NotificationRequest request)
```

<br>

## 1.2 IssuanceGatewayService

### 1.2.1 generateCredentialOfferUri

### Class Name
`IssuanceGatewayService`

### Function Name
`generateCredentialOfferUri`

### Function Introduction
`Generates a Credential Offer URI and QR code data based on the user identifier and Grant type.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| userId | String | User identifier | M | |
| grantType | String | Grant type (e.g., pre-authorized_code) | M | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| Map\<String, Object\> | Generated QR image and URL data | M | |

### Function Declaration

```java
public Map<String, Object> generateCredentialOfferUri(String userId, String grantType)
    throws IOException, WriterException, NoSuchAlgorithmException, OID4VCIException
```

<br>

# 2. Interfaces (SPI)

## 2.1 UserDataProvider
`Interface for providing user claim data to be included in the credential.`

```java
public interface UserDataProvider {
    Map<String, Object> getUserClaims(String userId, String credentialType);
}
```

## 2.2 KeyDataProvider
`Interface for providing key information (Private Key, Public Key, etc.) required for VC signing.`

```java
public interface KeyDataProvider {
    Map<String, Object> getKeyInfo(String userId, String credentialType);
}
```

## 2.3 ProtocolIssuer
`Interface responsible for actually generating the VC according to specific protocol flows.`

```java
public interface ProtocolIssuer {
    boolean supports(String format);
    Object issueCredential(Map<String, Object> claims, Map<String, Object> keyInfo) throws OID4VCIException;
}
```

## 2.4 Store Interfaces
- `CredentialOfferStore`: Temporarily stores generated Offer information.
- `SessionStore`: Manages issuance sessions and deferred issuance information.
- `CNonceStore`: Manages the list of valid c_nonces.

<br>

# 3. Data Classes

## 3.1 CredentialRequest
`Data for a credential issuance request received from a client.`

| Property | Type | Description |
|----------|------|-------------|
| credential_configuration_id | String | The ID of the requested credential configuration |
| credential_identifier | String | Credential identifier |
| proofs | Proofs | Proof of possession data |

## 3.2 Proofs
`Container for various types of proofs.`

| Property | Type | Description |
|----------|------|-------------|
| jwt | List\<String\> | JWT-based proofs |
| di_vp | List\<String\> | DI-VP based proofs |
| attestation | List\<String\> | Attestation-based proofs |

## 3.3 CredentialResponse
`Response data returned to the client upon successful credential issuance.`

| Property | Type | Description |
|----------|------|-------------|
| credentials | List\<Credential\> | List of issued credentials |
| c_nonce | String | Nonce to be used in the next request (Optional) |
| c_nonce_expires_in | Integer | Expiration time of the Nonce (Optional) |

## 3.4 IssuerMetadataResponse
`Metadata response data defining the Issuer's information.`

| Property | Type | Description |
|----------|------|-------------|
| credential_issuer | String | Issuer identifier URL |
| credential_endpoint | String | Credential issuance endpoint |
| credential_configurations_supported | Map | List of supported credential configurations |

## 3.5 PreAuthorizeResponse
`Pre-Authorized Code information received from the Authorization Server.`

| Property | Type | Description |
|----------|------|-------------|
| pre-authorized_code | String | Pre-authorized code |
| user_pin_required | boolean | Whether a PIN code is required |
| expires_in | int | Expiration time in seconds |

## 3.6 DeferredIssuanceResponse
`Response returned when credential issuance is deferred.`

| Property | Type | Description |
|----------|------|-------------|
| transaction_id | String | Transaction identifier for deferred issuance |
| interval | long | Polling interval in seconds |

## 3.7 NotificationRequest
`Request data for notification events.`

| Property | Type | Description |
|----------|------|-------------|
| notification_id | String | Notification identifier |
| event | String | Event type |
| event_description | String | Description of the event |
