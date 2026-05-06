package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class InvalidSignatureException extends ApiException {
    public InvalidSignatureException(String message) {
        super(message, ErrorCode.INVALID_SIGNATURE_ERROR);
    }

    public InvalidSignatureException(String message, Throwable cause) {
        super(message, ErrorCode.INVALID_SIGNATURE_ERROR,  cause);
    }
}
