package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class SecretKeyCryptoException extends ApiException {
    public SecretKeyCryptoException(String message) {
        super(message, ErrorCode.SECRET_KEY_CRYPTO_ERROR);
    }

    public SecretKeyCryptoException(String message, Throwable cause) {
        super(message, ErrorCode.SECRET_KEY_CRYPTO_ERROR, cause);
    }
}
