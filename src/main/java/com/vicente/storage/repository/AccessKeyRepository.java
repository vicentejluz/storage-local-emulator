package com.vicente.storage.repository;

import com.vicente.storage.domain.AccessKey;
import com.vicente.storage.dto.AccessKeyDTO;

import java.util.List;
import java.util.Optional;

public interface AccessKeyRepository {

    Long save(AccessKey data);

    boolean existsByAccessKey(String accessKey);

    Optional<Long> findIdByAccessKey(String accessKey);

    Optional<AccessKey> findByAccessKey(String accessKey);

    List<AccessKeyDTO> findAllByMasterKeyIdNot(Long masterKeyId);

    Integer countByMasterKeyVersion(Long version);

    void updateAccessKey(String accessKey, String secretKey, Long masterKeyId);
}
