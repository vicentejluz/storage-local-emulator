package com.vicente.storage.exception.model;

import java.time.Instant;

public record StandardError(
        String error,
        String message,
        Integer status,
        String requestId,
        Instant timestamp
){
}
