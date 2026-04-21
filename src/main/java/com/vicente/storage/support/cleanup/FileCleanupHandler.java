package com.vicente.storage.support.cleanup;

import com.vicente.storage.repository.StoredFileMetadataRepository;
import com.vicente.storage.support.retry.StorageRetryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class FileCleanupHandler {
    private final StorageRetryExecutor storageRetryExecutor;
    private final StoredFileMetadataRepository storedFileMetadataRepository;
    private static final Logger logger = LoggerFactory.getLogger(FileCleanupHandler.class);

    public FileCleanupHandler(StorageRetryExecutor storageRetryExecutor, StoredFileMetadataRepository storedFileMetadataRepository) {
        this.storageRetryExecutor = storageRetryExecutor;
        this.storedFileMetadataRepository = storedFileMetadataRepository;
    }

    @Async(value = "asyncCleanupExecutor")
    public void triggerAsyncCleanup(String oldPhysicalFileName, Path oldPhysicalPath, Path oldChecksumPath) {
        logger.info("ASYNC THREAD: {}", Thread.currentThread().getName());

        boolean isStillReferenced = storedFileMetadataRepository.existsPhysicalFileName(oldPhysicalFileName);

        if(isStillReferenced) {
            logger.info("Skipping delete. File still referenced in DB: {}", oldPhysicalFileName);
            return;
        }

        logger.debug("File not referenced anymore. Proceeding with cleanup: {}", oldPhysicalFileName);
        storageRetryExecutor.removePreviousFileSafelyRetry(oldPhysicalPath, oldChecksumPath);
    }

    public void deleteFilesIfExists(Path... paths) {
        for (Path path : paths) {
            if (path == null) continue;
            try {
                Files.deleteIfExists(path);
            } catch (IOException ex) {
                logger.warn("Could not delete file {}", path, ex);
            }
        }
    }
}
