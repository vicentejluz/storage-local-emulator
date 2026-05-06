package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class BucketAlreadyExistsException extends ApiException {
    public BucketAlreadyExistsException(String message) {
        super(message, ErrorCode.BUCKET_ALREADY_EXISTS_ERROR);
    }

    public BucketAlreadyExistsException(String message, Throwable cause) {
        super(message, ErrorCode.BUCKET_ALREADY_EXISTS_ERROR,  cause);
    }
}
