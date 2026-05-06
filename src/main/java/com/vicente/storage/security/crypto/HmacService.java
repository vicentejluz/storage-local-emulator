package com.vicente.storage.security.crypto;

import com.vicente.storage.exception.HmacException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class HmacService {

    public String sign(String canonicalRequest, byte[] secretKey, Encoding encoding) {
        SecretKey key = new SecretKeySpec(secretKey, CryptoConstants.HMAC_SHA256);

        try {
            // Chama o método auxiliar que faz o cálculo do HMAC real
            byte[] rawSignature = hmac(key, canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return encoding.encode(rawSignature);
        } catch (NoSuchAlgorithmException e) {
            throw new HmacException("HMAC algorithm not found", e);
        } catch (InvalidKeyException e) {
            throw new HmacException("Invalid HMAC key", e);
        }
    }

    public boolean matches(String expectedSignature, String providedSignature, Encoding encoding) {
        if (expectedSignature == null || providedSignature == null) {
            return false;
        }

        try {
            byte[] digestA = encoding.decode(expectedSignature);
            byte[] digestB = encoding.decode(providedSignature);

            return MessageDigest.isEqual(digestA, digestB);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] hmac(Key key, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(CryptoConstants.HMAC_SHA256);

        // Inicializa o Mac com a chave secreta
        mac.init(key);

        // Calcula o HMAC dos dados e retorna como array de bytes
        return mac.doFinal(data);
    }
}
