package com.vicente.storage.service;

import com.vicente.storage.domain.AccessKey;

public interface AccessKeyService {
    Long initializeRootAccessKey(String accessKey, String secretKey, Long versionMasterKey);
    AccessKey findByAccessKey(String accessKey);
    byte[] getDecryptedSecret(String secretKey, Long versionMasterKey);
}
