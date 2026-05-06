package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class BucketPersistenceException extends ApiException {
    public BucketPersistenceException(String message) {
        super(message, ErrorCode.BUCKET_PERSISTENCE_ERROR);
    }

    public BucketPersistenceException(String message, Throwable cause) {
        super(message, ErrorCode.BUCKET_PERSISTENCE_ERROR, cause);
    }
}
