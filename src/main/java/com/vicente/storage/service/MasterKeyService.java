package com.vicente.storage.service;

import javax.crypto.SecretKey;

public interface MasterKeyService {
    Long ensureActiveMasterKey();
    void loadMasterKeyIntoMemory(Long version);
    SecretKey getUsableKey(Long id);
    Long getActiveMasterKeyIdByVersion(Long versionMasterKey);
}
