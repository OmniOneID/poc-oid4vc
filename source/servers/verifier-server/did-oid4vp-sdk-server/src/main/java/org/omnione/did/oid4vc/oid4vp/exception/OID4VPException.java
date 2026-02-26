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
 * Base exception class for OID4VP SDK.
 */
public class OID4VPException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Error code
     */
    protected final String errorCode;

    /**
     * Error message from error code
     */
    protected final String errorMsg;

    /**
     * Additional error reason/details
     */
    protected final String errorReason;

    /**
     * Constructs an OID4VPException with an error code enum.
     *
     * @param errorEnum the error code enum
     */
    public OID4VPException(OID4VPErrorCodeInterface errorEnum) {
        super(formatMessage(errorEnum.getCode(), errorEnum.getMsg(), null));
        this.errorCode = errorEnum.getCode();
        this.errorMsg = errorEnum.getMsg();
        this.errorReason = null;
    }

    /**
     * Constructs an OID4VPException with an error code enum and additional reason.
     *
     * @param errorEnum   the error code enum
     * @param errorReason additional error reason/details
     */
    public OID4VPException(OID4VPErrorCodeInterface errorEnum, String errorReason) {
        super(formatMessage(errorEnum.getCode(), errorEnum.getMsg(), errorReason));
        this.errorCode = errorEnum.getCode();
        this.errorMsg = errorEnum.getMsg();
        this.errorReason = errorReason;
    }

    /**
     * Constructs an OID4VPException with an error code enum and cause.
     *
     * @param errorEnum the error code enum
     * @param cause     the cause of the exception
     */
    public OID4VPException(OID4VPErrorCodeInterface errorEnum, Throwable cause) {
        super(formatMessage(errorEnum.getCode(), errorEnum.getMsg(), null), cause);
        this.errorCode = errorEnum.getCode();
        this.errorMsg = errorEnum.getMsg();
        this.errorReason = cause != null ? cause.getMessage() : null;
    }

    /**
     * Constructs an OID4VPException with an error code enum, reason, and cause.
     *
     * @param errorEnum   the error code enum
     * @param errorReason additional error reason/details
     * @param cause       the cause of the exception
     */
    public OID4VPException(OID4VPErrorCodeInterface errorEnum, String errorReason, Throwable cause) {
        super(formatMessage(errorEnum.getCode(), errorEnum.getMsg(), errorReason), cause);
        this.errorCode = errorEnum.getCode();
        this.errorMsg = errorEnum.getMsg();
        this.errorReason = errorReason;
    }

    /**
     * Constructs an OID4VPException with explicit error code and message.
     *
     * @param errorCode the error code
     * @param errorMsg  the error message
     */
    public OID4VPException(String errorCode, String errorMsg) {
        super(formatMessage(errorCode, errorMsg, null));
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
        this.errorReason = null;
    }

    /**
     * Constructs an OID4VPException with explicit error code, message, and reason.
     *
     * @param errorCode   the error code
     * @param errorMsg    the error message
     * @param errorReason additional error reason/details
     */
    public OID4VPException(String errorCode, String errorMsg, String errorReason) {
        super(formatMessage(errorCode, errorMsg, errorReason));
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
        this.errorReason = errorReason;
    }

    /**
     * Returns the error code.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the error message.
     *
     * @return the error message
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * Returns the error reason.
     *
     * @return the error reason, or null if not set
     */
    public String getErrorReason() {
        return errorReason;
    }

    /**
     * Formats the exception message.
     */
    private static String formatMessage(String code, String msg, String reason) {
        StringBuilder sb = new StringBuilder();
        sb.append("ErrorCode: ").append(code);
        sb.append(", Message: ").append(msg);
        if (reason != null && !reason.isEmpty()) {
            sb.append(", Reason: ").append(reason);
        }
        return sb.toString();
    }
}