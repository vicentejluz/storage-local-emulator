package com.vicente.storage.service;

import com.vicente.storage.dto.UploadMetadataDTO;

import java.io.InputStream;

public interface LocalStorageEmulatorService {
    String saveFile(InputStream fileStream, UploadMetadataDTO metadata) ;
}
