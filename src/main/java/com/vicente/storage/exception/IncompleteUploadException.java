package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class IncompleteUploadException extends ApiException {
    public IncompleteUploadException(String message) {
        super(message, ErrorCode.INCOMPLETE_UPLOAD_ERROR);
    }

    public IncompleteUploadException(String message, Throwable cause) {
        super(message, ErrorCode.INCOMPLETE_UPLOAD_ERROR, cause);
    }
}
