package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class MasterKeyStorageException extends ApiException {
    public MasterKeyStorageException(String message) {
        super(message, ErrorCode.MASTER_KEY_STORAGE_ERROR);
    }

    public MasterKeyStorageException(String message, Throwable cause) {
        super(message, ErrorCode.MASTER_KEY_STORAGE_ERROR, cause);
    }
}
