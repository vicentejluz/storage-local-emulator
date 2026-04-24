package com.vicente.storage.repository;

import com.vicente.storage.domain.AccessKey;
import com.vicente.storage.dto.AccessKeyDTO;

import java.util.List;

public interface AccessKeyRepository {

    void saveIfNotExists(AccessKey data);

    boolean existsAccessKey(String accessKey);

    List<AccessKeyDTO> findAllByMasterKeyIdNot(Long masterKeyId);

    void updateAccessKey(String accessKey, String secretKey, Long masterKeyId);
}
