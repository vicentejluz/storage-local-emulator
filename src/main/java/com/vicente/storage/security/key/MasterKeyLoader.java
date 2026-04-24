package com.vicente.storage.security.key;

import com.vicente.storage.exception.MasterKeyStorageException;
import com.vicente.storage.security.crypto.CryptoConstants;
import com.vicente.storage.support.retry.MasterKeyRetry;
import com.vicente.storage.util.StoragePathResolver;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Path;
import java.util.Arrays;

@Component
public class MasterKeyLoader {
    private final StoragePathResolver storagePathResolver;
    private final MasterKeyRetry masterKeyRetry;

    public MasterKeyLoader(StoragePathResolver storagePathResolver, MasterKeyRetry masterKeyRetry) {
        this.storagePathResolver = storagePathResolver;
        this.masterKeyRetry = masterKeyRetry;
    }

    public SecretKey load(Long version){
        validateVersion(version);

        Path masterKeyPath = storagePathResolver.masterKey(version);

        byte[] bytes = null;
        try {
            bytes = masterKeyRetry.readAllBytesRetry(masterKeyPath);

            validateKeySize(version, bytes);

            return new SecretKeySpec(bytes, CryptoConstants.AES);
        } finally {
            if (bytes != null) {
                Arrays.fill(bytes, (byte) 0);
            }
        }
    }

    private void validateVersion(Long version) {
        if (version == null || version <= 0) {
            throw new MasterKeyStorageException("Invalid master key version");
        }
    }

    private void validateKeySize(Long version, byte[] bytes) {
        int expected = CryptoConstants.AES_KEY_LENGTH / 8;
        if (bytes.length != expected) {
            throw new MasterKeyStorageException("Invalid master key size for version " + version + ": expected "
                    + expected + " bytes but found " + bytes.length);
        }

    }
}
