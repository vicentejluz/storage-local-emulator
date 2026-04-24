package com.vicente.storage.service.impl;

import com.vicente.storage.domain.AccessKey;
import com.vicente.storage.dto.AccessKeyDTO;
import com.vicente.storage.exception.ActiveMasterKeyNotFoundException;
import com.vicente.storage.exception.InvalidRootSecretKeyException;
import com.vicente.storage.exception.SecretKeyCryptoException;
import com.vicente.storage.repository.AccessKeyRepository;
import com.vicente.storage.repository.MasterKeyRepository;
import com.vicente.storage.security.crypto.SecretKeyCryptoService;
import com.vicente.storage.security.key.MasterKeyLoader;
import com.vicente.storage.service.AccessKeyService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
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
    private final MasterKeyLoader masterKeyLoader;
    private final ConcurrentMap<Long, SecretKey> cacheOldMasterKey = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(AccessKeyServiceImpl.class);

    public AccessKeyServiceImpl(AccessKeyRepository accessKeyRepository, SecretKeyCryptoService secretKeyCryptoService,
                                MasterKeyRepository masterKeyRepository, MasterKeyLoader masterKeyLoader) {
        this.accessKeyRepository = accessKeyRepository;
        this.secretKeyCryptoService = secretKeyCryptoService;
        this.masterKeyRepository = masterKeyRepository;
        this.masterKeyLoader = masterKeyLoader;
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
        List<AccessKeyDTO> accessKeyDTOs = accessKeyRepository.findAllByMasterKeyIdNot(masterKeyId);
        logger.info("Rotating {} AccessKey(s) to masterKeyId={}", accessKeyDTOs.size(), masterKeyId);
        try {
            for (AccessKeyDTO dto : accessKeyDTOs) {
                logger.debug("Rotating accessKey={} from version={} to masterKeyId={}",
                        dto.accessKey(), dto.masterKeyVersion(), masterKeyId);
                try {
                    SecretKey oldMasterKey = getMasterKey(dto.masterKeyVersion());
                    byte[] decrypted = secretKeyCryptoService.decrypt(dto.secretKey(), oldMasterKey);
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

    /*
     * Obtém uma Master Key correspondente à versão informada.
     * Funcionamento:
     * - Primeiro consulta o cache em memória (`cacheOldMasterKey`), que armazena
     *   versões de chaves já carregadas anteriormente.
     * - Se a chave da versão solicitada já estiver no cache, ela é retornada
     *   imediatamente, evitando novo acesso a disco e recriação do objeto SecretKey.
     * - Se não existir no cache, o método `computeIfAbsent(...)` executa
     *   automaticamente o carregamento chamando `loadMasterKey(version)`.
     * Sobre `computeIfAbsent`:
     * - `computeIfAbsent` é um método de `ConcurrentMap`.
     * - Ele verifica se a chave (`version`) existe no mapa.
     * - Se existir, retorna o valor atual.
     * - Se não existir, executa a função fornecida (`this::loadMasterKey`),
     *   salva o resultado no mapa e retorna esse valor.
     * Vantagens:
     * - Thread-safe: seguro para múltiplas threads acessando simultaneamente.
     * - Evita race conditions ao carregar a mesma chave ao mesmo tempo.
     * - Melhora performance reduzindo leituras repetidas do filesystem.
     * - Centraliza o lazy loading das Master Keys antigas.
     * Exemplo:
     * - Primeira chamada com versão 3:
     *      cache vazio -> loadMasterKey(3) -> salva no cache -> retorna chave.
     * - Segunda chamada com versão 3:
     *      já existe no cache -> retorna diretamente.
     * Uso no projeto:
     * - Importante durante rotação de chaves, quando Access Keys antigas
     *   precisam ser descriptografadas usando versões anteriores da Master Key.
     *
     * @param version versão da Master Key desejada
     * @return SecretKey correspondente à versão solicitada
     * @throws MasterKeyStorageException se ocorrer erro ao carregar a chave
     */
    private SecretKey getMasterKey(Long version) {
        return cacheOldMasterKey.computeIfAbsent(version, _ -> masterKeyLoader.load(version));
    }
}
