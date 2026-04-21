package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class ChecksumMismatchException extends ApiException{
    public ChecksumMismatchException(String message) {
        super(message, ErrorCode.CHECKSUM_MISMATCH_ERROR);
    }

    public ChecksumMismatchException(String message, Throwable cause) {
        super(message, ErrorCode.CHECKSUM_MISMATCH_ERROR, cause);
    }
}
