package com.vicente.storage.controller;

import com.vicente.storage.annotation.AuthAccessKey;
import com.vicente.storage.dto.UploadMetadataDTO;
import com.vicente.storage.exception.FileStreamException;
import com.vicente.storage.service.LocalStorageEmulatorService;
import com.vicente.storage.validation.ObjectKeyNormalizer;
import com.vicente.storage.validation.ValidationPatterns;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@Validated
public class LocalStorageEmulatorController {
    private final LocalStorageEmulatorService localStorageEmulatorService;
    private final DataSize maxUploadSize;

    public LocalStorageEmulatorController(LocalStorageEmulatorService localStorageEmulatorService,
                                          @Value("${storage.emulator.max-upload-size}") DataSize maxUploadSize) {
        this.localStorageEmulatorService = localStorageEmulatorService;
        this.maxUploadSize = maxUploadSize;
    }

    @PutMapping(value = "/objects/{bucket}/{*objectKey}")
    public ResponseEntity<?> upload(
            @PathVariable
            @NotBlank(message = "Bucket is required")
            @Pattern(
                    regexp = ValidationPatterns.BUCKET_NAME_REGEX,
                    message = "Bucket name must be 3-63 characters long, contain only lowercase letters, numbers, and hyphens, " +
                            "must not start or end with a hyphen, and cannot contain consecutive hyphens."
            )
            String bucket,

            @PathVariable
            @NotBlank(message = "Object key is required")
            @Size(max = 512, message = "Object key must not exceed 512 characters")
            String objectKey,

            @AuthAccessKey
            Long accessKeyId,

            HttpServletRequest request
    ) {

        if(request.getContentLengthLong() > maxUploadSize.toBytes()) {
            return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                    .body("Upload limit exceeded. Maximum allowed: " + maxUploadSize.toMegabytes() + "MB");
        }

        if(request.getContentLengthLong() < 0){
            return ResponseEntity.status(HttpStatus.LENGTH_REQUIRED).body("The 'Content-Length' header is required.");
        }

        objectKey = ObjectKeyNormalizer.normalize(objectKey);

        UploadMetadataDTO metadata = new UploadMetadataDTO(
                accessKeyId,
                request.getContentType(),
                request.getHeader(HttpHeaders.CONTENT_DISPOSITION),
                request.getHeader("X-Content-MD5"),
                request.getContentLengthLong(), objectKey, bucket
        );

        try {
            String etag = localStorageEmulatorService.saveFile(request.getInputStream(), metadata);

            URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/objects/{bucket}/{objectKey}")
                    .buildAndExpand(bucket, objectKey).encode().toUri();

            return ResponseEntity.created(location)
                    .header(HttpHeaders.ETAG, "\"" + etag + "\"").build();
        } catch (IOException e) {
            throw new FileStreamException("Failed to open HTTP request input stream", e);
        }
    }
}
