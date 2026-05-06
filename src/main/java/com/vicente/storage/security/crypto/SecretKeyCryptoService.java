package com.vicente.storage.security.crypto;

import com.vicente.storage.exception.SecretKeyCryptoException;
import com.vicente.storage.security.key.MasterKeyHolder;
import org.springframework.stereotype.Component;
import javax.crypto.*;
import java.security.GeneralSecurityException;
import java.util.Arrays;

@Component
public class SecretKeyCryptoService {
    private final MasterKeyHolder masterKeyHolder;

    public SecretKeyCryptoService(MasterKeyHolder masterKeyHolder) {
        this.masterKeyHolder = masterKeyHolder;
    }

    public String encrypt(final byte[] plainTextBytes) {
        byte[] encryptedBytes = null;
        try {
            Cipher cipher = Cipher.getInstance(CryptoConstants.AES);
            cipher.init(Cipher.ENCRYPT_MODE, masterKeyHolder.getActiveMasterKey());
            encryptedBytes = cipher.doFinal(plainTextBytes);
            return Encoding.BASE64.encode(encryptedBytes);
        } catch (GeneralSecurityException e) {
            throw new SecretKeyCryptoException("Failed to encrypt data using AES master key", e);
        }finally {
            if (encryptedBytes != null) Arrays.fill(encryptedBytes, (byte) 0);
            Arrays.fill(plainTextBytes, (byte) 0);
        }
    }

    public byte[] decrypt(final String encryptedText, SecretKey masterKey) {
        byte[] decodedBytes = null;
        try {
            Cipher cipher = Cipher.getInstance(CryptoConstants.AES);
            cipher.init(Cipher.DECRYPT_MODE, masterKey);
            decodedBytes = Encoding.BASE64.decode(encryptedText);
            return cipher.doFinal(decodedBytes);
        } catch (GeneralSecurityException e) {
            throw new SecretKeyCryptoException("Failed to decrypt data using AES master key", e);
        }finally {
            if(decodedBytes != null) Arrays.fill(decodedBytes, (byte) 0);
        }
    }
}
