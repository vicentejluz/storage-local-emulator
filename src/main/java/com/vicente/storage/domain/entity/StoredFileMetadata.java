package com.vicente.storage.domain.entity;

import com.vicente.storage.exception.InvalidStoredFileMetadataException;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;

public record StoredFileMetadata(Long id, String objectKey, String fileName, String physicalFileName, String extension,
                                 String contentType, Long size, String path, String bucket, String checksum,
                                 LocalDateTime createdAt) {
    public StoredFileMetadata {

        validateRequiredLength(objectKey, 512, "objectKey");

        validateRequiredLength(fileName, 255, "fileName");

        validateRequiredLength(physicalFileName, 36, "physicalFileName");

        validateRequiredLength(contentType, 255, "contentType");

        validateRequiredLength(path, 500, "path");

        validateRequiredLength(bucket, 63, "bucket");

        validateRequiredLength(checksum, 32, "checksum");

        if (extension != null && extension.length() > 10) {
            throw new InvalidStoredFileMetadataException("extension length must be <= 10");
        }

        if (size != null && size < 0) {
            throw new InvalidStoredFileMetadataException("size must be >= 0");
        }

    }

    public StoredFileMetadata(String objectKey, String fileName, String physicalFileName,
                              String extension, String contentType, Long size, String path, String bucket,
                              String checksum) {
        this(null, objectKey, fileName, physicalFileName, extension, contentType, size, path, bucket, checksum, null);

    }

    @Override
    public @NonNull String toString() {
        return "StoredFileMetadata{" +
                "id=" + id +
                ", objectKey='" + objectKey + '\'' +
                ", fileName='" + fileName + '\'' +
                ", physicalFileName='" + physicalFileName + '\'' +
                ", extension='" + extension + '\'' +
                ", contentType='" + contentType + '\'' +
                ", size=" + size +
                ", path='" + path + '\'' +
                ", bucket='" + bucket + '\'' +
                ", checksum='" + checksum + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    private void validateRequiredLength(String value, int max, String field) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new InvalidStoredFileMetadataException(
                    field + " must not be null and length <= " + max
            );
        }
    }
}