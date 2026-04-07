package com.vicente.storage.service.impl;

import com.vicente.storage.dao.StoredFileMetadataDAO;
import com.vicente.storage.service.LocalStorageEmulatorService;
import org.springframework.stereotype.Service;

@Service
public class LocalStorageEmulatorServiceImpl implements LocalStorageEmulatorService {
    private final StoredFileMetadataDAO storedFileMetadataDAO;

    public LocalStorageEmulatorServiceImpl(StoredFileMetadataDAO storedFileMetadataDAO) {
        this.storedFileMetadataDAO = storedFileMetadataDAO;
    }
}
