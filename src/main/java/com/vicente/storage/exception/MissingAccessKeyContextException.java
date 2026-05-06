package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class MissingAccessKeyContextException extends ApiException {
    public MissingAccessKeyContextException(String message) {
        super(message, ErrorCode.MISSING_ACCESS_KEY_CONTEXT_ERROR);
    }

    public MissingAccessKeyContextException(String message, Throwable cause) {
        super(message, ErrorCode.MISSING_ACCESS_KEY_CONTEXT_ERROR,  cause);
    }
}
