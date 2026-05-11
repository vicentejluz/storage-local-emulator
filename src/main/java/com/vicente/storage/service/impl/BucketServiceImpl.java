package com.vicente.storage.service.impl;

import com.vicente.storage.domain.Bucket;
import com.vicente.storage.exception.BucketCreationException;
import com.vicente.storage.exception.BucketNotFoundException;
import com.vicente.storage.repository.BucketRepository;
import com.vicente.storage.service.BucketService;
import com.vicente.storage.support.cleanup.FileCleanupHandler;
import com.vicente.storage.support.retry.BucketRetry;
import com.vicente.storage.util.StoragePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class BucketServiceImpl implements BucketService {
    private final BucketRepository bucketRepository;
    private final BucketRetry bucketRetry;
    private final StoragePathResolver storagePathResolver;
    private final FileCleanupHandler fileCleanupHandler;
    private static final Logger logger = LoggerFactory.getLogger(BucketServiceImpl.class);

    public BucketServiceImpl(BucketRepository bucketRepository, BucketRetry bucketRetry,
                             StoragePathResolver storagePathResolver, FileCleanupHandler fileCleanupHandler) {
        this.bucketRepository = bucketRepository;
        this.bucketRetry = bucketRetry;
        this.storagePathResolver = storagePathResolver;
        this.fileCleanupHandler = fileCleanupHandler;
    }

    @Override
    @Transactional
    public void createBucketIfNotExists(String bucketName, Long accessKeyId) {
        Path bucketPath = resolveBucketStoragePath(bucketName, accessKeyId);

        try {
            boolean createdNow = ensureBucketDirectoryExists(bucketName, bucketPath);

            registerBucketRollbackSynchronization(createdNow, bucketPath);

            if(!bucketRepository.existsByName(bucketName)) {
                logger.debug("Bucket not found in database. Inserting | bucketName={} | accessKeyId={}", bucketName, accessKeyId);
                bucketRetry.bucketInitRetry(new Bucket(bucketName, accessKeyId));
                logger.info("Bucket persisted in database | bucketName={}", bucketName);
                return;
            }

            logger.debug("Bucket already exists in database | bucketName={}", bucketName);
        } catch (IOException e) {
            logger.error("Failed to create bucket directory | bucketName={} | path={}", bucketName, bucketPath, e);
            throw new BucketCreationException("Failed to create bucket directory for bucket: " + bucketName, e);
        }
    }

    @Override
    @Transactional
    public void createBucket(String bucketName, Long accessKeyId) {
        Path bucketPath = resolveBucketStoragePath(bucketName, accessKeyId);

        try {
            boolean createdNow = ensureBucketDirectoryExists(bucketName, bucketPath);

            registerBucketRollbackSynchronization(createdNow, bucketPath);

            bucketRetry.bucketSaveRetry(new Bucket(bucketName, accessKeyId));
        } catch (IOException e) {
            logger.error("Failed to create bucket directory | bucketName={} | path={}", bucketName, bucketPath, e);
            throw new BucketCreationException("Failed to create bucket directory for bucket: " + bucketName, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long findIdByNameAndAccessKeyId(String bucketName, long accessKeyId) {
        return bucketRepository.findIdByNameAndAccessKeyId(bucketName, accessKeyId).orElseThrow(() ->
                        new BucketNotFoundException("Bucket not found or access denied"));
    }

    private void registerBucketRollbackSynchronization(boolean createdNow, Path bucketPath) {
        if (createdNow && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(transactionSynchronization(bucketPath));
        }
    }

    private Path resolveBucketStoragePath(String bucketName, Long accessKeyId) {
        logger.debug("Starting bucket creation check | bucketName={} | accessKeyId={}", bucketName, accessKeyId);
        Path bucketPath = storagePathResolver.bucketDir(bucketName);
        logger.debug("Resolved bucket path | bucketName={} | path={}", bucketName, bucketPath);
        return bucketPath;
    }

    private boolean ensureBucketDirectoryExists(String bucketName, Path bucketPath) throws IOException {
        Files.createDirectories(bucketPath.getParent());

        boolean createdNow;
        try {
            Files.createDirectory(bucketPath);
            createdNow = true;
            logger.info("Bucket directory created | bucketName={} | path={}", bucketName, bucketPath);
        }catch (FileAlreadyExistsException _){
            createdNow = false;
            logger.debug("Bucket directory already exists | bucketName={} | path={}", bucketName, bucketPath);
        }
        return createdNow;
    }

    private TransactionSynchronization transactionSynchronization(Path bucketPath) {
        return new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if(status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        logger.warn("Transaction rolled back. Cleaning bucket directory | path={}", bucketPath);
                        fileCleanupHandler.deleteFilesIfExists(bucketPath);
                }
            }
        };
    }
}
