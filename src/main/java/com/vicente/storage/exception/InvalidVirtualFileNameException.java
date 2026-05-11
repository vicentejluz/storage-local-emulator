package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class InvalidVirtualFileNameException extends ApiException {
    public InvalidVirtualFileNameException(String message) {
        super(message, ErrorCode.INVALID_VIRTUAL_FILE_NAME_ERROR);
    }

    public InvalidVirtualFileNameException(String message, Throwable cause) {
        super(message, ErrorCode.INVALID_VIRTUAL_FILE_NAME_ERROR, cause);
    }
}
