package com.vicente.storage.repository;

import com.vicente.storage.domain.Bucket;

import java.util.Optional;

public interface BucketRepository {
    void saveIfNotExists(Bucket data);
    void save(Bucket data);
    boolean existsByName(String bucketName);
    Optional<Long> findIdByNameAndAccessKeyId(String bucketName, long accessKeyId);
}
