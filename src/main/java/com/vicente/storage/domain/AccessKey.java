package com.vicente.storage.domain;

import com.vicente.storage.domain.base.AuditableEntity;

import java.time.LocalDateTime;

public class AccessKey extends AuditableEntity {
    private final String accessKey;
    private String secretKey;
    private Long masterKeyId;

    public AccessKey(String accessKey, String secretKey, Long masterKeyId) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.masterKeyId = masterKeyId;
    }

    public AccessKey(Long id, String accessKey, String secretKey, Long masterKeyId,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(accessKey, secretKey, masterKeyId);
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;

    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public Long getMasterKeyId() {
        return masterKeyId;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setMasterKeyId(Long masterKeyId) {
        this.masterKeyId = masterKeyId;
    }
}
