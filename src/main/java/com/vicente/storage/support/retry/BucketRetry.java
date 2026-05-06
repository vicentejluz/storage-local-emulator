package com.vicente.storage.support.retry;

import com.vicente.storage.domain.Bucket;
import com.vicente.storage.exception.BucketAlreadyExistsException;
import com.vicente.storage.exception.BucketCreationException;
import com.vicente.storage.exception.BucketPersistenceException;
import com.vicente.storage.repository.BucketRepository;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BucketRetry {
    private final BucketRepository bucketRepository;
    private static final Logger logger = LoggerFactory.getLogger(BucketRetry.class);

    public BucketRetry(BucketRepository bucketRepository) {
        this.bucketRepository = bucketRepository;
    }

    @Retry(name = "bucketInitRetry", fallbackMethod = "handleBucketSaveFailure")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void bucketInitRetry(Bucket bucket) {
        logger.info("Attempting to save bucket | name={}", bucket.getName());
        bucketRepository.saveIfNotExists(bucket);
    }

    @Retry(name = "bucketSaveRetry", fallbackMethod = "handleBucketSaveFailure")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void bucketSaveRetry(Bucket bucket) {
        logger.info("Attempting to save bucket (strict) | name={}", bucket.getName());
        bucketRepository.save(bucket);

    }

    public void handleBucketSaveFailure(Bucket bucket, Throwable ex) {
        if (ex instanceof BucketAlreadyExistsException bucketAlreadyExistsException) {
            throw bucketAlreadyExistsException;
        }

        if (ex instanceof BucketPersistenceException bucketPersistenceException) {
            throw bucketPersistenceException;
        }

        logger.error("Bucket save failed | name={}", bucket.getName(), ex);
        throw new BucketCreationException("bucket save failed", ex);
    }
}
