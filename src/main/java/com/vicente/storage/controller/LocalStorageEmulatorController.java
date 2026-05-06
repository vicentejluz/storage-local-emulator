package com.vicente.storage.controller;

import com.vicente.storage.dto.UploadMetadataDTO;
import com.vicente.storage.service.LocalStorageEmulatorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Validated
public class LocalStorageEmulatorController {
    private final LocalStorageEmulatorService localStorageEmulatorService;

    public LocalStorageEmulatorController(LocalStorageEmulatorService localStorageEmulatorService) {
        this.localStorageEmulatorService = localStorageEmulatorService;
    }

    @PutMapping(value = "/objects/{bucket}/{*objectKey}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @PathVariable
            @NotBlank(message = "Bucket is required")
            String bucket,

            @PathVariable
            @NotBlank(message = "Object key is required")
            @Size(max = 512, message = "Object key must not exceed 512 characters")
            String objectKey,

            @RequestHeader("Content-Type")
            @NotBlank(message = "Content-Type is required")
            String contentType,

            @RequestHeader(value = "Content-Length")
            @NotNull(message = "Content-Length is required")
            @Positive(message = "Content-Length must be greater than zero")
            Long contentLength,

            @RequestHeader(value = "Content-Disposition", required = false)
            String disposition,

            @RequestHeader(value = "X-Content-MD5", required = false)
            String checksum,

            @RequestHeader("X-Access-Key")
            @NotBlank(message = "Access key is required")
            @Size(min = 3, max = 64, message = "Access key must be between 3 and 64 characters")
            String accessKey,

            @RequestHeader("X-Signature")
            @NotBlank(message = "Signature is required")
            String signature,

            @RequestHeader("X-Timestamp")
            @NotNull(message = "Timestamp is required")
            Instant timestamp,

            HttpServletRequest request
    ) {

        InputStream fileStream;
        try {
            fileStream = request.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        UploadMetadataDTO metadata = new UploadMetadataDTO(
                contentType,
                disposition,
                checksum,
                contentLength,
                objectKey,
                bucket
        );

        String etag;
        etag = localStorageEmulatorService.saveFile(fileStream, metadata);

        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, "\"" + etag + "\"")
                .body(Map.of(
                        "bucket", bucket,
                        "objectKey", objectKey,
                        "etag", etag
        ));
    }
}
