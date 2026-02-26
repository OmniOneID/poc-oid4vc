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

package org.omnione.did.oid4vc.oid4vci.exception;

/**
 * Error codes for OID4VCI SDK.
 */
public enum OID4VCIErrorCode implements OID4VCIErrorCodeInterface {

    // Base
    ERR_CODE_OID4VCI_BASE("SSOID4VCI", ""),

    // Credential Offer (01)
    ERR_CODE_OFFER_BASE(ERR_CODE_OID4VCI_BASE, "01", ""),
    ERR_CODE_OFFER_NOT_FOUND(ERR_CODE_OFFER_BASE, "000", "Credential offer not found"),
    ERR_CODE_OFFER_EXPIRED(ERR_CODE_OFFER_BASE, "001", "Credential offer has expired"),
    ERR_CODE_OFFER_INVALID_STATUS(ERR_CODE_OFFER_BASE, "002", "Invalid credential offer status"),
    ERR_CODE_OFFER_GENERATE_FAILED(ERR_CODE_OFFER_BASE, "003", "Failed to generate credential offer"),

    // Issuance (02)
    ERR_CODE_ISSUE_BASE(ERR_CODE_OID4VCI_BASE, "02", ""),
    ERR_CODE_ISSUE_INVALID_REQUEST(ERR_CODE_ISSUE_BASE, "000", "Invalid issuance request"),
    ERR_CODE_ISSUE_UNSUPPORTED_FORMAT(ERR_CODE_ISSUE_BASE, "001", "Unsupported credential format"),
    ERR_CODE_ISSUE_CREDENTIAL_NOT_FOUND(ERR_CODE_ISSUE_BASE, "002", "Requested credential configuration not found"),
    ERR_CODE_ISSUE_GENERATE_FAILED(ERR_CODE_ISSUE_BASE, "003", "Failed to generate credential"),
    ERR_CODE_ISSUE_USER_NOT_FOUND(ERR_CODE_ISSUE_BASE, "004", "User not found"),
    ERR_CODE_ISSUE_DEFERRED_TRANSACTION_NOT_FOUND(ERR_CODE_ISSUE_BASE, "005", "Deferred transaction not found"),

    // Proof / Nonce (03)
    ERR_CODE_PROOF_BASE(ERR_CODE_OID4VCI_BASE, "03", ""),
    ERR_CODE_PROOF_INVALID(ERR_CODE_PROOF_BASE, "000", "Invalid proof of possession"),
    ERR_CODE_PROOF_MISSING(ERR_CODE_PROOF_BASE, "001", "Proof of possession is missing"),
    ERR_CODE_PROOF_UNSUPPORTED_TYPE(ERR_CODE_PROOF_BASE, "002", "Unsupported proof type"),
    ERR_CODE_PROOF_INVALID_NONCE(ERR_CODE_PROOF_BASE, "003", "Invalid or expired c_nonce"),
    ERR_CODE_PROOF_PARSE_FAILED(ERR_CODE_PROOF_BASE, "004", "Failed to parse proof"),

    // JWS/JWT (04)
    ERR_CODE_JWS_BASE(ERR_CODE_OID4VCI_BASE, "04", ""),
    ERR_CODE_JWS_SIGN_FAILED(ERR_CODE_JWS_BASE, "000", "Failed to sign JWT"),
    ERR_CODE_JWS_VERIFY_FAILED(ERR_CODE_JWS_BASE, "001", "Failed to verify JWS"),
    ERR_CODE_JWS_INVALID_FORMAT(ERR_CODE_JWS_BASE, "002", "Invalid JWT format"),
    ERR_CODE_JWS_UNSUPPORTED_ALGORITHM(ERR_CODE_JWS_BASE, "003", "Unsupported signing algorithm"),

    // Metadata (05)
    ERR_CODE_METADATA_BASE(ERR_CODE_OID4VCI_BASE, "05", ""),
    ERR_CODE_METADATA_LOAD_FAILED(ERR_CODE_METADATA_BASE, "000", "Failed to load issuer metadata"),
    ERR_CODE_METADATA_INVALID_FORMAT(ERR_CODE_METADATA_BASE, "001", "Invalid metadata format"),
    ERR_CODE_METADATA_MISSING_REQUIRED_FIELD(ERR_CODE_METADATA_BASE, "002", "Required metadata field is missing"),

    // Store (06)
    ERR_CODE_STORE_BASE(ERR_CODE_OID4VCI_BASE, "06", ""),
    ERR_CODE_STORE_SAVE_FAILED(ERR_CODE_STORE_BASE, "000", "Failed to save to store"),
    ERR_CODE_STORE_LOAD_FAILED(ERR_CODE_STORE_BASE, "001", "Failed to load from store"),
    ERR_CODE_STORE_DELETE_FAILED(ERR_CODE_STORE_BASE, "002", "Failed to delete from store"),

    // General (99)
    ERR_CODE_GENERAL_BASE(ERR_CODE_OID4VCI_BASE, "99", ""),
    ERR_CODE_GENERAL_INVALID_PARAMETER(ERR_CODE_GENERAL_BASE, "000", "Invalid parameter"),
    ERR_CODE_GENERAL_NULL_PARAMETER(ERR_CODE_GENERAL_BASE, "001", "Parameter cannot be null"),
    ERR_CODE_GENERAL_EMPTY_PARAMETER(ERR_CODE_GENERAL_BASE, "002", "Parameter cannot be empty"),
    ERR_CODE_GENERAL_UNEXPECTED_ERROR(ERR_CODE_GENERAL_BASE, "999", "Unexpected error occurred"),
    ;

    private final String code;
    private final String msg;

    OID4VCIErrorCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    OID4VCIErrorCode(OID4VCIErrorCode baseCode, String subCode, String msg) {
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
     * @return the matching OID4VCIErrorCode
     * @throws OID4VCIException if no matching code is found
     */
    public static OID4VCIErrorCode getByCode(String code) throws OID4VCIException {
        for (OID4VCIErrorCode errorCode : OID4VCIErrorCode.values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        throw new OID4VCIException(ERR_CODE_GENERAL_INVALID_PARAMETER, "Unknown error code: " + code);
    }
}
