package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class FileCleanupException extends ApiException {

    public FileCleanupException(String message) {
        super(message, ErrorCode.FILE_CLEANUP_ERROR);
    }

    public FileCleanupException(String message, Throwable cause) {
        super(message, ErrorCode.FILE_CLEANUP_ERROR, cause);
    }
}
