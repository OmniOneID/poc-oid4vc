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

MSO mDoc SDK API
==

- Subject: MSO mDoc SDK API
- Author: Open Source Development Team
- Date: 2026-03-31
- Version: v1.0.0

| Version | Date       | Changes         |
| ------- | ---------- | --------------- |
| v1.0.0  | 2026-03-31 | Initial version |

<div style="page-break-after: always;"></div>

# Table of Contents
- [Overview](#overview)
- [1. Reader APIs](#1-reader-apis)
    - [1.1 TransferController](#11-transfercontroller)
        - [1.1.1 initializeVerifier](#111-initializeverifier)
        - [1.1.2 initializeTransferManager](#112-initializetransfermanager)
        - [1.1.3 startEngagement](#113-startengagement)
        - [1.1.4 sendRequest](#114-sendrequest)
        - [1.1.5 stopConnection](#115-stopconnection)
    - [1.2 DeviceEngagement](#12-deviceengagement)
        - [1.2.1 fromQrCode](#121-fromqrcode)
        - [1.2.2 fromBytes](#122-frombytes)
    - [1.3 TrustManager](#13-trustmanager)
        - [1.3.1 isDocumentTrusted](#131-isdocumenttrusted)
- [2. Holder APIs](#2-holder-apis)
    - [2.1 MdocSessionManager](#21-mdocsessionmanager)
        - [2.1.1 decryptSessionEstablishment](#211-decryptsessionestablishment)
        - [2.1.2 generateDeviceResponse](#212-generatedeviceresponse)
    - [2.2 MdocProximityServer](#22-mdocproximityserver)
- [3. OID4VP APIs](#3-oid4vp-apis)
    - [3.1 OID4VPHandler](#31-oid4vphandler)
        - [3.1.1 createVPToken](#311-createvptoken)
    - [3.2 MDocVerifier](#32-mdocverifier)
        - [3.2.1 verify](#321-verify)
        - [3.2.2 verifyWithX5c](#322-verifywithx5c)
- [4. Data Classes](#4-data-classes)
    - [4.1 TransportConfig](#41-transportconfig)
    - [4.2 EngagementSource](#42-engagementsource)
    - [4.3 RequestedDocument](#43-requesteddocument)
    - [4.4 TransferStatus](#44-transferstatus)
    - [4.5 ReceivedDocument](#45-receiveddocument)
    - [4.6 DocumentValidity](#46-documentvalidity)
- [5. Enums](#5-enums)
    - [5.1 AttestationType](#51-attestationtype)
    - [5.2 DocumentMode](#52-documentmode)

<div style="page-break-after: always;"></div>

## Overview

This document defines the SDK API for **ISO/IEC 18013-5** based mDoc proximity presentation and **OpenID4VP** client VP Token creation/verification.

### Key Features
- **Reader**: Request and receive mDoc via BLE / Wi-Fi Aware / NFC
- **Holder**: Respond with mDoc (Peripheral/Server role) via BLE / NFC
- **OID4VP**: VP Token creation and verification (selective disclosure, DeviceAuth)

<br>

<div style="page-break-after: always;"></div>

# 1. Reader APIs

## 1.1 TransferController

### 1.1.1 initializeVerifier

### Class Name
`TransferController`

### Function Name
`initializeVerifier`

### Function Introduction
`Initializes issuer certificate trust verification. Sets a list of trusted root certificates in PEM format.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| certificates | List\<String\> | List of trusted root certificates in PEM format | M | |
| skipIssuerTrust | boolean | Skip issuer trust verification | M | For testing |
| includeSystemRoots | boolean | Whether to include system root certificates | O | Default: false |

### Function Declaration

```java
void initializeVerifier(List<String> certificates, boolean skipIssuerTrust)

void initializeVerifier(List<String> certificates, boolean skipIssuerTrust, boolean includeSystemRoots)
```

### Function Usage
```java
TransferController controller = new TransferController(context);

List<String> certs = List.of("-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----");
controller.initializeVerifier(certs, false, true);
```

<br>

### 1.1.2 initializeTransferManager

### Class Name
`TransferController`

### Function Name
`initializeTransferManager`

### Function Introduction
`Initializes a TransportManager for the specified transport method (BLE, Wi-Fi Aware, NFC).`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| config | TransportConfig | Transport-specific configuration | M | [Link](#41-transportconfig) |

### Function Declaration

```java
void initializeTransferManager(TransportConfig config)
```

### Function Usage
```java
// BLE
controller.initializeTransferManager(
    new TransportConfig.Ble(true, peripheralUuid, null));

// Wi-Fi Aware
controller.initializeTransferManager(
    new TransportConfig.WifiAware(passphrase, null, null));

// NFC
controller.initializeTransferManager(
    new TransportConfig.Nfc(isoDep, 255, 256));
```

<br>

### 1.1.3 startEngagement

### Class Name
`TransferController`

### Function Name
`startEngagement`

### Function Introduction
`Parses DeviceEngagement and initiates transport channel connection.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| source | EngagementSource | Engagement data source | M | [Link](#42-engagementsource) |

### Function Declaration

```java
void startEngagement(EngagementSource source)
```

### Function Usage
```java
// From QR code
controller.startEngagement(new EngagementSource.QrCode("mdoc:owBjMS..."));

// From NFC
controller.startEngagement(new EngagementSource.Nfc(engagementBytes));
```

<br>

### 1.1.4 sendRequest

### Class Name
`TransferController`

### Function Name
`sendRequest`

### Function Introduction
`Sends an mDoc claim request to the Holder and receives the result via callback.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| requestedDocs | List\<RequestedDocument\> | List of requested documents | M | [Link](#43-requesteddocument) |
| retainData | boolean | Intent to retain data (intentToRetain) | M | |
| callback | TransferCallback | Status change callback | M | |

### Function Declaration

```java
void sendRequest(List<RequestedDocument> requestedDocs, boolean retainData, TransferCallback callback)
```

### Function Usage
```java
List<String> claims = List.of("family_name", "given_name", "birth_date");
RequestedDocument doc = new RequestedDocument("mdl", AttestationType.MDL, DocumentMode.FULL, claims);

controller.sendRequest(List.of(doc), false, status -> {
    switch (status.getType()) {
        case RESPONSE_RECEIVED:
            List<ReceivedDocument> docs = status.getReceivedDocuments();
            break;
        case ERROR:
            String error = status.getErrorMessage();
            break;
    }
});
```

<br>

### 1.1.5 stopConnection

### Class Name
`TransferController`

### Function Name
`stopConnection`

### Function Introduction
`Terminates the transfer session and releases resources.`

### Function Declaration

```java
void stopConnection()
```

<br>

## 1.2 DeviceEngagement

### 1.2.1 fromQrCode

### Class Name
`DeviceEngagement`

### Function Name
`fromQrCode`

### Function Introduction
`Parses DeviceEngagement from a QR code string. Automatically handles the "mdoc:" prefix.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| qrCode | String | QR code string | M | Base64URL encoded |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| DeviceEngagement | Parsed Engagement object | M | |

### Function Declaration

```java
static DeviceEngagement fromQrCode(String qrCode) throws Exception
```

<br>

### 1.2.2 fromBytes

### Class Name
`DeviceEngagement`

### Function Name
`fromBytes`

### Function Introduction
`Parses DeviceEngagement from a byte array received via NFC or other means.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| data | byte[] | CBOR-encoded DeviceEngagement | M | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| DeviceEngagement | Parsed Engagement object | M | |

### Function Declaration

```java
static DeviceEngagement fromBytes(byte[] data) throws Exception
```

<br>

## 1.3 TrustManager

### 1.3.1 isDocumentTrusted

### Class Name
`TrustManager`

### Function Name
`isDocumentTrusted`

### Function Introduction
`Verifies whether the issuer certificate of the received document chains to a trusted root certificate using PKIX.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| document | ParsedDocument | Parsed mDoc document | M | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| boolean | Whether the document is trusted | M | |

### Function Declaration

```java
boolean isDocumentTrusted(DeviceResponseParser.ParsedDocument document)
```

<br>

<div style="page-break-after: always;"></div>

# 2. Holder APIs

## 2.1 MdocSessionManager

### 2.1.1 decryptSessionEstablishment

### Class Name
`MdocSessionManager`

### Function Name
`decryptSessionEstablishment`

### Function Introduction
`Decrypts a SessionEstablishment message received from the Reader and extracts the DeviceRequest.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| message | byte[] | SessionEstablishment CBOR bytes | M | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| CBORObject | Decrypted DeviceRequest | M | |

### Function Declaration

```java
CBORObject decryptSessionEstablishment(byte[] message)
```

<br>

### 2.1.2 generateDeviceResponse

### Class Name
`MdocSessionManager`

### Function Name
`generateDeviceResponse`

### Function Introduction
`Generates a DeviceResponse with selected claims and returns it encrypted with session encryption.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| mDoc | String | Base64-encoded original mDoc | M | |
| selectedClaimsKeys | List\<String\> | List of claim keys to disclose | M | |
| selectedClaimsNamespaces | List\<String\> | List of namespaces to disclose | M | |
| holderPrivateKey | PrivateKey | Holder's private key (for DeviceAuth) | M | |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| byte[] | Encrypted SessionData bytes | M | |

### Function Declaration

```java
byte[] generateDeviceResponse(String mDoc, List<String> selectedClaimsKeys, 
    List<String> selectedClaimsNamespaces, PrivateKey holderPrivateKey)
```

<br>

## 2.2 MdocProximityServer

### Interface Name
`MdocProximityServer`

### Description
`Holder-side proximity server interface. BLE/NFC implementations implement this interface.`

### Declaration

```java
public interface MdocProximityServer {
    void start();
    void stop();
    void sendResponse(byte[] response);
    void setListener(MdocProximityListener listener);
}
```

### Listener Interface

```java
public interface MdocProximityListener {
    void onDeviceConnected();
    void onDeviceDisconnected();
    void onRequestReceived(byte[] request);
}
```

<br>

<div style="page-break-after: always;"></div>

# 3. OID4VP APIs

## 3.1 OID4VPHandler

### 3.1.1 createVPToken

### Class Name
`OID4VPHandler`

### Function Name
`createVPToken`

### Function Introduction
`Creates a VP Token from an mDoc. Supports selective disclosure and DeviceAuth.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| issuerSignedBase64 | String | Base64-encoded IssuerSigned | M | |
| docType | String | Document type | O | Auto-detected if null |
| requestedClaims | Map\<String, Set\<String\>\> | Requested claims per namespace | O | All claims if null |
| holderPrivateKey | PrivateKey | Holder private key | O | For DeviceAuth |
| clientId | String | Verifier Client ID | O | For DeviceAuth |
| nonce | String | Nonce value | O | For DeviceAuth |
| responseUri | String | Response URI | O | For DeviceAuth |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| String | Base64-encoded VP Token | M | |

### Function Declaration

```java
// Basic (all claims, auto-detect docType)
static String createVPToken(String issuerSignedBase64)

// Selective disclosure (auto-detect docType)
static String createVPToken(String issuerSignedBase64, Map<String, Set<String>> requestedClaims)

// With DeviceAuth (auto-detect docType)
static String createVPToken(String issuerSignedBase64, PrivateKey holderPrivateKey,
    String clientId, String nonce, String responseUri)

// Selective disclosure + DeviceAuth (auto-detect docType)
static String createVPToken(String issuerSignedBase64, Map<String, Set<String>> requestedClaims,
    PrivateKey holderPrivateKey, String clientId, String nonce, String responseUri)

// Basic (explicit docType)
static String createVPToken(String issuerSignedBase64, String docType)

// Selective disclosure (explicit docType)
static String createVPToken(String issuerSignedBase64, String docType,
    Map<String, Set<String>> requestedClaims)

// With DeviceAuth (explicit docType)
static String createVPToken(String issuerSignedBase64, String docType,
    PrivateKey holderPrivateKey, String clientId, String nonce, String responseUri)

// Selective disclosure + DeviceAuth (explicit docType, full parameters)
static String createVPToken(String issuerSignedBase64, String docType,
    Map<String, Set<String>> requestedClaims, PrivateKey holderPrivateKey,
    String clientId, String nonce, String responseUri)
```

### Function Usage
```java
// Selective disclosure + DeviceAuth
Map<String, Set<String>> claims = Map.of(
    "org.iso.18013.5.1", Set.of("family_name", "given_name", "birth_date"));

String vpToken = OID4VPHandler.createVPToken(
    issuerSignedBase64, "org.iso.18013.5.1.mDL",
    claims, holderPrivateKey,
    "did:web:verifier.example.com", nonce, responseUri);
```

<br>

## 3.2 MDocVerifier

### 3.2.1 verify

### Class Name
`MDocVerifier`

### Function Name
`verify`

### Function Introduction
`Verifies a VP Token and extracts claims.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| mdocBase64 | String | Base64-encoded VP Token | M | |
| clientId | String | Verifier Client ID | O | For DeviceAuth verification |
| nonce | String | Nonce value | O | For DeviceAuth verification |
| responseUri | String | Response URI | O | For DeviceAuth verification |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| MDocClaimsSet | Verified claims set | M | |

### Function Declaration

```java
MDocClaimsSet verify(String mdocBase64)

MDocClaimsSet verify(String mdocBase64, String clientId, String nonce, String responseUri)
```

<br>

### 3.2.2 verifyWithX5c

### Class Name
`MDocVerifier`

### Function Name
`verifyWithX5c`

### Function Introduction
`Extracts the public key from the x5chain in IssuerAuth and verifies the signature. If trusted root certificates are provided, the certificate chain is also validated.`

### Input Parameters

| Parameter | Type | Description | **M/O** | **Note** |
|-----------|------|-------------|---------|----------|
| mdocBase64 | String | Base64-encoded VP Token | M | |
| trustedRootCerts | List\<X509Certificate\> | Trusted root certificates | O | Skips chain validation if null |
| clientId | String | Verifier Client ID | O | For DeviceAuth verification |
| nonce | String | Nonce value | O | For DeviceAuth verification |
| responseUri | String | Response URI | O | For DeviceAuth verification |

### Output Parameters

| Type | Description | **M/O** | **Note** |
|------|-------------|---------|----------|
| MDocClaimsSet | Verified claims set | M | |

### Function Declaration

```java
// x5chain signature verification only
static MDocClaimsSet verifyWithX5c(String mdocBase64)

// x5chain signature + certificate chain validation
static MDocClaimsSet verifyWithX5c(String mdocBase64, List<X509Certificate> trustedRootCerts)

// x5chain signature + DeviceAuth verification (no chain validation)
static MDocClaimsSet verifyWithX5c(String mdocBase64,
    String clientId, String nonce, String responseUri)

// x5chain signature + certificate chain + DeviceAuth verification (full)
static MDocClaimsSet verifyWithX5c(String mdocBase64, List<X509Certificate> trustedRootCerts,
    String clientId, String nonce, String responseUri)
```

<br>

<div style="page-break-after: always;"></div>

# 4. Data Classes

## 4.1 TransportConfig

### Declaration

```java
public sealed interface TransportConfig {
    record Ble(boolean usePeripheralServerMode, UUID peripheralServerModeUuid,
               UUID centralClientModeUuid) implements TransportConfig {}

    record WifiAware(String passphrase, Integer channelInfo,
                     Integer bandInfo) implements TransportConfig {}

    record Nfc(IsoDep isoDep, int maxCommandDataLength,
               int maxResponseDataLength) implements TransportConfig {}
}
```

### Property

| Type | Parameter | Type | Description | **M/O** |
|------|-----------|------|-------------|---------|
| Ble | usePeripheralServerMode | boolean | Whether to use peripheral mode | M |
| Ble | peripheralServerModeUuid | UUID | Peripheral service UUID | O |
| Ble | centralClientModeUuid | UUID | Central client UUID | O |
| WifiAware | passphrase | String | NAN Data Path security passphrase | M |
| WifiAware | channelInfo | Integer | Channel information | O |
| WifiAware | bandInfo | Integer | Band information (2.4/5GHz) | O |
| Nfc | isoDep | IsoDep | NFC IsoDep instance | M |
| Nfc | maxCommandDataLength | int | Maximum command data length | M |
| Nfc | maxResponseDataLength | int | Maximum response data length | M |

<br>

## 4.2 EngagementSource

### Declaration

```java
public sealed interface EngagementSource {
    record QrCode(String qrCode) implements EngagementSource {}
    record Nfc(byte[] data) implements EngagementSource {}
    record NfcDataTransfer(byte[] data, IsoDep isoDep,
        int maxCommandDataLength, int maxResponseDataLength) implements EngagementSource {}
}
```

<br>

## 4.3 RequestedDocument

### Declaration

```java
public class RequestedDocument implements Parcelable {
    public RequestedDocument(String id, AttestationType documentType,
        DocumentMode mode, List<String> claims)
}
```

### Property

| Name | Type | Description | **M/O** |
|------|------|-------------|---------|
| id | String | Document identifier | M |
| documentType | AttestationType | Document type (MDL, PID) | M |
| mode | DocumentMode | Document mode | M |
| claims | List\<String\> | List of requested claims | M |

<br>

## 4.4 TransferStatus

### Declaration

```java
public class TransferStatus {
    public enum Type {
        CONNECTING, CONNECTED, DEVICE_ENGAGEMENT_COMPLETED,
        REQUEST_SENT, RESPONSE_RECEIVED, ERROR, DISCONNECTED
    }
}
```

### Property

| Name | Type | Description | **M/O** |
|------|------|-------------|---------|
| type | Type | Status type | M |
| errorMessage | String | Error message (for ERROR type) | O |
| receivedDocuments | List\<ReceivedDocument\> | Received documents (for RESPONSE_RECEIVED type) | O |

<br>

## 4.5 ReceivedDocument

### Declaration

```java
public class ReceivedDocument {
    public ReceivedDocument(boolean trusted, String docType,
        Map<String, Object> claims, DocumentValidity validity)
}
```

### Property

| Name | Type | Description | **M/O** |
|------|------|-------------|---------|
| trusted | boolean | Whether the document is trusted | M |
| docType | String | Document type | M |
| claims | Map\<String, Object\> | Claims map | M |
| validity | DocumentValidity | Validity information | M |

<br>

## 4.6 DocumentValidity

### Declaration

```java
public class DocumentValidity {
    public DocumentValidity(Boolean deviceSignatureValid, Boolean issuerSignatureValid,
        Boolean dataIntegrityIntact, String signed, String validFrom, String validUntil)
}
```

### Property

| Name | Type | Description | **M/O** |
|------|------|-------------|---------|
| deviceSignatureValid | Boolean | Whether device signature is valid | O |
| issuerSignatureValid | Boolean | Whether issuer signature is valid | O |
| dataIntegrityIntact | Boolean | Whether data integrity is intact | O |
| signed | String | Signing timestamp | O |
| validFrom | String | Validity start | O |
| validUntil | String | Validity end | O |

<br>

# 5. Enums

## 5.1 AttestationType

| Value | docType | namespace |
|-------|---------|-----------|
| MDL | org.iso.18013.5.1.mDL | org.iso.18013.5.1 |
| PID | eu.europa.ec.eudi.pid.1 | eu.europa.ec.eudi.pid.1 |

## 5.2 DocumentMode

| Value | Description |
|-------|-------------|
| FULL | Include all claims |
| CUSTOM | Specify selective claims |

<br>
