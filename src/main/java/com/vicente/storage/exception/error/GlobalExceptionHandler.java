package com.vicente.storage.exception.error;

import com.vicente.storage.exception.ApiException;
import com.vicente.storage.exception.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<StandardError> handleApiException(
            ApiException e, HttpServletRequest request) {

        StandardError standardError = new StandardError(
                e.getError(),
                e.getMessage(),
                e.getStatus().value(),
                request.getRequestId(),
                Instant.now()
        );

        return ResponseEntity.status(e.getStatus()).body(standardError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleGeneric(HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        String message = "Unexpected error occurred. Please contact support.";

        StandardError standardError = new StandardError(errorCode.value(),
                message, errorCode.status().value(), request.getRequestId(), Instant.now());
        return ResponseEntity.status(errorCode.status()).body(standardError);
    }
}
