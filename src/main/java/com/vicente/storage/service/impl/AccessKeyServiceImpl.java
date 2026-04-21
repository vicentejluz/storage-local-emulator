package com.vicente.storage.service.impl;

import com.vicente.storage.domain.AccessKey;
import com.vicente.storage.dto.AccessKeyDTO;
import com.vicente.storage.exception.ActiveMasterKeyNotFoundException;
import com.vicente.storage.exception.InvalidRootSecretKeyException;
import com.vicente.storage.exception.MasterKeyStorageException;
import com.vicente.storage.exception.SecretKeyCryptoException;
import com.vicente.storage.repository.AccessKeyRepository;
import com.vicente.storage.repository.MasterKeyRepository;
import com.vicente.storage.security.crypto.SecretKeyCryptoService;
import com.vicente.storage.service.AccessKeyService;

import com.vicente.storage.support.retry.MasterKeyRetry;
import com.vicente.storage.util.FileSuffixes;
import com.vicente.storage.util.PathValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AccessKeyServiceImpl implements AccessKeyService {
    private final AccessKeyRepository accessKeyRepository;
    private final SecretKeyCryptoService secretKeyCryptoService;
    private final MasterKeyRepository masterKeyRepository;
    private final MasterKeyRetry masterKeyRetry;
    private final Path rootPath;
    private final ConcurrentMap<Long, SecretKey> cacheOldMasterKey = new ConcurrentHashMap<>();
    public static final String ALGORITHM = "AES";
    private static final int KEY_LENGTH = 256;
    private static final Logger logger = LoggerFactory.getLogger(AccessKeyServiceImpl.class);

    public AccessKeyServiceImpl(AccessKeyRepository accessKeyRepository, SecretKeyCryptoService secretKeyCryptoService,
                                MasterKeyRepository masterKeyRepository, MasterKeyRetry masterKeyRetry,
                                @Value("${storage.emulator.root.path}") String rootPath) {
        this.accessKeyRepository = accessKeyRepository;
        this.secretKeyCryptoService = secretKeyCryptoService;
        this.masterKeyRepository = masterKeyRepository;
        this.masterKeyRetry = masterKeyRetry;
        this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
    }

    @Override
    @Transactional
    public void initializeAndRotateAccessKeys(String accessKey, String secretKey, Long versionMasterKey) {
        logger.info("Starting root AccessKey initialization process");

        String normalizedSecret = secretKey.trim();

        validateRootSecretKey(normalizedSecret);

        long masterKeyId = masterKeyRepository.findIdByActiveVersion(versionMasterKey).orElseThrow(() ->
                new ActiveMasterKeyNotFoundException("No active MasterKey found"));


        createRootIfMissing(accessKey, normalizedSecret, masterKeyId);

        rotateOldKeys(masterKeyId);
    }

    private static void validateRootSecretKey(String secretKey) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secretKey);
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

    private void createRootIfMissing(String accessKey, String secretKey, long masterKeyId) {
        if (!accessKeyRepository.existsAccessKey(accessKey)) {
            logger.info("Creating root AccessKey={}", accessKey);
            String encrypted = secretKeyCryptoService.encrypt(secretKey.getBytes(StandardCharsets.UTF_8));
            AccessKey data = new AccessKey(accessKey, encrypted, masterKeyId);
            accessKeyRepository.saveIfNotExists(data);
        }else{
            logger.debug("Root AccessKey already exists");
        }
    }

    private void rotateOldKeys(long masterKeyId) {
        int success = 0;
        int failed = 0;
        long start = System.currentTimeMillis();
        List<AccessKeyDTO> accessKeyDTOs = accessKeyRepository.findAllDifferentFromMasterKeyId(masterKeyId);
        logger.info("Rotating {} AccessKey(s) to masterKeyId={}", accessKeyDTOs.size(), masterKeyId);
        try {
            for (AccessKeyDTO dto : accessKeyDTOs) {
                logger.debug("Rotating accessKey={} from version={} to masterKeyId={}",
                        dto.accessKey(), dto.masterKeyVersion(), masterKeyId);
                try {
                    SecretKey oldMasterKey = getMasterKey(dto.masterKeyVersion());
                    byte[] decrypted = secretKeyCryptoService.decrypt(
                            dto.secretKey(), oldMasterKey);
                    String encrypted = secretKeyCryptoService.encrypt(decrypted);
                    accessKeyRepository.updateAccessKey(dto.accessKey(), encrypted, masterKeyId);
                    success++;
                } catch (SecretKeyCryptoException | DataAccessException e) {
                    logger.warn("Failed rotate accessKey={}", dto.accessKey(), e);
                    failed++;
                }
            }
        }finally {
            cacheOldMasterKey.clear();
        }

        logger.info("Rotation finished | total={} success={} failed={} duration={}ms",
                accessKeyDTOs.size(), success, failed, System.currentTimeMillis() - start);
    }

    private SecretKey getMasterKey(Long version) {
        return cacheOldMasterKey.computeIfAbsent(version, this::loadMasterKey);
    }


    private SecretKey loadMasterKey(Long version){
        Path keysPath = rootPath.resolve("keys").normalize();
        Path masterKeyPath = keysPath.resolve("master-key-" + version + FileSuffixes.KEY_SUFFIX).normalize();
        PathValidator.ensurePathsWithinRoot(rootPath, keysPath, masterKeyPath);
        byte[] bytes = null;
        try {
            bytes = masterKeyRetry.readAllBytesRetry(masterKeyPath);

            if(bytes.length != (KEY_LENGTH / 8)){
                throw new MasterKeyStorageException("Invalid master key size: expected " + (KEY_LENGTH / 8) + " bytes");
            }

            return new SecretKeySpec(bytes, ALGORITHM);
        } finally {
            if (bytes != null) {
                Arrays.fill(bytes, (byte) 0);
            }
        }
    }
}
