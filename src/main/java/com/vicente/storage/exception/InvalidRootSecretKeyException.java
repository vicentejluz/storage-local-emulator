package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class InvalidRootSecretKeyException extends ApiException {
    public InvalidRootSecretKeyException(String message) {
        super(message, ErrorCode.INVALID_ROOT_SECRET_KEY_ERROR);
    }

    public InvalidRootSecretKeyException(String message, Throwable cause) {
        super(message, ErrorCode.INVALID_ROOT_SECRET_KEY_ERROR, cause);
    }
}
