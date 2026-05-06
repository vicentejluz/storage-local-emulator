package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class InvalidTimestampException extends ApiException {
    public InvalidTimestampException(String message) {
        super(message, ErrorCode.INVALID_TIMESTAMP_ERROR);
    }

    public InvalidTimestampException(String message, Throwable cause) {
        super(message, ErrorCode.INVALID_TIMESTAMP_ERROR,  cause);
    }
}
