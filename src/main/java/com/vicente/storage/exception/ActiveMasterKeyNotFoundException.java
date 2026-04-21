package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class ActiveMasterKeyNotFoundException extends ApiException {
    public ActiveMasterKeyNotFoundException(String message) {
        super(message, ErrorCode.ACTIVE_MASTER_KEY_NOT_FOUND_ERROR);
    }

    public ActiveMasterKeyNotFoundException(String message, Throwable cause) {
        super(message, ErrorCode.ACTIVE_MASTER_KEY_NOT_FOUND_ERROR, cause);
    }
}
