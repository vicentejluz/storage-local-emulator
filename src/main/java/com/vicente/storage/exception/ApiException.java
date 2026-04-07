package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public abstract class ApiException extends RuntimeException {
    private final ErrorCode errorCode;

    protected ApiException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return errorCode.status();
    }

    public String getError() {
        return errorCode.value();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
