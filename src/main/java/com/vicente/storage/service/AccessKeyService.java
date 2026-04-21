package com.vicente.storage.service;

public interface AccessKeyService {
    void initializeAndRotateAccessKeys(String accessKey, String secretKey, Long versionMasterKey);
}
