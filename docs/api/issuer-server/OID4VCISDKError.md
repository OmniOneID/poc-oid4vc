# OID4VCI SDK Error Codes

This document defines the list of error codes used in the OID4VCI SDK. All error codes start with the `SSOID4VCI` prefix.

## 1. Credential Offer (01)
Errors related to the issuance offer process.

| Error Code | Message | Description |
|------------|---------|-------------|
| SSOID4VCI01000 | Credential offer not found | The requested Credential Offer could not be found. |
| SSOID4VCI01001 | Credential offer has expired | The Credential Offer has expired. |
| SSOID4VCI01002 | Invalid credential offer status | The status of the Credential Offer is incorrect. |
| SSOID4VCI01003 | Failed to generate credential offer | Failed to generate the Credential Offer. |

## 2. Issuance (02)
Errors related to the verifiable credential issuance process.

| Error Code | Message | Description |
|------------|---------|-------------|
| SSOID4VCI02000 | Invalid issuance request | The issuance request parameters are incorrect. |
| SSOID4VCI02001 | Unsupported credential format | The requested credential format is not supported. |
| SSOID4VCI02002 | Requested credential configuration not found | The requested credential configuration (Configuration ID) could not be found. |
| SSOID4VCI02003 | Failed to generate credential | An error occurred during credential generation. |
| SSOID4VCI02004 | User not found | The user could not be identified. |
| SSOID4VCI02005 | Deferred transaction not found | The deferred issuance transaction could not be found. |

## 3. Proof / Nonce (03)
Errors related to proof of possession and nonce validation.

| Error Code | Message | Description |
|------------|---------|-------------|
| SSOID4VCI03000 | Invalid proof of possession | Verification of the proof of possession (Proof) failed. |
| SSOID4VCI03001 | Proof of possession is missing | The proof of possession data is missing. |
| SSOID4VCI03002 | Unsupported proof type | The requested proof type is not supported. |
| SSOID4VCI03003 | Invalid or expired c_nonce | The c_nonce is invalid or has expired. |
| SSOID4VCI03004 | Failed to parse proof | Failed to parse the Proof data. |

## 4. JWS/JWT (04)
Errors related to signing and token processing.

| Error Code | Message | Description |
|------------|---------|-------------|
| SSOID4VCI04000 | Failed to sign JWT | Failed to generate the JWT signature. |
| SSOID4VCI04001 | Failed to verify JWS | Failed to verify the JWS signature. |
| SSOID4VCI04002 | Invalid JWT format | The JWT format is incorrect. |
| SSOID4VCI04003 | Unsupported signing algorithm | The requested signing algorithm is not supported. |

## 5. Metadata (05)
Errors related to metadata processing.

| Error Code | Message | Description |
|------------|---------|-------------|
| SSOID4VCI05000 | Failed to load issuer metadata | Failed to load the issuer metadata. |
| SSOID4VCI05001 | Invalid metadata format | The metadata format is incorrect. |
| SSOID4VCI05002 | Required metadata field is missing | A required metadata field is missing. |

## 6. Store (06)
Errors related to data storage.

| Error Code | Message | Description |
|------------|---------|-------------|
| SSOID4VCI06000 | Failed to save to store | Failed to save data to the store. |
| SSOID4VCI06001 | Failed to load from store | Failed to load data from the store. |
| SSOID4VCI06002 | Failed to delete from store | Failed to delete data from the store. |

## 7. General (99)
Common errors.

| Error Code | Message | Description |
|------------|---------|-------------|
| SSOID4VCI99000 | Invalid parameter | The parameter is invalid. |
| SSOID4VCI99001 | Parameter cannot be null | The parameter cannot be null. |
| SSOID4VCI99002 | Parameter cannot be empty | The parameter cannot be empty. |
| SSOID4VCI99999 | Unexpected error occurred | An unexpected server error occurred. |
