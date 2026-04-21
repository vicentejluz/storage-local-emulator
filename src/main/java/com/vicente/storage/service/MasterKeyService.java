package com.vicente.storage.service;

public interface MasterKeyService {
    Long ensureActiveMasterKey();
    void loadMasterKeyIntoMemory(Long version);
}
