package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class InvalidUploadMetadataException extends ApiException {
    public InvalidUploadMetadataException(String message) {
        super(message, ErrorCode.INVALID_UPLOAD_METADATA_ERROR);
    }

    public InvalidUploadMetadataException(String message, Throwable cause) {
        super(message, ErrorCode.INVALID_UPLOAD_METADATA_ERROR, cause);
    }
}
