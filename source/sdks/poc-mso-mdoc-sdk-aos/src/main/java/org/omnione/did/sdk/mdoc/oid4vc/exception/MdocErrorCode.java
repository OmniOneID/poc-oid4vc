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

package org.omnione.did.sdk.mdoc.oid4vc.exception;

/**
 * Enumeration of error codes for the mso-MDOC SDK module.
 *
 * <p>Error codes follow the format {@code MSDKMDC{5-digit code}} where the 5-digit code
 * is categorized as follows:</p>
 * <ul>
 *   <li>00xxx - General errors (parameter validation, unsupported operations)</li>
 *   <li>01xxx - Cryptographic errors (key, signature, algorithm)</li>
 *   <li>02xxx - JWT errors (format, parsing, signing)</li>
 *   <li>03xxx - mso-MDOC errors (CBOR format, parsing, building)</li>
 *   <li>04xxx - Disclosure errors (IssuerSignedItem)</li>
 *   <li>05xxx - Disclosure Frame errors</li>
 *   <li>06xxx - Key Binding / DeviceAuth errors</li>
 *   <li>07xxx - JSON processing errors</li>
 *   <li>08xxx - Encoding errors (Base64)</li>
 *   <li>09xxx - Hash errors</li>
 *   <li>10xxx - Salt (random) errors</li>
 *   <li>11xxx - Validation errors</li>
 *   <li>12xxx - (Reserved)</li>
 *   <li>13xxx - OID4VP errors (credential presentation)</li>
 * </ul>
 *
 * @see MdocErrorCodeInterface
 * @see MdocException
 */
public enum MdocErrorCode implements MdocErrorCodeInterface {
    ERR_CODE_GENERAL_BASE("MSDKMDC", "00000", "mso-MDOC General Error"),

    // ========== General Errors (00xxx) ==========
    ERR_CODE_GENERAL_INVALID_PARAMETER("MSDKMDC", "00001", "Invalid parameter"),
    ERR_CODE_GENERAL_NULL_PARAMETER("MSDKMDC", "00002", "Parameter cannot be null"),
    ERR_CODE_GENERAL_EMPTY_PARAMETER("MSDKMDC", "00003", "Parameter cannot be empty"),
    ERR_CODE_GENERAL_UNSUPPORTED_OPERATION("MSDKMDC", "00004", "Unsupported operation"),

    // ========== Crypto Errors (01xxx) ==========
    ERR_CODE_CRYPTO_BASE("MSDKMDC", "01000", "Crypto Error"),
    ERR_CODE_CRYPTO_INVALID_KEY("MSDKMDC", "01001", "Invalid key"),
    ERR_CODE_CRYPTO_SIGNATURE_FAILED("MSDKMDC", "01002", "Signature operation failed"),
    ERR_CODE_CRYPTO_SIGNATURE_VERIFICATION_FAILED("MSDKMDC", "01003", "Signature verification failed"),
    ERR_CODE_CRYPTO_UNSUPPORTED_ALGORITHM("MSDKMDC", "01004", "Unsupported algorithm"),
    ERR_CODE_CRYPTO_UNSUPPORTED_KEY_TYPE("MSDKMDC", "01005", "Unsupported key type"),
    ERR_CODE_CRYPTO_ALGORITHM_NOT_AVAILABLE("MSDKMDC", "01006", "Algorithm not available"),
    ERR_CODE_CRYPTO_INVALID_SIGNATURE_FORMAT("MSDKMDC", "01007", "Invalid signature format"),

    // ========== JWT Errors (02xxx) ==========
    ERR_CODE_JWT_BASE("MSDKMDC", "02000", "JWT Error"),
    ERR_CODE_JWT_INVALID_FORMAT("MSDKMDC", "02001", "Invalid JWT format"),
    ERR_CODE_JWT_PARSE_FAILED("MSDKMDC", "02002", "JWT parse failed"),
    ERR_CODE_JWT_MISSING_HEADER("MSDKMDC", "02003", "JWT header missing"),
    ERR_CODE_JWT_MISSING_PAYLOAD("MSDKMDC", "02004", "JWT payload missing"),
    ERR_CODE_JWT_SIGN_FAILED("MSDKMDC", "02005", "JWT sign failed"),

    // ========== mso-MDOC Errors (03xxx) ==========
    ERR_CODE_MDOC_BASE("MSDKMDC", "03000", "mso-MDOC Error"),
    ERR_CODE_MDOC_INVALID_FORMAT("MSDKMDC", "03001", "Invalid mso-MDOC format"),
    ERR_CODE_MDOC_PARSE_FAILED("MSDKMDC", "03002", "mso-MDOC parse failed"),
    ERR_CODE_MDOC_BUILD_FAILED("MSDKMDC", "03003", "mso-MDOC build failed"),

    // ========== Disclosure Errors (04xxx) ==========
    ERR_CODE_DISCLOSURE_BASE("MSDKMDC", "04000", "Disclosure Error"),
    ERR_CODE_DISCLOSURE_INVALID_FORMAT("MSDKMDC", "04001", "Invalid disclosure format"),
    ERR_CODE_DISCLOSURE_PARSE_FAILED("MSDKMDC", "04002", "Disclosure parse failed"),
    ERR_CODE_DISCLOSURE_CREATE_FAILED("MSDKMDC", "04003", "Disclosure create failed"),
    ERR_CODE_DISCLOSURE_HASH_MISMATCH("MSDKMDC", "04004", "Disclosure hash mismatch"),
    ERR_CODE_DISCLOSURE_INVALID_SALT("MSDKMDC", "04005", "Invalid disclosure salt"),
    ERR_CODE_DISCLOSURE_INVALID_CLAIM_NAME("MSDKMDC", "04006", "Invalid disclosure claim name"),
    ERR_CODE_DISCLOSURE_ARRAY_ELEMENT_NOT_SUPPORTED("MSDKMDC", "04007", "Array element disclosure not supported in this context"),

    // ========== Disclosure Frame Errors (05xxx) ==========
    ERR_CODE_FRAME_BASE("MSDKMDC", "05000", "Disclosure Frame Error"),
    ERR_CODE_FRAME_INVALID_FORMAT("MSDKMDC", "05001", "Invalid disclosure frame format"),
    ERR_CODE_FRAME_PARSE_FAILED("MSDKMDC", "05002", "Disclosure frame parse failed"),
    ERR_CODE_FRAME_INVALID_FIELD("MSDKMDC", "05003", "Invalid field in disclosure frame"),
    ERR_CODE_FRAME_RESERVED_FIELD("MSDKMDC", "05004", "Reserved field name in disclosure frame"),
    ERR_CODE_FRAME_NON_EXISTENT_CLAIM("MSDKMDC", "05005", "Disclosure frame references non-existent claim"),
    ERR_CODE_FRAME_INVALID_NESTED_TYPE("MSDKMDC", "05006", "Invalid nested type in disclosure frame"),

    // ========== Key Binding Errors (06xxx) ==========
    ERR_CODE_KB_BASE("MSDKMDC", "06000", "Key Binding Error"),
    ERR_CODE_KB_INVALID_FORMAT("MSDKMDC", "06001", "Invalid key binding JWT format"),
    ERR_CODE_KB_BUILD_FAILED("MSDKMDC", "06002", "Key binding JWT build failed"),
    ERR_CODE_KB_INVALID_KEY("MSDKMDC", "06003", "Invalid holder key"),

    // ========== JSON Processing Errors (07xxx) ==========
    ERR_CODE_JSON_BASE("MSDKMDC", "07000", "JSON Processing Error"),
    ERR_CODE_JSON_PROCESSING_FAILED("MSDKMDC", "07001", "JSON processing failed"),
    ERR_CODE_JSON_MAPPING_FAILED("MSDKMDC", "07002", "JSON mapping failed"),
    ERR_CODE_JSON_SERIALIZE_FAILED("MSDKMDC", "07003", "JSON serialization failed"),
    ERR_CODE_JSON_DESERIALIZE_FAILED("MSDKMDC", "07004", "JSON deserialization failed"),

    // ========== Encoding Errors (08xxx) ==========
    ERR_CODE_ENCODING_BASE("MSDKMDC", "08000", "Encoding Error"),
    ERR_CODE_ENCODING_BASE64_FAILED("MSDKMDC", "08001", "Base64 encoding/decoding failed"),
    ERR_CODE_ENCODING_UNSUPPORTED("MSDKMDC", "08002", "Unsupported encoding"),
    ERR_CODE_ENCODING_INVALID_FORMAT("MSDKMDC", "08003", "Invalid encoding format"),

    // ========== Hash Errors (09xxx) ==========
    ERR_CODE_HASH_BASE("MSDKMDC", "09000", "Hash Error"),
    ERR_CODE_HASH_ALGORITHM_NOT_AVAILABLE("MSDKMDC", "09001", "Hash algorithm not available"),
    ERR_CODE_HASH_UNSUPPORTED_ALGORITHM("MSDKMDC", "09002", "Unsupported hash algorithm"),
    ERR_CODE_HASH_COMPUTATION_FAILED("MSDKMDC", "09003", "Hash computation failed"),

    // ========== Salt Errors (10xxx) ==========
    ERR_CODE_SALT_BASE("MSDKMDC", "10000", "Salt Error"),
    ERR_CODE_SALT_INVALID_LENGTH("MSDKMDC", "10001", "Invalid salt length"),
    ERR_CODE_SALT_GENERATION_FAILED("MSDKMDC", "10002", "Salt generation failed"),

    // ========== Validation Errors (11xxx) ==========
    ERR_CODE_VALIDATION_BASE("MSDKMDC", "11000", "Validation Error"),
    ERR_CODE_VALIDATION_FAILED("MSDKMDC", "11001", "Validation failed"),
    ERR_CODE_VALIDATION_CLAIM_NOT_FOUND("MSDKMDC", "11002", "Required claim not found"),

    // ========== OID4VP Errors (13xxx) ==========
    ERR_CODE_OID4VP_BASE("MSDKMDC", "13000", "OID4VP Error"),
    ERR_CODE_OID4VP_PARSE_FAILED("MSDKMDC", "13001", "OID4VP mdoc parse failed"),
    ERR_CODE_OID4VP_INVALID_CREDENTIAL("MSDKMDC", "13002", "OID4VP invalid mdoc credential"),
    ERR_CODE_OID4VP_NO_X5CHAIN("MSDKMDC", "13003", "OID4VP no x5chain found in IssuerAuth"),
    ERR_CODE_OID4VP_SIGNATURE_FAILED("MSDKMDC", "13004", "OID4VP IssuerAuth signature verification failed"),
    ERR_CODE_OID4VP_DIGEST_MISMATCH("MSDKMDC", "13005", "OID4VP IssuerSignedItem digest mismatch"),
    ERR_CODE_OID4VP_DEVICE_AUTH_FAILED("MSDKMDC", "13006", "OID4VP DeviceAuth verification failed"),
    ERR_CODE_OID4VP_UNSUPPORTED_KEY("MSDKMDC", "13007", "OID4VP unsupported key type or curve"),

    ;

    private final String prefix;
    private final String code;
    private final String msg;

    MdocErrorCode(String prefix, String code, String msg) {
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
     * @return the matching MDOCErrorCode
     * @throws MdocException if no matching code is found
     */
    public static MdocErrorCode getByCode(String code) throws MdocException {
        for (MdocErrorCode errorCode : MdocErrorCode.values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        throw new MdocException(ERR_CODE_GENERAL_INVALID_PARAMETER.getMsg() + ": Unknown error code: " + code);
    }
}
