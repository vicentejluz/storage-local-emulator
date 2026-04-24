package com.vicente.storage.controller;

import com.vicente.storage.dto.UploadMetadataDTO;
import com.vicente.storage.service.LocalStorageEmulatorService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/")
public class LocalStorageEmulatorController {
    private final LocalStorageEmulatorService localStorageEmulatorService;

    public LocalStorageEmulatorController(LocalStorageEmulatorService localStorageEmulatorService) {
        this.localStorageEmulatorService = localStorageEmulatorService;
    }

    @PostMapping(value = "/upload/{bucket}/{*objectKey}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @RequestPart("file") MultipartFile file,
            @PathVariable String bucket,
            @PathVariable String objectKey
    ) {

        UploadMetadataDTO metadata = new UploadMetadataDTO(
                file.getContentType(),
                "attachment; filename=\"" + file.getOriginalFilename() + "\"",
                null,
                file.getSize(),
                objectKey,
                bucket
        );

        String etag;
        try {
            etag = localStorageEmulatorService.saveFile(file.getInputStream(), metadata);
        } catch (IOException e) {
            System.out.println("chegou aqui2");
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(Map.of(
                "etag", etag,
                "bucket", bucket,
                "objectKey", objectKey
        ));
    }
}
