package com.vicente.storage.dto;

import java.time.Instant;

public record AuthRequestDTO(
        String accessKey,
        String signature,
        Instant timestamp,
        String method,
        String path,
        String query,
        String contentType,
        long contentLength) {
}
