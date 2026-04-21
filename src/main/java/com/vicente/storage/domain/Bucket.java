package com.vicente.storage.domain;

import com.vicente.storage.domain.base.AbstractEntity;

import java.time.LocalDateTime;

public class Bucket extends AbstractEntity {
    private String name;
    private final Long accessKeyId;


    public Bucket(String name, Long accessKeyId) {
        this.name = name;
        this.accessKeyId = accessKeyId;
    }

    public Bucket(Long id, String name, Long accessKeyId, LocalDateTime createdAt) {
        this(name, accessKeyId);
        this.id = id;
        this.createdAt = createdAt;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getAccessKeyId() {
        return accessKeyId;
    }
}
