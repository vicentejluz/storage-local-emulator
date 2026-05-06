package com.vicente.storage.scheduler.util;

import com.vicente.storage.dto.AccessKeyDTO;
import com.vicente.storage.repository.AccessKeyRepository;
import com.vicente.storage.security.crypto.SecretKeyCryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Arrays;

@Component
public class AccessKeySchedulerHelper {
    private final AccessKeyRepository accessKeyRepository;
    private final SecretKeyCryptoService secretKeyCryptoService;
    private static final Logger logger = LoggerFactory.getLogger(AccessKeySchedulerHelper.class);

    public AccessKeySchedulerHelper(AccessKeyRepository accessKeyRepository, SecretKeyCryptoService secretKeyCryptoService) {
        this.accessKeyRepository = accessKeyRepository;
        this.secretKeyCryptoService = secretKeyCryptoService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reencryptAccessKeySecret(AccessKeyDTO dto, Long masterKeyId, SecretKey oldMasterKey) {
        logger.debug("[ACCESS KEY SCHEDULER] Re-encrypting accessKey={} from version={} to masterKeyId={}",
                dto.accessKey(), dto.masterKeyVersion(), masterKeyId);
        byte[] decrypted = null;
        try {
            decrypted = secretKeyCryptoService.decrypt(dto.secretKey(), oldMasterKey);
            String encrypted = secretKeyCryptoService.encrypt(decrypted);
            accessKeyRepository.updateAccessKey(dto.accessKey(), encrypted, masterKeyId);
        } finally {
            if (decrypted != null) Arrays.fill(decrypted, (byte) 0);
        }
    }

}
