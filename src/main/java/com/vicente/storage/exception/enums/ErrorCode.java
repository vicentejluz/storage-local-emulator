package com.vicente.storage.exception.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_METADATA("InvalidMetadata", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("InternalError", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String value;
    private final HttpStatus status;

    ErrorCode(String value, HttpStatus status) {
        this.value = value;
        this.status = status;
    }

    public String value() {
        return value;
    }

    public HttpStatus status() {
        return status;
    }

}
