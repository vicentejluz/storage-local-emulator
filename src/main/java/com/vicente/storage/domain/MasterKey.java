package com.vicente.storage.domain;

import com.vicente.storage.domain.base.AuditableEntity;
import com.vicente.storage.domain.enums.MasterKeyStatus;

import java.time.LocalDateTime;

public class MasterKey extends AuditableEntity {
    private Long version;
    private MasterKeyStatus status;


    public MasterKey() {
        this.status = MasterKeyStatus.ACTIVE;
    }

    public MasterKey(Long id, Long version, MasterKeyStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.version = version;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public MasterKeyStatus getStatus() {
        return status;
    }

    public void activate() {
        this.status = MasterKeyStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = MasterKeyStatus.INACTIVE;
    }
}
