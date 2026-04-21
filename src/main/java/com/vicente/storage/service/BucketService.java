package com.vicente.storage.service;

public interface BucketService {
    String createBucketIfNotExists(String bucketName, String accessKey);
}
