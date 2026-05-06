package com.vicente.storage.support.retry;

import com.vicente.storage.domain.StoredFileMetadata;
import com.vicente.storage.exception.ChecksumMoveException;
import com.vicente.storage.exception.FileCleanupException;
import com.vicente.storage.exception.FileStorageException;
import com.vicente.storage.repository.StoredFileMetadataRepository;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class StorageRetryExecutor {
    private final StoredFileMetadataRepository storedFileMetadataRepository;
    private static final Logger logger = LoggerFactory.getLogger(StorageRetryExecutor.class);

    public StorageRetryExecutor(StoredFileMetadataRepository storedFileMetadataRepository) {
        this.storedFileMetadataRepository = storedFileMetadataRepository;
    }

    @Retry(name = "saveMetadataRetry", fallbackMethod = "handleMetadataSaveFailure")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveMetadataWithRetry(StoredFileMetadata metadata, Path physicalTmp, Path checksumTmp) {
        logger.info("Attempting to save metadata for objectKey: {} into bucket: {}",
                metadata.getObjectKey(), metadata.getBucketId());
        storedFileMetadataRepository.save(metadata);
    }

    public void handleMetadataSaveFailure(StoredFileMetadata metadata, Path physicalTmp, Path checksumTmp, Throwable ex) {
        logger.error("All retry attempts exhausted for saving metadata [objectKey: {}]. Reason: {}",
                metadata.getObjectKey(), ex.getMessage(), ex);
            try {
                boolean deletedMain = Files.deleteIfExists(physicalTmp);
                boolean deletedCheck = Files.deleteIfExists(checksumTmp);

                logger.info("Fallback cleanup status - Main file deleted: {}, Checksum file deleted: {}", deletedMain, deletedCheck);
            } catch (IOException e) {
                logger.warn("Critical: Failed to clean up temporary files during fallback for objectKey: {}",
                        metadata.getObjectKey(), e);
            }
            throw new FileStorageException("Database persistence failed after multiple retries for metadata: "
                    + metadata.getObjectKey(), ex);
    }

    @Retry(name = "renameRetry", fallbackMethod = "handleRenameFailure")
    public void renameRetry(Path physicalPathTemp, Path physicalPath,
                            Path checksumTmpPath, Path checksumPath){
        // 1. Move Principal (CRÍTICO)
        try {
            if (Files.exists(physicalPathTemp)) {
                logger.info("Renaming main file to final destination: {}", physicalPath);
                Files.move(physicalPathTemp, physicalPath, StandardCopyOption.ATOMIC_MOVE);
            } else {
                logger.debug("Main file already renamed or not found at: {}", physicalPathTemp);
            }
        }catch (AtomicMoveNotSupportedException e) {
            logger.warn("Atomic rename not supported, falling back to standard rename | physicalPathTemp={}", physicalPathTemp);
            try {
                Files.move(physicalPathTemp, physicalPath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Main file renamed successfully using fallback: {}", physicalPath);
            } catch (IOException ex) {
                logger.warn("Critical: Failed to rename main file to final destination: {}", physicalPath, ex);
                throw new FileStorageException("I/O error during main file rename", ex);
            }
        } catch (IOException e) {
            logger.warn("Transient failure renaming main file: {}. Retry will be triggered.", physicalPath.getFileName());
            // Lança para o Resilience4j interceptar e fazer Retry
            throw new FileStorageException("I/O error during main file rename", e);
        }

        // 2. Move Checksum (COM RETRY "SILENCIOSO")
        // Para o Checksum ter retry, ele precisa lançar erro,
        // mas o Fallback é que vai decidir não repassar esse erro.
        try {
            logger.info("Renaming checksum file to final destination: {}", checksumPath);
            Files.move(checksumTmpPath, checksumPath, StandardCopyOption.ATOMIC_MOVE);
        }catch (AtomicMoveNotSupportedException e) {
            logger.warn("Atomic rename not supported, falling back to standard rename | checksumTmpPath={}", checksumTmpPath);
            try {
                Files.move(checksumTmpPath, checksumPath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Checksum file renamed successfully using fallback: {}", checksumPath);
            } catch (IOException ex) {
                logger.warn("Critical: Failed to rename checksum file to final destination: {}", checksumPath, ex);
                throw new ChecksumMoveException("I/O error during checksum rename", ex);
            }
        } catch (IOException e) {
            logger.warn("Transient failure renaming checksum: {}. Retry will be triggered.", checksumPath.getFileName());
            // Lança para disparar o Retry.
            // No final das tentativas, o handleRenameFailure será chamado.
            throw new ChecksumMoveException("I/O error during checksum rename", e);
        }
    }

    public void handleRenameFailure(Path physicalPathTemp, Path physicalPath,
                            Path checksumTmpPath, Path checksumPath, Throwable ex) {
        if(ex instanceof FileStorageException) {
            logger.error("CRITICAL: Final failure renaming main file [{}]. Executing emergency cleanup.",
                    physicalPath.getFileName());
            try {
                Files.deleteIfExists(physicalPathTemp);
                Files.deleteIfExists(checksumTmpPath);
                logger.info("Emergency cleanup completed for renamed files: {} and {}",
                        physicalPathTemp.getFileName(), checksumTmpPath.getFileName());
            } catch (IOException e) {
                logger.error("HYPER-CRITICAL: Emergency cleanup failed for {}", physicalPath.getFileName(), e);
            }

            // esse sim causa rollback no banco
            throw new FileStorageException(
                    "File rename failed permanently. Rollback triggered for " + physicalPath.getFileName(), ex);
        }

        if(ex instanceof ChecksumMoveException) {
            // Usamos WARN porque o sistema continuará funcionando, mas sem o checksum
            logger.warn("Partial failure: Main file renamed but checksum [{}] failed after all retries. " +
                            "Cleaning up checksum temp.", checksumPath.getFileName());
            try {
                Files.deleteIfExists(checksumTmpPath);
                logger.info("Checksum temp file deleted successfully: {}", checksumTmpPath);
            } catch (IOException e) {
                logger.warn("Minor: Failed to delete orphaned checksum temp file: {}", checksumTmpPath, e);
            }
        }
    }

    @Retry(name = "removePreviousFileSafelyRetry", fallbackMethod = "fallbackAbortCleanup")
    public void removePreviousFileSafelyRetry(Path oldPhysicalPath, Path oldChecksumPath){

        logger.info("Attempting cleanup. physical={}, checksum={}",
                oldPhysicalPath, oldChecksumPath);

        boolean failed = false;

        try {
            Files.deleteIfExists(oldPhysicalPath);
            logger.info("Physical file deleted successfully: {}", oldPhysicalPath);
        } catch (IOException e) {
            logger.warn("Failed to delete physical file", e);
            failed = true;
        }

        try {
            Files.deleteIfExists(oldChecksumPath);
            logger.info("Checksum file deleted successfully: {}", oldChecksumPath);
        } catch (IOException e) {
            logger.warn("Failed to delete checksum file", e);
            failed = true;
        }

        // validação extra (recomendada)
        if (Files.exists(oldPhysicalPath) || Files.exists(oldChecksumPath)) {
            failed = true;
            logger.warn("Files still exist after delete attempt");
        }

        if (failed) {
            throw new FileCleanupException("Failed to delete old files");
        }
    }


    public void fallbackAbortCleanup(Path oldPhysicalPath, Path oldChecksumPath, Throwable ex){
        logger.warn("Delete failed, trying rename fallback. physical={}, checksum={}",
                oldPhysicalPath, oldChecksumPath, ex);

        Path physicalTmp = oldPhysicalPath.resolveSibling(oldPhysicalPath.getFileName() + ".tmp");

        try {
            logger.debug("Renaming physical file to tmp. from={} to={}", oldPhysicalPath, physicalTmp);

            Files.move(oldPhysicalPath, physicalTmp, StandardCopyOption.ATOMIC_MOVE);

            logger.info("Physical file moved to tmp successfully (atomic): {}", physicalTmp);
        } catch (AtomicMoveNotSupportedException atomicEx) {
            logger.warn("Atomic move not supported for physical file system. Using standard move instead.");
            try {
                Files.move(oldPhysicalPath, physicalTmp, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Physical file moved to tmp successfully (method=fallback/non-atomic): {}", physicalTmp);
            } catch (IOException exc) {
                logger.error("Failed fallback move for physical file. from={} to={}", oldPhysicalPath, physicalTmp, exc);
            }
        } catch (IOException e) {
            logger.error("Failed to rename physical file. from={} to={} ", oldPhysicalPath, physicalTmp, e);
        }

        Path checksumTmp = oldChecksumPath.resolveSibling(oldChecksumPath.getFileName() + ".tmp");

        try {
            logger.debug("Renaming checksum file to tmp. from={} to={}", oldChecksumPath, checksumTmp);

            Files.move(oldChecksumPath, checksumTmp, StandardCopyOption.ATOMIC_MOVE);

            logger.info("Checksum file moved to tmp successfully (atomic): {}", checksumTmp);

        } catch (AtomicMoveNotSupportedException atomicEx) {

            logger.warn("Atomic move not supported for checksum file. Falling back to non-atomic move.");

            try {
                Files.move(oldChecksumPath, checksumTmp, StandardCopyOption.REPLACE_EXISTING);

                logger.info("Checksum file moved to tmp successfully (fallback move): {}", checksumTmp);

            } catch (IOException exc) {
                logger.error("Failed fallback move for checksum file. from={} to={}", oldChecksumPath, checksumTmp, exc);
            }

        } catch (IOException e) {
            logger.error("Failed to rename checksum file. from={} to={}", oldChecksumPath, checksumTmp, e);
        }

        logger.info("Fallback cleanup finished. physicalTmp={}, checksumTmp={}", physicalTmp, checksumTmp);
    }
}
