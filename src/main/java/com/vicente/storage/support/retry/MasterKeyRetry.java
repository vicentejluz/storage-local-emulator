package com.vicente.storage.support.retry;

import com.vicente.storage.domain.MasterKey;

import com.vicente.storage.exception.MasterKeyStorageException;
import com.vicente.storage.repository.MasterKeyRepository;
import io.github.resilience4j.retry.annotation.Retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;

@Component
public class MasterKeyRetry {
    private final MasterKeyRepository masterKeyRepository;
    private static final Logger logger = LoggerFactory.getLogger(MasterKeyRetry.class);

    public MasterKeyRetry(MasterKeyRepository masterKeyRepository) {
        this.masterKeyRepository = masterKeyRepository;
    }

    @Retry(name = "saveVersionMasterKeyRetry", fallbackMethod = "handleVersionMasterKeySaveFailure")
    public Long saveVersionMasterKeyRetry() {
        logger.info("Attempting to save master key version");
        return masterKeyRepository.save(new MasterKey());
    }

    public Long handleVersionMasterKeySaveFailure(Throwable ex) {
        logger.error("Version master key save failed", ex);
        throw new MasterKeyStorageException("Version master key save failed", ex);
    }

    @Retry(name = "writeBytesRetry")
    public void writeBytesRetry(Path masterKeyPath, byte[] keyBytes) {
        try {
            Files.write(masterKeyPath, keyBytes,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            logger.error("Error writing MasterKey file", e);
            throw new MasterKeyStorageException("Error creating master key file", e);
        }
    }

    @Retry(name = "removeFileSafelyRetry", fallbackMethod = "fallbackAbortCleanup")
    public void removeFileSafelyRetry(Path masterKeyPath) {
        try {
            Files.deleteIfExists(masterKeyPath);
            logger.warn("Orphaned MasterKey file removed after rollback");
        } catch (IOException e) {
            throw new MasterKeyStorageException("Failed to delete orphaned MasterKey file during rollback cleanup", e);
        }
    }

    public void fallbackAbortCleanup(Path masterKeyPath, Throwable ex) {
        logger.error("Rollback cleanup failed permanently. Orphaned MasterKey file remains on disk", ex);
        throw new MasterKeyStorageException("Rollback cleanup failed permanently", ex);
    }

    @Retry(name = "readAllBytesRetry")
    public byte[] readAllBytesRetry(Path masterKeyPath) {
        try {
           return Files.readAllBytes(masterKeyPath);
        } catch (IOException e) {
            logger.error("Failed to read MasterKey file", e);
            throw new MasterKeyStorageException("Failed to read master key file", e);
        }
    }
}
