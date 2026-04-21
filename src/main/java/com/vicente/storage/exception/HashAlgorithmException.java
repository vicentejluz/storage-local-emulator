package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class HashAlgorithmException extends ApiException {
    public HashAlgorithmException(String message) {
        super(message, ErrorCode.HASH_ALGORITHM_ERROR);
    }

    public HashAlgorithmException(String message, Throwable cause) {
        super(message, ErrorCode.HASH_ALGORITHM_ERROR, cause);
    }
}
