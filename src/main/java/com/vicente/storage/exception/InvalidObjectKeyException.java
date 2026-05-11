package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class InvalidObjectKeyException extends ApiException {
    public InvalidObjectKeyException(String message) {
        super(message, ErrorCode.INVALID_OBJECT_KEY_ERROR);
    }

    public InvalidObjectKeyException(String message, Throwable cause) {
        super(message, ErrorCode.INVALID_OBJECT_KEY_ERROR, cause);
    }
}
