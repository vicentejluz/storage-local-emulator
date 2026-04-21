package com.vicente.storage.domain.base;

import java.time.LocalDateTime;

public abstract class AbstractEntity {
    protected Long id;
    protected LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
