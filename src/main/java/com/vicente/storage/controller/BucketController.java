package com.vicente.storage.controller;

import com.vicente.storage.annotation.AuthAccessKey;

import com.vicente.storage.service.BucketService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/buckets")
@Validated
public class BucketController {
    private final BucketService bucketService;


    public BucketController(BucketService bucketService) {
        this.bucketService = bucketService;
    }

    @PutMapping("/{bucket}")
    public ResponseEntity<Void> create(
            @PathVariable("bucket")
            @NotBlank(message = "Bucket is required")
            @Pattern(
                    regexp = "^(?!\\d+\\.\\d+\\.\\d+\\.\\d+$)(?!.*\\.\\.)(?!.*\\.-)(?!.*-\\.)[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$",
                    message = "Invalid bucket name format"
            )
            String bucketName,

            @AuthAccessKey
            Long accessKeyId
    ){
        bucketService.createBucket(bucketName, accessKeyId);

        return ResponseEntity.noContent().build();
    }
}
