package com.vicente.storage.domain;

import com.vicente.storage.domain.base.AuditableEntity;
import com.vicente.storage.exception.InvalidStoredFileMetadataException;

import java.time.LocalDateTime;

public class StoredFileMetadata extends AuditableEntity {
    private String objectKey;
    private String virtualFileName;
    private String physicalFileName;
    private String extension;
    private String contentType;
    private String contentDisposition;
    private long contentLength;
    private String virtualPath;
    private Long bucketId;
    private String etag;


    public StoredFileMetadata(String objectKey, String virtualFileName, String physicalFileName,
                              String contentDisposition, String virtualPath, Long bucketId) {
        this.objectKey = objectKey;
        this.virtualFileName = virtualFileName;
        this.physicalFileName = physicalFileName;
        this.contentDisposition = contentDisposition;
        this.virtualPath = virtualPath;
        this.bucketId = bucketId;
    }

    public StoredFileMetadata(Long id, String objectKey, String virtualFileName, String physicalFileName,
                              String extension, String contentType, String contentDisposition, long contentLength,
                              String virtualPath, Long bucketId, String etag, LocalDateTime createdAt,  LocalDateTime updatedAt) {
    this(objectKey, virtualFileName, physicalFileName, contentDisposition, virtualPath, bucketId);
    this.id = id;
    this.contentType = contentType;
    this.extension = extension;
    this.contentLength = contentLength;
    this.etag = etag;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getVirtualFileName() {
        return virtualFileName;
    }

    public void setVirtualFileName(String virtualFileName) {
        this.virtualFileName = virtualFileName;
    }

    public String getPhysicalFileName() {
        return physicalFileName;
    }

    public void setPhysicalFileName(String physicalFileName) {
        this.physicalFileName = physicalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getContentDisposition() {
        return contentDisposition;
    }

    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public String getVirtualPath() {
        return virtualPath;
    }

    public void setVirtualPath(String virtualPath) {
        this.virtualPath = virtualPath;
    }

    public Long getBucketId() {
        return bucketId;
    }

    public void setBucketId(Long bucketId) {
        this.bucketId = bucketId;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }


    private void validateRequiredLength(String value, int max, String field) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new InvalidStoredFileMetadataException(
                    field + " must not be null and length <= " + max
            );
        }
    }
}