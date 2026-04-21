package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class InvalidStoragePathException extends ApiException {
    public InvalidStoragePathException(String message) {
        super(message, ErrorCode.INVALID_STORAGE_PATH_ERROR);
    }

    public InvalidStoragePathException(String message, Throwable cause) {
        super(message, ErrorCode.INVALID_STORAGE_PATH_ERROR, cause);
    }
}
