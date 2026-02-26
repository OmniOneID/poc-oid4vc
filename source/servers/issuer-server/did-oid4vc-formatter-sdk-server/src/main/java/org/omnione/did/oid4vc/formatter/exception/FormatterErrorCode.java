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

package org.omnione.did.oid4vc.formatter.exception;

/**
 * Error codes for OID4VC Formatter module.
 *
 * Code format: SSFMT + [category 2-digit] + [detail 3-digit]
 *   - 01: VC Generation
 *   - 02: VP Token Processing
 *   - 03: Crypto / Key
 *   - 99: General
 */
public enum FormatterErrorCode implements FormatterErrorCodeInterface {

    // Base
    ERR_CODE_FORMATTER_BASE("SSFMT", ""),

    // ========================================================================================
    // VC Generation (01)
    // ========================================================================================
    ERR_CODE_VC_BASE(ERR_CODE_FORMATTER_BASE, "01", ""),
    ERR_CODE_VC_GENERATE_FAILED(ERR_CODE_VC_BASE, "000", "VC generation failed"),
    ERR_CODE_VC_INVALID_PARAMETER(ERR_CODE_VC_BASE, "001", "Invalid parameter provided for VC generation"),
    ERR_CODE_VC_UNSUPPORTED_FORMAT(ERR_CODE_VC_BASE, "002", "Unsupported VC format"),

    // ========================================================================================
    // VP Token Processing (02)
    // ========================================================================================
    ERR_CODE_VP_BASE(ERR_CODE_FORMATTER_BASE, "02", ""),
    ERR_CODE_VP_TOKEN_NULL(ERR_CODE_VP_BASE, "000", "VP Token cannot be null or empty"),
    ERR_CODE_VP_TOKEN_PARSE_FAILED(ERR_CODE_VP_BASE, "001", "Failed to parse VP Token"),
    ERR_CODE_VP_INVALID_CREDENTIAL(ERR_CODE_VP_BASE, "002", "Invalid credential format"),
    ERR_CODE_VP_NO_VERIFIER_FOUND(ERR_CODE_VP_BASE, "003", "No suitable verifier found"),
    ERR_CODE_VP_VERIFICATION_FAILED(ERR_CODE_VP_BASE, "004", "VP verification failed"),
    ERR_CODE_X5C_VALIDATION_NOT_SUPPORTED(ERR_CODE_VP_BASE, "005", "X.509 certificate chain validation is not supported for this format"),

    // ========================================================================================
    // Crypto / Key (03)
    // ========================================================================================
    ERR_CODE_CRYPTO_BASE(ERR_CODE_FORMATTER_BASE, "03", ""),
    ERR_CODE_CRYPTO_ALGORITHM_NOT_AVAILABLE(ERR_CODE_CRYPTO_BASE, "000", "Cryptographic algorithm not available"),
    ERR_CODE_CRYPTO_INVALID_KEY(ERR_CODE_CRYPTO_BASE, "001", "Invalid key"),
    ERR_CODE_CRYPTO_DECODE_FAILED(ERR_CODE_CRYPTO_BASE, "002", "Failed to decode data"),

    // ========================================================================================
    // General (99)
    // ========================================================================================
    ERR_CODE_GENERAL_BASE(ERR_CODE_FORMATTER_BASE, "99", ""),
    ERR_CODE_GENERAL_INVALID_PARAMETER(ERR_CODE_GENERAL_BASE, "000", "Invalid parameter"),
    ERR_CODE_GENERAL_NULL_PARAMETER(ERR_CODE_GENERAL_BASE, "001", "Parameter cannot be null"),
    ERR_CODE_GENERAL_UNEXPECTED_ERROR(ERR_CODE_GENERAL_BASE, "999", "Unexpected error occurred"),
    ;

    private final String code;
    private final String msg;

    FormatterErrorCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    FormatterErrorCode(FormatterErrorCode baseCode, String subCode, String msg) {
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
     * @return the matching FormatterErrorCode
     * @throws FormatterException if no matching code is found
     */
    public static FormatterErrorCode getByCode(String code) throws FormatterException {
        for (FormatterErrorCode errorCode : FormatterErrorCode.values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        throw new FormatterException(ERR_CODE_GENERAL_INVALID_PARAMETER, "Unknown error code: " + code);
    }
}
