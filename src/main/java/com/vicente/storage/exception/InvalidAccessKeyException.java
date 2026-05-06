package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class InvalidAccessKeyException extends ApiException {
    public InvalidAccessKeyException(String message) {
        super(message, ErrorCode.INVALID_ACCESS_KEY_ERROR);
    }

    public InvalidAccessKeyException(String message, Throwable cause) {
        super(message, ErrorCode.INVALID_ACCESS_KEY_ERROR,  cause);
    }
}
