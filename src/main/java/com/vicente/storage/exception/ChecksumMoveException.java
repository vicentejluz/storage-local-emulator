package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class ChecksumMoveException extends ApiException {
    public ChecksumMoveException(String message) {
        super(message, ErrorCode.CHECKSUM_MOVE_ERROR);

    }

    public ChecksumMoveException(String message, Throwable cause) {
        super(message, ErrorCode.CHECKSUM_MOVE_ERROR, cause);
    }
}
