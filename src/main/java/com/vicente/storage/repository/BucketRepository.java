package com.vicente.storage.repository;

import com.vicente.storage.domain.Bucket;

public interface BucketRepository {
    void saveIfNotExists(Bucket data);
    void save(Bucket data);
    boolean existsByName(String bucketName);
}
