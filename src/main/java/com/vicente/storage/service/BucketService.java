package com.vicente.storage.service;

public interface BucketService {
    void createBucketIfNotExists(String bucketName, Long accessKeyId);
    void createBucket(String bucketName, Long accessKeyId);
}
