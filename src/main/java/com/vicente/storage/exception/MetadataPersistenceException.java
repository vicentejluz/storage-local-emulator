package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class MetadataPersistenceException extends ApiException {
    public MetadataPersistenceException(String message) {
        super(message, ErrorCode.METADATA_PERSISTENCE_ERROR);
    }

    public MetadataPersistenceException(String message, Throwable cause) {
        super(message, ErrorCode.METADATA_PERSISTENCE_ERROR, cause);
    }
}
