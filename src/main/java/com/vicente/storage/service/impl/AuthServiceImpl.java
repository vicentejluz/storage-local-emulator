package com.vicente.storage.service.impl;

import com.vicente.storage.domain.AccessKey;
import com.vicente.storage.dto.AuthRequestDTO;
import com.vicente.storage.exception.InvalidSignatureException;
import com.vicente.storage.security.crypto.Encoding;
import com.vicente.storage.security.crypto.HmacService;
import com.vicente.storage.security.validation.RequestValidator;
import com.vicente.storage.service.AccessKeyService;
import com.vicente.storage.service.AuthService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;

@Service
public class AuthServiceImpl implements AuthService {
    private final AccessKeyService accessKeyService;
    private final RequestValidator requestValidator;
    private final HmacService hmacService;

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    public AuthServiceImpl(AccessKeyService accessKeyService, RequestValidator requestValidator, HmacService hmacService) {
        this.accessKeyService = accessKeyService;
        this.requestValidator = requestValidator;
        this.hmacService = hmacService;
    }

    @Override
    @Transactional(readOnly = true)
    public Long authenticate(AuthRequestDTO authRequestDTO) {

        String accessKey = authRequestDTO.accessKey();
        String signature = authRequestDTO.signature();
        String method = authRequestDTO.method();
        String path = authRequestDTO.path();
        Instant timestamp = authRequestDTO.timestamp();
        String query = authRequestDTO.query();
        String contentType = authRequestDTO.contentType();
        long contentLength = authRequestDTO.contentLength();

        logger.info("Starting authentication | accessKey={} | method={} | path={}", accessKey, method, path);

        requestValidator.validateTimestamp(timestamp);

        AccessKey accessKeyEntity = accessKeyService.findByAccessKey(accessKey);

        logger.debug("AccessKey resolved | id={}", accessKeyEntity.getId());

        String canonical = buildCanonicalRequest(method, path, query, contentType, contentLength, timestamp);

        verifyRequestSignature(signature, canonical, accessKeyEntity);

        logger.info("Authentication successful | accessKey={}", accessKey);

        return accessKeyEntity.getId();
    }

    private void verifyRequestSignature(String signature, String canonical, AccessKey accessKeyEntity) {
        byte[] secretBytes = null;
        try {
            secretBytes = accessKeyService.getDecryptedSecret(
                    accessKeyEntity.getSecretKey(), accessKeyEntity.getMasterKeyId());

            String expectedSignature = hmacService.sign(canonical, secretBytes, Encoding.HEX);

            verifySignatureOrThrow(signature, expectedSignature);
        }finally {
            logger.debug("Cleaning sensitive secret bytes from memory");
            if (secretBytes != null) Arrays.fill(secretBytes, (byte) 0);
        }
    }

    private static @NonNull String buildCanonicalRequest(String method, String path, String query,
                                                         String contentType, long contentLength, Instant timestamp) {
        return method.toUpperCase().trim() + "\n" +
                path.trim() + "\n" +
                ((query == null || query.isBlank()) ? "" : query.trim()) + "\n" +
                ((contentType == null || contentType.isBlank()) ? "" : contentType.toLowerCase().trim()) + "\n" +
                (contentLength < 0 ? "" : contentLength) + "\n" +
                timestamp.toString().trim();
    }

    private void verifySignatureOrThrow(String signature, String expectedSignature) {
        if(!hmacService.matches(expectedSignature, signature, Encoding.HEX)) {
            logger.warn("Invalid signature detected");
            throw new InvalidSignatureException("Invalid signature");
        }
    }
}
