package com.vicente.storage.service.impl;

import com.vicente.storage.domain.AccessKey;
import com.vicente.storage.exception.InvalidAccessKeyException;
import com.vicente.storage.exception.InvalidRootSecretKeyException;
import com.vicente.storage.repository.AccessKeyRepository;
import com.vicente.storage.security.crypto.Encoding;
import com.vicente.storage.security.crypto.SecretKeyCryptoService;
import com.vicente.storage.service.AccessKeyService;
import com.vicente.storage.service.MasterKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AccessKeyServiceImpl implements AccessKeyService {
    private final AccessKeyRepository accessKeyRepository;
    private final SecretKeyCryptoService secretKeyCryptoService;
    private final MasterKeyService masterKeyService;

    private final ReentrantLock lock = new ReentrantLock();
    private static final Logger logger = LoggerFactory.getLogger(AccessKeyServiceImpl.class);

    public AccessKeyServiceImpl(AccessKeyRepository accessKeyRepository, SecretKeyCryptoService secretKeyCryptoService,
                                MasterKeyService masterKeyService) {
        this.accessKeyRepository = accessKeyRepository;
        this.secretKeyCryptoService = secretKeyCryptoService;
        this.masterKeyService = masterKeyService;
    }

    @Override
    @Transactional
    public Long initializeRootAccessKey(String accessKey, String secretKey, Long versionMasterKey) {
        lock.lock();
        try {
            logger.info("Starting root AccessKey initialization process");

            String normalizedSecret = secretKey.trim();

            validateRootSecretKey(normalizedSecret);

            long masterKeyId = masterKeyService.getActiveMasterKeyIdByVersion(versionMasterKey);

            return getOrCreateRootAccessKey(accessKey, normalizedSecret, masterKeyId);
        }finally {
            lock.unlock();
            logger.debug("Root AccessKey initialization lock released");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AccessKey findByAccessKey(String accessKey) {
        return accessKeyRepository.findByAccessKey(accessKey).orElseThrow(() -> {
            logger.warn("Invalid AccessKey attempt");
            return new InvalidAccessKeyException("Invalid access key");
        });
    }

    @Override
    public byte[] getDecryptedSecret(String secretKey, Long versionMasterKey) {
        SecretKey masterKey = masterKeyService.getUsableKey(versionMasterKey);

        logger.debug("Decrypting secret using masterKeyId={}", versionMasterKey);

        return secretKeyCryptoService.decrypt(secretKey, masterKey);
    }

    private static void validateRootSecretKey(String secretKey) {
        byte[] decoded;
        try {
            decoded = Encoding.BASE64.decode(secretKey);
        }catch (IllegalArgumentException e){
            throw new InvalidRootSecretKeyException("Root secret key must be a valid Base64 string");
        }

        try {
            if (decoded.length < 32) {
                throw new InvalidRootSecretKeyException("Secret key must decode to at least 32 bytes");
            }
        }finally {
            if(decoded != null) Arrays.fill(decoded, (byte)0);
        }
    }

    private Long getOrCreateRootAccessKey(String accessKey, String secretKey, long masterKeyId) {
        Optional<Long> optionalAccessKeyId = accessKeyRepository.findIdByAccessKey(accessKey);

        Long accessKeyId;

        if(optionalAccessKeyId.isPresent()) {
            accessKeyId = optionalAccessKeyId.get();
            logger.debug("Root AccessKey already exists | accessKey={} | id={}", accessKey, accessKeyId);
            return accessKeyId;
        }

        logger.info("Creating root AccessKey={}", accessKey);
        String encrypted = secretKeyCryptoService.encrypt(secretKey.getBytes(StandardCharsets.UTF_8));
        AccessKey data = new AccessKey(accessKey, encrypted, masterKeyId);
        accessKeyId = accessKeyRepository.save(data);
        logger.info("Root AccessKey created successfully | accessKey={} | id={}", accessKey, accessKeyId);

        return accessKeyId;
    }
}
