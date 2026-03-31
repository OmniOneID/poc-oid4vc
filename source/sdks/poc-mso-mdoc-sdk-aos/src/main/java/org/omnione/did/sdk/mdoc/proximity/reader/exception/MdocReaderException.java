package org.omnione.did.sdk.mdoc.proximity.reader.exception;

public class MdocReaderException extends Exception {
    private final MdocReaderErrorCode errorCode;

    public MdocReaderException(MdocReaderErrorCode errorCode) {
        super("[" + errorCode.getErrorCodeString() + "] " + errorCode.getMsg());
        this.errorCode = errorCode;
    }

    public MdocReaderException(MdocReaderErrorCode errorCode, String detail) {
        super("[" + errorCode.getErrorCodeString() + "] " + errorCode.getMsg() + ": " + detail);
        this.errorCode = errorCode;
    }

    public MdocReaderException(MdocReaderErrorCode errorCode, Throwable cause) {
        super("[" + errorCode.getErrorCodeString() + "] " + errorCode.getMsg(), cause);
        this.errorCode = errorCode;
    }

    public MdocReaderErrorCode getErrorCode() { return errorCode; }
    public String getCode() { return errorCode.getErrorCodeString(); }
}
