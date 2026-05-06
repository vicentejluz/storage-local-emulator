package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class MissingAuthHeaderException extends ApiException {
    public MissingAuthHeaderException(String message) {
        super(message, ErrorCode.MISSING_AUTH_HEADER_ERROR);
    }

    public MissingAuthHeaderException(String message, Throwable cause) {
        super(message, ErrorCode.MISSING_AUTH_HEADER_ERROR,  cause);
    }
}
