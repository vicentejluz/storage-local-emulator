package com.vicente.storage.service.impl;

import com.vicente.storage.domain.MasterKey;
import com.vicente.storage.domain.enums.MasterKeyStatus;
import com.vicente.storage.exception.MasterKeyStorageException;
import com.vicente.storage.repository.MasterKeyRepository;
import com.vicente.storage.security.crypto.CryptoConstants;
import com.vicente.storage.security.key.MasterKeyHolder;
import com.vicente.storage.security.key.MasterKeyLoader;
import com.vicente.storage.service.MasterKeyService;
import com.vicente.storage.support.retry.MasterKeyRetry;
import com.vicente.storage.util.StoragePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.crypto.KeyGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class MasterKeyServiceImpl implements MasterKeyService {
    private final MasterKeyRepository masterKeyRepository;
    private final MasterKeyRetry masterKeyRetry;
    private final StoragePathResolver storagePathResolver;
    private final MasterKeyHolder holder;
    private final MasterKeyLoader loader;
    private final ReentrantLock lock = new ReentrantLock();
    private final int rotationExpiration;
    private static final Logger logger = LoggerFactory.getLogger(MasterKeyServiceImpl.class);

    public MasterKeyServiceImpl(MasterKeyRepository masterKeyRepository, MasterKeyRetry masterKeyRetry,
                                StoragePathResolver storagePathResolver, MasterKeyHolder holder, MasterKeyLoader loader,
                                @Value("${storage.emulator.root.master-key.rotation-months:2}") int rotationExpiration) {
        this.masterKeyRepository = masterKeyRepository;
        this.masterKeyRetry = masterKeyRetry;
        this.storagePathResolver = storagePathResolver;
        this.holder = holder;
        this.loader = loader;
        this.rotationExpiration = rotationExpiration;
    }

    @Override
    @Transactional
    public Long ensureActiveMasterKey() {
        lock.lock();

        try {
            logger.info("Starting MasterKey creation/rotation process");

            return ensureInternal();
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void loadMasterKeyIntoMemory(Long version) {
        holder.update(loader.load(version), version);
    }

    private Long ensureInternal() {
        Optional<MasterKey> optionalMasterKey = masterKeyRepository.findByStatusActive();

        if (optionalMasterKey.isEmpty()) {
            logger.debug("No ACTIVE MasterKey found. Creating new one.");
            return createAndStoreMasterKey();
        }

        MasterKey masterKey = optionalMasterKey.get();
        LocalDateTime expirationMasterKey = LocalDateTime.now().minusMonths(rotationExpiration);
        if (masterKey.getCreatedAt().isBefore(expirationMasterKey)) {
            logger.debug("MasterKey expired. Rotating key | current_version={}", masterKey.getVersion());
            masterKeyRepository.transitionStatus(
                    masterKey.getVersion(),
                    masterKey.getStatus(),
                    MasterKeyStatus.ROTATING
            );
            return rotateAndStoreMasterKey(masterKey.getVersion());
        }

        logger.debug("MasterKey is still valid | version={}", masterKey.getVersion());

        return masterKey.getVersion();
    }

    private static byte[] generateMasterKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(CryptoConstants.AES);
            keyGenerator.init(CryptoConstants.AES_KEY_LENGTH);
            return keyGenerator.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Generate master key failed definitively | algorithm={} | keyLength={} | cause={}",
                    CryptoConstants.AES, CryptoConstants.AES_KEY_LENGTH, e.getMessage(), e);
            throw new MasterKeyStorageException("Failed to generate master key: algorithm " + CryptoConstants.AES + " not available", e);
        }
    }

    private Long createAndStoreMasterKey() {
        Long version = masterKeyRetry.saveVersionMasterKeyRetry();
        storeMasterKeyFileOnDisk(version);
        return version;
    }

    private Long rotateAndStoreMasterKey(Long version) {
        try {
            return createAndStoreMasterKey();
        } catch (MasterKeyStorageException e) {
            logger.error(
                    "Failed to rotate MasterKey | rolling back transaction and keeping previous version={} | cause={}",
                    version, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return version;
        }
    }

    private void storeMasterKeyFileOnDisk(Long version) {
        Path masterKeyPath = prepareWritableMasterKeyPath(version);
        byte[] keyBytes = generateMasterKey();
        try {
            masterKeyRetry.writeBytesRetry(masterKeyPath, keyBytes);
        }finally {
            // Limpeza de segurança: sobrescreve o array da chave com zeros após o uso.
            //
            // Por que usar:
            // Chaves criptográficas são dados sensíveis. Se o byte[] permanecer na memória,
            // o segredo pode continuar no heap da JVM até o Garbage Collector executar,
            // e o momento exato dessa limpeza automática é imprevisível.
            //
            // O que isso faz:
            // Arrays.fill(keyBytes, (byte) 0) substitui cada posição do array pelo valor 0x00,
            // removendo o conteúdo original da chave dessa instância específica de byte[].
            //
            // Exemplo:
            // Antes: [34, -91, 12, 77, ...]
            // Depois: [0,   0,  0,  0, ...]
            //
            // Importante:
            // Isso limpa apenas este objeto byte[]. Não apaga cópias que possam ter sido
            // criadas em outros lugares (logs, buffers, arrays clonados, etc.).
            //
            // Benefício:
            // Reduz o tempo de permanência de dados sensíveis na memória e diminui o risco
            // de exposição em memory dumps, crash reports, inspeções de heap ou reutilização acidental.
            Arrays.fill(keyBytes, (byte) 0);
        }
        TransactionSynchronizationManager.registerSynchronization(transactionSynchronization(masterKeyPath));
        logger.debug("MasterKey file created successfully | version={}", version);
    }

    private Path prepareWritableMasterKeyPath(Long version) {
        Path masterKeyPath = storagePathResolver.masterKey(version);
        try {
            Files.createDirectories(masterKeyPath.getParent());
            return masterKeyPath;
        } catch (IOException e) {
            logger.error("Failed to create MasterKey directory", e);
            throw new MasterKeyStorageException("Could not initialize MasterKey directory", e);
        }
    }

    private TransactionSynchronization transactionSynchronization(Path masterKeyPath) {
        return new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if(status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    masterKeyRetry.removeFileSafelyRetry(masterKeyPath);
                }
            }
        };
    }
}
