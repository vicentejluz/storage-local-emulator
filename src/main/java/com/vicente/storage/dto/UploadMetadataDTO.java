package com.vicente.storage.dto;

import com.vicente.storage.exception.InvalidUploadMetadataException;

public record UploadMetadataDTO(
       String contentType,
       String contentDisposition,
       String checksum,
       Long contentLength,
       String objectKey,
       String bucket
) {

    public UploadMetadataDTO {
        if (bucket == null || bucket.isBlank()) throw new InvalidUploadMetadataException("Bucket is required");
        if (objectKey == null || objectKey.isBlank()) throw new InvalidUploadMetadataException("Object key is required");
        if (contentLength == null || contentLength <= 0) throw new InvalidUploadMetadataException("Content length must be greater than zero");
        if (contentType == null || contentType.isBlank()) throw new InvalidUploadMetadataException("Content type is required");
    }
}
