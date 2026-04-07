package com.vicente.storage.dao;

import com.vicente.storage.domain.entity.StoredFileMetadata;

import java.util.List;
import java.util.Optional;

public interface StoredFileMetadataDAO {
    void save(StoredFileMetadata data);

    Boolean deleteByBucketAndObjectKey(String bucket, String objectKey);

    Integer deleteAllByBucketAndPath(String bucket, String path);

    Integer deleteAllByBucket(String bucket);

    Optional<StoredFileMetadata> findByBucketAndObjectKey(String bucket, String objectKey);

    Boolean existsByBucketAndObjectKey(String bucket, String objectKey);

    Integer countByBucket(String bucket);

    List<StoredFileMetadata> findAllByBucket(String bucket);

    List<StoredFileMetadata> findAllByPath(String path);

    List<StoredFileMetadata> findAllByBucketAndPath(String bucket, String path);

    List<StoredFileMetadata> findAllByBucket(String bucket, int limit, int offset);

    List<StoredFileMetadata> findAllByPath(String path,  int limit, int offset);

    List<StoredFileMetadata> findAllByBucketAndPath(String bucket, String path, int limit, int offset);
}
