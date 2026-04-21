package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class BucketCreationException extends ApiException {
    public BucketCreationException(String message) {
        super(message, ErrorCode.BUCKET_CREATION_ERROR);
    }

    public BucketCreationException(String message, Throwable cause) {
        super(message, ErrorCode.BUCKET_CREATION_ERROR,  cause);
    }
}
