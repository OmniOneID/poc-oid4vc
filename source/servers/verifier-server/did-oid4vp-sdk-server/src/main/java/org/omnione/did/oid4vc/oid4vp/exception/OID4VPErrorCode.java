/*
 * Copyright 2026 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnione.did.oid4vc.oid4vp.exception;

/**
 * Error codes for OID4VP SDK.
 */
public enum OID4VPErrorCode implements OID4VPErrorCodeInterface {

    // Base
    ERR_CODE_OID4VP_BASE("SSOID4VP", ""),

    // Authorization (01)
    ERR_CODE_AUTH_BASE(ERR_CODE_OID4VP_BASE, "01", ""),
    ERR_CODE_AUTH_INVALID_REQUEST_URI(ERR_CODE_AUTH_BASE, "000", "Invalid or expired request_uri"),
    ERR_CODE_AUTH_REQUEST_ALREADY_FETCHED(ERR_CODE_AUTH_BASE, "001", "Request URI already used"),
    ERR_CODE_AUTH_SESSION_EXPIRED(ERR_CODE_AUTH_BASE, "002", "Session has expired"),
    ERR_CODE_AUTH_INVALID_SESSION(ERR_CODE_AUTH_BASE, "003", "Invalid session state"),
    ERR_CODE_AUTH_SIGNER_REQUIRED(ERR_CODE_AUTH_BASE, "004", "CompactSigner is required"),
    ERR_CODE_AUTH_SIGN_FAILED(ERR_CODE_AUTH_BASE, "005", "Failed to sign authorization request"),
    ERR_CODE_AUTH_SESSION_NOT_COMPLETED(ERR_CODE_AUTH_BASE, "006", "Session status is not COMPLETED"),

    // Initiation (02)
    ERR_CODE_INIT_BASE(ERR_CODE_OID4VP_BASE, "02", ""),
    ERR_CODE_INIT_MISSING_QUERY(ERR_CODE_INIT_BASE, "000", "Either dcql_query or scope is required"),
    ERR_CODE_INIT_BOTH_QUERY_PROVIDED(ERR_CODE_INIT_BASE, "001", "Cannot specify both dcql_query and scope"),
    ERR_CODE_INIT_INVALID_SCOPE(ERR_CODE_INIT_BASE, "002", "No DCQL mapping found for scope"),
    ERR_CODE_INIT_INVALID_DCQL_FORMAT(ERR_CODE_INIT_BASE, "003", "Invalid DCQL JSON format"),
    ERR_CODE_INIT_DCQL_VALIDATION_FAILED(ERR_CODE_INIT_BASE, "004", "DCQL query validation failed"),

    // VP Token Processing (03)
    ERR_CODE_VP_BASE(ERR_CODE_OID4VP_BASE, "03", ""),
    ERR_CODE_VP_TOKEN_NULL(ERR_CODE_VP_BASE, "000", "VP Token cannot be null or empty"),
    ERR_CODE_VP_TOKEN_PARSE_FAILED(ERR_CODE_VP_BASE, "001", "Failed to parse VP Token"),
    ERR_CODE_VP_INVALID_CREDENTIAL(ERR_CODE_VP_BASE, "002", "Invalid credential format"),
    ERR_CODE_VP_NO_VERIFIER_FOUND(ERR_CODE_VP_BASE, "003", "No suitable verifier found"),
    ERR_CODE_VP_VERIFICATION_FAILED(ERR_CODE_VP_BASE, "004", "VP verification failed"),
    ERR_CODE_VP_INSUFFICIENT_KEYS(ERR_CODE_VP_BASE, "005", "Insufficient public keys provided"),
    ERR_CODE_VP_DCQL_ID_MISMATCH(ERR_CODE_VP_BASE, "006", "DCQL ID not found in query"),
    ERR_CODE_VP_DCQL_NO_CREDENTIALS(ERR_CODE_VP_BASE, "007", "DCQL Query has no credentials defined"),
    ERR_CODE_VP_FORMAT_MISMATCH(ERR_CODE_VP_BASE, "008", "Credential format mismatch"),
    ERR_CODE_VP_CREDENTIAL_COUNT_MISMATCH(ERR_CODE_VP_BASE, "009", "Credential count mismatch"),
    ERR_CODE_VP_META_MISMATCH(ERR_CODE_VP_BASE, "010", "Credential meta condition not satisfied"),

    // JWS/JWT (04)
    ERR_CODE_JWS_BASE(ERR_CODE_OID4VP_BASE, "04", ""),
    ERR_CODE_JWS_SIGN_FAILED(ERR_CODE_JWS_BASE, "000", "Failed to sign JWT"),
    ERR_CODE_JWS_NOT_SIGNED(ERR_CODE_JWS_BASE, "001", "JWT has not been signed"),
    ERR_CODE_JWS_SERIALIZE_FAILED(ERR_CODE_JWS_BASE, "002", "Failed to serialize JWT"),
    ERR_CODE_JWS_INVALID_FORMAT(ERR_CODE_JWS_BASE, "003", "Invalid signature format"),
    ERR_CODE_JWS_UNSUPPORTED_ALGORITHM(ERR_CODE_JWS_BASE, "004", "Unsupported key algorithm"),
    ERR_CODE_JWS_INVALID_KEY(ERR_CODE_JWS_BASE, "005", "Invalid key"),
    ERR_CODE_JWS_DER_CONVERSION_FAILED(ERR_CODE_JWS_BASE, "006", "Failed to convert DER to P1363"),

    // Configuration (05)
    ERR_CODE_CONFIG_BASE(ERR_CODE_OID4VP_BASE, "05", ""),
    ERR_CODE_CONFIG_LOAD_FAILED(ERR_CODE_CONFIG_BASE, "000", "Failed to load configuration"),
    ERR_CODE_CONFIG_SAVE_FAILED(ERR_CODE_CONFIG_BASE, "001", "Failed to save configuration"),
    ERR_CODE_CONFIG_NOT_FOUND(ERR_CODE_CONFIG_BASE, "002", "Configuration not found"),
    ERR_CODE_CONFIG_ALREADY_EXISTS(ERR_CODE_CONFIG_BASE, "003", "Configuration already exists"),

    // Scope Mapping (06)
    ERR_CODE_SCOPE_BASE(ERR_CODE_OID4VP_BASE, "06", ""),
    ERR_CODE_SCOPE_LOAD_FAILED(ERR_CODE_SCOPE_BASE, "000", "Failed to load scope mappings"),
    ERR_CODE_SCOPE_SAVE_FAILED(ERR_CODE_SCOPE_BASE, "001", "Failed to save scope mapping"),
    ERR_CODE_SCOPE_DELETE_FAILED(ERR_CODE_SCOPE_BASE, "002", "Failed to delete scope mapping"),
    ERR_CODE_SCOPE_NOT_FOUND(ERR_CODE_SCOPE_BASE, "003", "Scope mapping not found"),
    ERR_CODE_SCOPE_DUPLICATE_CREDENTIAL_ID(ERR_CODE_SCOPE_BASE, "004", "Duplicate credential ID"),
    ERR_CODE_SCOPE_DUPLICATE_CLAIM_ID(ERR_CODE_SCOPE_BASE, "005", "Duplicate claim ID"),
    ERR_CODE_SCOPE_UPDATE_FAILED(ERR_CODE_SCOPE_BASE, "006", "Failed to update scope mapping"),
    ERR_CODE_SCOPE_ALREADY_EXISTS(ERR_CODE_SCOPE_BASE, "007", "Scope already exists"),

    // Crypto (07)
    ERR_CODE_CRYPTO_BASE(ERR_CODE_OID4VP_BASE, "07", ""),
    ERR_CODE_CRYPTO_KEY_NOT_CONFIGURED(ERR_CODE_CRYPTO_BASE, "000", "Encryption key not configured"),
    ERR_CODE_CRYPTO_INVALID_KEY_LENGTH(ERR_CODE_CRYPTO_BASE, "001", "Invalid encryption key length"),
    ERR_CODE_CRYPTO_ENCRYPT_FAILED(ERR_CODE_CRYPTO_BASE, "002", "Failed to encrypt"),
    ERR_CODE_CRYPTO_DECRYPT_FAILED(ERR_CODE_CRYPTO_BASE, "003", "Failed to decrypt"),
    ERR_CODE_CRYPTO_INIT_FAILED(ERR_CODE_CRYPTO_BASE, "004", "Failed to initialize encryptor"),
    ERR_CODE_CRYPTO_ALGORITHM_NOT_AVAILABLE(ERR_CODE_CRYPTO_BASE, "005", "Cryptographic algorithm not available"),
    ERR_CODE_CRYPTO_DECODE_FAILED(ERR_CODE_CRYPTO_BASE, "006", "Failed to decode data"),
    ERR_CODE_CRYPTO_UNSUPPORTED_FORMAT(ERR_CODE_CRYPTO_BASE, "007", "Unsupported format"),

    // DCQL (08)
    ERR_CODE_DCQL_BASE(ERR_CODE_OID4VP_BASE, "08", ""),
    ERR_CODE_DCQL_PARSE_FAILED(ERR_CODE_DCQL_BASE, "000", "Failed to parse DCQL"),
    ERR_CODE_DCQL_NO_ADAPTER_FOUND(ERR_CODE_DCQL_BASE, "001", "No adapter found for credential format"),

    // General (99)
    ERR_CODE_GENERAL_BASE(ERR_CODE_OID4VP_BASE, "99", ""),
    ERR_CODE_GENERAL_INVALID_PARAMETER(ERR_CODE_GENERAL_BASE, "000", "Invalid parameter"),
    ERR_CODE_GENERAL_NULL_PARAMETER(ERR_CODE_GENERAL_BASE, "001", "Parameter cannot be null"),
    ERR_CODE_GENERAL_EMPTY_PARAMETER(ERR_CODE_GENERAL_BASE, "002", "Parameter cannot be empty"),
    ERR_CODE_GENERAL_UNEXPECTED_ERROR(ERR_CODE_GENERAL_BASE, "999", "Unexpected error occurred"),
    ;

    private final String code;
    private final String msg;

    OID4VPErrorCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    OID4VPErrorCode(OID4VPErrorCode baseCode, String subCode, String msg) {
        this.code = baseCode.getCode() + subCode;
        this.msg = msg;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    /**
     * Finds an error code by its code string.
     *
     * @param code the code string to search for
     * @return the matching OID4VPErrorCode
     * @throws OID4VPException if no matching code is found
     */
    public static OID4VPErrorCode getByCode(String code) throws OID4VPException {
        for (OID4VPErrorCode errorCode : OID4VPErrorCode.values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        throw new OID4VPException(ERR_CODE_GENERAL_INVALID_PARAMETER, "Unknown error code: " + code);
    }
}