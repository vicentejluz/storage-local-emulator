package com.vicente.storage.domain.base;


import java.time.LocalDateTime;

public abstract class AuditableEntity extends AbstractEntity {
    protected LocalDateTime updatedAt;

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
