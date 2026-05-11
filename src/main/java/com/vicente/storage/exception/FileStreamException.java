package com.vicente.storage.exception;

import com.vicente.storage.exception.enums.ErrorCode;

public class FileStreamException extends ApiException {
    public FileStreamException(String message) {
        super(message, ErrorCode.FILE_STREAM_ERROR);
    }

    public FileStreamException(String message, Throwable cause) {
        super(message, ErrorCode.FILE_STREAM_ERROR, cause);
    }
}
