package com.vicente.storage.scheduler.service.impl;

import com.vicente.storage.dto.AccessKeyDTO;
import com.vicente.storage.exception.ActiveMasterKeyNotFoundException;
import com.vicente.storage.exception.SecretKeyCryptoException;
import com.vicente.storage.repository.AccessKeyRepository;
import com.vicente.storage.repository.MasterKeyRepository;
import com.vicente.storage.scheduler.service.AccessKeySchedulerService;
import com.vicente.storage.scheduler.util.AccessKeySchedulerHelper;
import com.vicente.storage.security.key.MasterKeyHolder;
import com.vicente.storage.security.key.MasterKeyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AccessKeySchedulerServiceImpl implements AccessKeySchedulerService {
    private final AccessKeyRepository accessKeyRepository;
    private final MasterKeyRepository masterKeyRepository;
    private final MasterKeyHolder holder;
    private final AccessKeySchedulerHelper accessKeySchedulerHelper;
    private final MasterKeyLoader masterKeyLoader;
    private final ConcurrentMap<Long, SecretKey> cacheOldMasterKey = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(AccessKeySchedulerServiceImpl.class);

    public AccessKeySchedulerServiceImpl(AccessKeyRepository accessKeyRepository,
                                         MasterKeyRepository masterKeyRepository, MasterKeyHolder masterKeyHolder,
                                         AccessKeySchedulerHelper accessKeySchedulerHelper, MasterKeyLoader masterKeyLoader) {
        this.accessKeyRepository = accessKeyRepository;
        this.masterKeyRepository = masterKeyRepository;
        this.holder = masterKeyHolder;
        this.accessKeySchedulerHelper = accessKeySchedulerHelper;
        this.masterKeyLoader = masterKeyLoader;
    }

    @Override
    public void rotatePendingAccessKeys() {
        if (!holder.isLoaded()) {
            logger.debug("[ACCESS KEY SCHEDULER] Skipping rotation | MasterKey not loaded yet");
            return;

        }

        logger.debug("[ACCESS KEY SCHEDULER] Starting rotation job | activeVersion={}", holder.getActiveVersion());
        Long masterKeyId = masterKeyRepository.findIdByActiveVersion(holder.getActiveVersion())
                .orElseThrow(() -> new ActiveMasterKeyNotFoundException("No active MasterKey found"));
        List<AccessKeyDTO> accessKeyDTOs = accessKeyRepository.findAllByMasterKeyIdNot(masterKeyId);

        processPendingAccessKeyRotations(accessKeyDTOs, masterKeyId);
    }

    private void processPendingAccessKeyRotations(List<AccessKeyDTO> accessKeyDTOs, Long masterKeyId) {

            if(!accessKeyDTOs.isEmpty()) {
                AtomicInteger success = new AtomicInteger();
                AtomicInteger failed = new AtomicInteger();
                logger.info("[ACCESS KEY SCHEDULER] Found {} pending AccessKey(s) for rotation | activeMasterKeyId={}",
                        accessKeyDTOs.size(), masterKeyId);
                try {
                    rotateAccessKeysIndividually(accessKeyDTOs, masterKeyId, success, failed);
                } finally {
                    cacheOldMasterKey.clear();
                    logger.debug("[ACCESS KEY SCHEDULER] Cleared old MasterKey cache");
                }
                if(success.get() > 0) {
                    logger.info("[ACCESS KEY SCHEDULER] Rotation finished | rotated={} failed={} total={} activeMasterKeyId={}",
                            success.get(), failed.get(), accessKeyDTOs.size(), masterKeyId);
                    return;
                }
                logger.warn("[ACCESS KEY SCHEDULER] Rotation finished with no successful records | failed={} total={} activeMasterKeyId={}",
                        failed.get(), accessKeyDTOs.size(), masterKeyId);
            } else{
                logger.debug("[ACCESS KEY SCHEDULER] No pending AccessKey(s) found for rotation | activeMasterKeyId={}",
                        masterKeyId);
            }
    }

    private void rotateAccessKeysIndividually(List<AccessKeyDTO> accessKeyDTOs, Long masterKeyId,
                                              AtomicInteger success, AtomicInteger failed) {
        accessKeyDTOs.forEach(dto -> {
            try {
                logger.debug("[ACCESS KEY SCHEDULER] Rotating accessKey={} from version={} to masterKeyId={}",
                        dto.accessKey(), dto.masterKeyVersion(), masterKeyId);
                SecretKey oldMasterKey = getMasterKey(dto.masterKeyVersion());
                accessKeySchedulerHelper.reencryptAccessKeySecret(dto, masterKeyId, oldMasterKey);
                success.getAndIncrement();
            } catch (SecretKeyCryptoException | DataAccessException e) {
                failed.getAndIncrement();
                logger.warn("[ACCESS KEY SCHEDULER] Failed to rotate accessKey={} from version={} to masterKeyId={} | cause={}",
                        dto.accessKey(), dto.masterKeyVersion(), masterKeyId, e.getMessage(), e);
            }
        });
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
