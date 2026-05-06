package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class CryptoSystemException extends ApiException {
    public CryptoSystemException(String message) {
        super(message, ErrorCode.CRYPTO_SYSTEM_ERROR);
    }

    public CryptoSystemException(String message, Throwable cause) {
        super(message, ErrorCode.CRYPTO_SYSTEM_ERROR,  cause);
    }
}
