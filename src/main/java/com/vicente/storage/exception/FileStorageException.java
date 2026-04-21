package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class FileStorageException extends ApiException {
    public FileStorageException(String message) {
        super(message, ErrorCode.FILE_STORAGE_ERROR);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, ErrorCode.FILE_STORAGE_ERROR, cause);
    }
}
