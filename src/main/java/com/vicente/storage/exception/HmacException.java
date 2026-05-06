package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class HmacException extends ApiException {
    public HmacException(String message) {
        super(message, ErrorCode.HMAC_ERROR);
    }

    public HmacException(String message, Throwable cause) {
        super(message, ErrorCode.HMAC_ERROR, cause);
    }
}
