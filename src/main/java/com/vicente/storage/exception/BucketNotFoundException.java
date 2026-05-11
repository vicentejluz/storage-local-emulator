package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class BucketNotFoundException extends ApiException {
    public BucketNotFoundException(String message) {
        super(message, ErrorCode.BUCKET_NOT_FOUND_ERROR);
    }

    public BucketNotFoundException(String message, Throwable cause) {
        super(message, ErrorCode.BUCKET_NOT_FOUND_ERROR,  cause);
    }
}
