package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class InvalidStoredFileMetadataException extends ApiException {

    public InvalidStoredFileMetadataException(String message) {
        super(message, ErrorCode.INVALID_METADATA);
    }
}
