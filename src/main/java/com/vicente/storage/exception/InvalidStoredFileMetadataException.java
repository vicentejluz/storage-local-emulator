package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class InvalidStoredFileMetadataException extends ApiException {
    public InvalidStoredFileMetadataException(String message) {
        super(message, ErrorCode.INVALID_STORED_FILE_METADATA_ERROR);
    }

    public InvalidStoredFileMetadataException(String message, Throwable cause) {
        super(message, ErrorCode.INVALID_STORED_FILE_METADATA_ERROR,  cause);
    }
}
