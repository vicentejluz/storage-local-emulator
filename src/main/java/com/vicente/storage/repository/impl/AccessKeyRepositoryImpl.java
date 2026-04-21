package com.vicente.storage.repository.impl;

import com.vicente.storage.domain.AccessKey;
import com.vicente.storage.dto.AccessKeyDTO;
import com.vicente.storage.repository.AccessKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class AccessKeyRepositoryImpl implements AccessKeyRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final String TABLE_NAME = "tb_access_key";
    private static final Logger logger = LoggerFactory.getLogger(AccessKeyRepositoryImpl.class);

    public AccessKeyRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveIfNotExists(AccessKey data) {
        int rows = jdbcTemplate.update(
                "INSERT INTO " + TABLE_NAME + " (access_key, secret_key, master_key_id) VALUES (?, ?, ?) " +
                        "ON CONFLICT(access_key) DO NOTHING",
                data.getAccessKey(), data.getSecretKey(), data.getMasterKeyId());

        if (rows > 0) {
            logger.debug("AccessKey created successfully.");
        } else {
            logger.debug("AccessKey already exists. Skipping insert.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAccessKey(String accessKey) {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM " + TABLE_NAME + " WHERE access_key=?)",
                Integer.class, accessKey
        );

        boolean exists = result != null && result > 0;

        logger.debug("Checking existence. accessKey={}, exists={}", accessKey, exists);

        return exists;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessKeyDTO> findAllDifferentFromMasterKeyId(Long masterKeyId) {
        logger.debug("Fetching AccessKeys with master_key_id different from current active id={}", masterKeyId);
        List<AccessKeyDTO> keys = jdbcTemplate.query("SELECT ac.access_key, ac.secret_key, mk.version " +
                        "FROM " + TABLE_NAME + " AS ac INNER JOIN tb_master_key AS mk ON ac.master_key_id = mk.id " +
                        "WHERE ac.master_key_id <> ? ORDER BY ac.created_at", (rs, _) ->
                new AccessKeyDTO(
                        rs.getString("access_key"),
                        rs.getString("secret_key"),
                        rs.getObject("version", Long.class)
                ), masterKeyId);

        logger.debug("Found {} AccessKey(s) requiring rotation for active master_key_id={}", keys.size(), masterKeyId);

        return keys;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateAccessKey(String accessKey, String secretKey, Long masterKeyId) {
        logger.debug("Updating AccessKey encryption with new master_key_id={} | accessKey={}",
                masterKeyId, accessKey);
        int rows = jdbcTemplate.update("UPDATE " + TABLE_NAME + " SET secret_key=?, master_key_id=?, updated_at=CURRENT_TIMESTAMP " +
                "WHERE access_key=?", secretKey, masterKeyId, accessKey);

        if (rows > 0) {
            logger.debug("AccessKey updated successfully | accessKey={} | rowsAffected={}", accessKey, rows);
        } else {
            logger.warn("No AccessKey updated | accessKey={} not found", accessKey);
        }
    }
}
