package com.vicente.storage.repository;

import com.vicente.storage.domain.MasterKey;
import com.vicente.storage.domain.enums.MasterKeyStatus;

import java.util.Optional;

public interface MasterKeyRepository {
    Long save(MasterKey data);

    Optional<MasterKey> findByStatusActive();

    Optional<Long> findIdByActiveVersion(Long version);

    void transitionStatus(Long masterKeyVersion, MasterKeyStatus from, MasterKeyStatus to);
}
