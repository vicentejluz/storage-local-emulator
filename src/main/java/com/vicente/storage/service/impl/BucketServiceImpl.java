package com.vicente.storage.service.impl;

import com.vicente.storage.repository.AccessKeyRepository;
import com.vicente.storage.service.BucketService;
import org.springframework.stereotype.Service;

@Service
public class BucketServiceImpl implements BucketService {
    private final AccessKeyRepository accessKeyRepository;

    public BucketServiceImpl(AccessKeyRepository accessKeyRepository) {
        this.accessKeyRepository = accessKeyRepository;
    }

    @Override
    public String createBucketIfNotExists(String bucketName, String accessKey) {
        return "";
    }
}
