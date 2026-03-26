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

package org.omnione.did.sdk.sdjwt.exception;

/**
 * Error codes for SD-JWT module.
 */
public enum SDJWTErrorCode implements SDJWTErrorCodeInterface {
    ERR_CODE_GENERAL_BASE("MSDKSDJ", "00000", "SD-JWT General Error"),

    // ========== General Errors (00xxx) ==========
    ERR_CODE_GENERAL_INVALID_PARAMETER("MSDKSDJ", "00001", "Invalid parameter"),
    ERR_CODE_GENERAL_NULL_PARAMETER("MSDKSDJ", "00002", "Parameter cannot be null"),
    ERR_CODE_GENERAL_EMPTY_PARAMETER("MSDKSDJ", "00003", "Parameter cannot be empty"),
    ERR_CODE_GENERAL_UNSUPPORTED_OPERATION("MSDKSDJ", "00004", "Unsupported operation"),

    // ========== Crypto Errors (01xxx) ==========
    ERR_CODE_CRYPTO_BASE("MSDKSDJ", "01000", "Crypto Error"),
    ERR_CODE_CRYPTO_INVALID_KEY("MSDKSDJ", "01001", "Invalid key"),
    ERR_CODE_CRYPTO_SIGNATURE_FAILED("MSDKSDJ", "01002", "Signature operation failed"),
    ERR_CODE_CRYPTO_SIGNATURE_VERIFICATION_FAILED("MSDKSDJ", "01003", "Signature verification failed"),
    ERR_CODE_CRYPTO_UNSUPPORTED_ALGORITHM("MSDKSDJ", "01004", "Unsupported algorithm"),
    ERR_CODE_CRYPTO_UNSUPPORTED_KEY_TYPE("MSDKSDJ", "01005", "Unsupported key type"),
    ERR_CODE_CRYPTO_ALGORITHM_NOT_AVAILABLE("MSDKSDJ", "01006", "Algorithm not available"),
    ERR_CODE_CRYPTO_INVALID_SIGNATURE_FORMAT("MSDKSDJ", "01007", "Invalid signature format"),

    // ========== JWT Errors (02xxx) ==========
    ERR_CODE_JWT_BASE("MSDKSDJ", "02000", "JWT Error"),
    ERR_CODE_JWT_INVALID_FORMAT("MSDKSDJ", "02001", "Invalid JWT format"),
    ERR_CODE_JWT_PARSE_FAILED("MSDKSDJ", "02002", "JWT parse failed"),
    ERR_CODE_JWT_MISSING_HEADER("MSDKSDJ", "02003", "JWT header missing"),
    ERR_CODE_JWT_MISSING_PAYLOAD("MSDKSDJ", "02004", "JWT payload missing"),
    ERR_CODE_JWT_SIGN_FAILED("MSDKSDJ", "02005", "JWT sign failed"),

    // ========== SD-JWT Errors (03xxx) ==========
    ERR_CODE_SDJWT_BASE("MSDKSDJ", "03000", "SD-JWT Error"),
    ERR_CODE_SDJWT_INVALID_FORMAT("MSDKSDJ", "03001", "Invalid SD-JWT format"),
    ERR_CODE_SDJWT_PARSE_FAILED("MSDKSDJ", "03002", "SD-JWT parse failed"),
    ERR_CODE_SDJWT_BUILD_FAILED("MSDKSDJ", "03003", "SD-JWT build failed"),

    // ========== Disclosure Errors (04xxx) ==========
    ERR_CODE_DISCLOSURE_BASE("MSDKSDJ", "04000", "Disclosure Error"),
    ERR_CODE_DISCLOSURE_INVALID_FORMAT("MSDKSDJ", "04001", "Invalid disclosure format"),
    ERR_CODE_DISCLOSURE_PARSE_FAILED("MSDKSDJ", "04002", "Disclosure parse failed"),
    ERR_CODE_DISCLOSURE_CREATE_FAILED("MSDKSDJ", "04003", "Disclosure create failed"),
    ERR_CODE_DISCLOSURE_HASH_MISMATCH("MSDKSDJ", "04004", "Disclosure hash mismatch"),
    ERR_CODE_DISCLOSURE_INVALID_SALT("MSDKSDJ", "04005", "Invalid disclosure salt"),
    ERR_CODE_DISCLOSURE_INVALID_CLAIM_NAME("MSDKSDJ", "04006", "Invalid disclosure claim name"),
    ERR_CODE_DISCLOSURE_ARRAY_ELEMENT_NOT_SUPPORTED("MSDKSDJ", "04007", "Array element disclosure not supported in this context"),

    // ========== Disclosure Frame Errors (05xxx) ==========
    ERR_CODE_FRAME_BASE("MSDKSDJ", "05000", "Disclosure Frame Error"),
    ERR_CODE_FRAME_INVALID_FORMAT("MSDKSDJ", "05001", "Invalid disclosure frame format"),
    ERR_CODE_FRAME_PARSE_FAILED("MSDKSDJ", "05002", "Disclosure frame parse failed"),
    ERR_CODE_FRAME_INVALID_FIELD("MSDKSDJ", "05003", "Invalid field in disclosure frame"),
    ERR_CODE_FRAME_RESERVED_FIELD("MSDKSDJ", "05004", "Reserved field name in disclosure frame"),
    ERR_CODE_FRAME_NON_EXISTENT_CLAIM("MSDKSDJ", "05005", "Disclosure frame references non-existent claim"),
    ERR_CODE_FRAME_INVALID_NESTED_TYPE("MSDKSDJ", "05006", "Invalid nested type in disclosure frame"),

    // ========== Key Binding Errors (06xxx) ==========
    ERR_CODE_KB_BASE("MSDKSDJ", "06000", "Key Binding Error"),
    ERR_CODE_KB_INVALID_FORMAT("MSDKSDJ", "06001", "Invalid key binding JWT format"),
    ERR_CODE_KB_BUILD_FAILED("MSDKSDJ", "06002", "Key binding JWT build failed"),
    ERR_CODE_KB_INVALID_KEY("MSDKSDJ", "06003", "Invalid holder key"),

    // ========== JSON Processing Errors (07xxx) ==========
    ERR_CODE_JSON_BASE("MSDKSDJ", "07000", "JSON Processing Error"),
    ERR_CODE_JSON_PROCESSING_FAILED("MSDKSDJ", "07001", "JSON processing failed"),
    ERR_CODE_JSON_MAPPING_FAILED("MSDKSDJ", "07002", "JSON mapping failed"),
    ERR_CODE_JSON_SERIALIZE_FAILED("MSDKSDJ", "07003", "JSON serialization failed"),
    ERR_CODE_JSON_DESERIALIZE_FAILED("MSDKSDJ", "07004", "JSON deserialization failed"),

    // ========== Encoding Errors (08xxx) ==========
    ERR_CODE_ENCODING_BASE("MSDKSDJ", "08000", "Encoding Error"),
    ERR_CODE_ENCODING_BASE64_FAILED("MSDKSDJ", "08001", "Base64 encoding/decoding failed"),
    ERR_CODE_ENCODING_UNSUPPORTED("MSDKSDJ", "08002", "Unsupported encoding"),
    ERR_CODE_ENCODING_INVALID_FORMAT("MSDKSDJ", "08003", "Invalid encoding format"),

    // ========== Hash Errors (09xxx) ==========
    ERR_CODE_HASH_BASE("MSDKSDJ", "09000", "Hash Error"),
    ERR_CODE_HASH_ALGORITHM_NOT_AVAILABLE("MSDKSDJ", "09001", "Hash algorithm not available"),
    ERR_CODE_HASH_UNSUPPORTED_ALGORITHM("MSDKSDJ", "09002", "Unsupported hash algorithm"),
    ERR_CODE_HASH_COMPUTATION_FAILED("MSDKSDJ", "09003", "Hash computation failed"),

    // ========== Salt Errors (10xxx) ==========
    ERR_CODE_SALT_BASE("MSDKSDJ", "10000", "Salt Error"),
    ERR_CODE_SALT_INVALID_LENGTH("MSDKSDJ", "10001", "Invalid salt length"),
    ERR_CODE_SALT_GENERATION_FAILED("MSDKSDJ", "10002", "Salt generation failed"),

    // ========== Validation Errors (11xxx) ==========
    ERR_CODE_VALIDATION_BASE("MSDKSDJ", "11000", "Validation Error"),
    ERR_CODE_VALIDATION_FAILED("MSDKSDJ", "11001", "Validation failed"),
    ERR_CODE_VALIDATION_CLAIM_NOT_FOUND("MSDKSDJ", "11002", "Required claim not found"),

    // ========== OID4VCI Errors (12xxx) ==========
    ERR_CODE_OID4VCI_BASE("MSDKSDJ", "12000", "OID4VCI Error"),
    ERR_CODE_OID4VCI_ISSUE_FAILED("MSDKSDJ", "12001", "Credential issuance failed"),
    ERR_CODE_OID4VCI_SIGN_FAILED("MSDKSDJ", "12002", "Credential signing failed"),

    ;

    private final String prefix;
    private final String code;
    private final String msg;

    SDJWTErrorCode(String prefix, String code, String msg) {
        this.prefix = prefix;
        this.code = prefix + code;
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

    public String getPrefix() {
        return prefix;
    }

    /**
     * Finds an error code by its code string.
     *
     * @param code the code string to search for
     * @return the matching SDJWTErrorCode
     * @throws SDJWTException if no matching code is found
     */
    public static SDJWTErrorCode getByCode(String code) throws SDJWTException {
        for (SDJWTErrorCode errorCode : SDJWTErrorCode.values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        throw new SDJWTException(ERR_CODE_GENERAL_INVALID_PARAMETER.getMsg() + ": Unknown error code: " + code);
    }
}
