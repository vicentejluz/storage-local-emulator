package com.vicente.storage.config;

import com.vicente.storage.service.AccessKeyService;
import com.vicente.storage.service.BucketService;
import com.vicente.storage.service.MasterKeyService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StorageInitializer implements ApplicationRunner {
    private final StorageProperties storageProperties;
    private final MasterKeyService masterKeyService;
    private final AccessKeyService accessKeyService;
    private final BucketService bucketService;

    private static final Logger logger = LoggerFactory.getLogger(StorageInitializer.class);

    public StorageInitializer(StorageProperties storageProperties, MasterKeyService masterKeyService,
                              AccessKeyService accessKeyService, BucketService bucketService) {
        this.storageProperties = storageProperties;
        this.masterKeyService = masterKeyService;
        this.accessKeyService = accessKeyService;
        this.bucketService = bucketService;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        logger.info("Starting storage initialization");

        logger.info("Ensuring active MasterKey");
        long versionMasterKey = masterKeyService.ensureActiveMasterKey();

        logger.info("Loading active MasterKey into memory");
        masterKeyService.loadMasterKeyIntoMemory(versionMasterKey);

        logger.info("Initializing root AccessKey");
        Long accessKeyId = accessKeyService.initializeRootAccessKey(
                storageProperties.accessKey(), storageProperties.secretKey(), versionMasterKey);

        if(storageProperties.initialBucket() != null) {
            logger.info("Creating initial bucket");
            bucketService.createBucketIfNotExists(storageProperties.initialBucket(), accessKeyId);
        }

        logger.info("Storage initialization completed");
    }
}
