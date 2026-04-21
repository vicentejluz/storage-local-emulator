package com.vicente.storage.repository;

import com.vicente.storage.domain.MasterKey;

import java.util.Optional;

public interface MasterKeyRepository {
    Long save(MasterKey data);

    Optional<MasterKey> findByStatusActive();

    Optional<Long> findIdByActiveVersion(Long version);

    void updateStatusForInactive(MasterKey data);
}
