package com.vicente.storage.repository.impl;

import com.vicente.storage.domain.AccessKey;
import com.vicente.storage.dto.AccessKeyDTO;
import com.vicente.storage.mapper.AccessKeyRowMapper;
import com.vicente.storage.repository.AccessKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class AccessKeyRepositoryImpl implements AccessKeyRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final String TABLE_NAME = "tb_access_key";
    private final AccessKeyRowMapper accessKeyRowMapper = new AccessKeyRowMapper();
    private static final Logger logger = LoggerFactory.getLogger(AccessKeyRepositoryImpl.class);

    public AccessKeyRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long save(AccessKey data) {
        try {
            Long id = jdbcTemplate.queryForObject(
                    "INSERT INTO " + TABLE_NAME + " (access_key, secret_key, master_key_id) VALUES (?, ?, ?) " +
                            "RETURNING id",
                    Long.class,
                    data.getAccessKey(),
                    data.getSecretKey(),
                    data.getMasterKeyId()
            );

            logger.debug("AccessKey created successfully | accessKey={} | id={}", data.getAccessKey(), id);

            return id;
        }catch (DataAccessException e){
            logger.error("Failed to create AccessKey | accessKey={}", data.getAccessKey(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAccessKey(String accessKey) {
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
    public Optional<Long> findIdByAccessKey(String accessKey) {
        logger.debug("Fetching AccessKey id by accessKey={}", accessKey);
        try {
             Long id = jdbcTemplate.queryForObject("SELECT id FROM " + TABLE_NAME + " WHERE access_key=? LIMIT 1",
                     Long.class, accessKey);

            logger.debug("AccessKey found | accessKey={} | id={}", accessKey, id);

            return Optional.ofNullable(id);
        }catch (EmptyResultDataAccessException e) {
            logger.debug("No AccessKey found for accessKey={}", accessKey);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccessKey> findByAccessKey(String accessKey) {
        logger.debug("Fetching AccessKey");
        try {
            AccessKey accessKeyEntity = jdbcTemplate.queryForObject("SELECT id, access_key, secret_key, master_key_id, created_at, updated_at" +
                            " FROM " + TABLE_NAME + " WHERE access_key=? LIMIT 1",
                    accessKeyRowMapper, accessKey);

            logger.debug("AccessKey found | id={}", accessKeyEntity.getId());

            return Optional.of(accessKeyEntity);
        }catch (EmptyResultDataAccessException e) {
            logger.debug("No AccessKey found");
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessKeyDTO> findAllByMasterKeyIdNot(Long masterKeyId) {
        logger.debug("Fetching AccessKeys with master_key_id different from current active id={}", masterKeyId);
        List<AccessKeyDTO> keys = jdbcTemplate.query("SELECT ac.access_key, ac.secret_key, mk.version, mk.status " +
                "FROM " + TABLE_NAME + " AS ac INNER JOIN tb_master_key AS mk ON ac.master_key_id = mk.id " +
                "WHERE ac.master_key_id <> ? ORDER BY ac.created_at", (rs, _) ->
                new AccessKeyDTO(
                        rs.getString("access_key"),
                        rs.getString("secret_key"),
                        rs.getObject("version", Long.class),
                        rs.getString("status")
                ), masterKeyId);

        logger.debug("Found {} AccessKey(s) requiring rotation for active master_key_id={}", keys.size(), masterKeyId);

        return keys;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer countByMasterKeyVersion(Long version) {
        logger.debug("Counting AccessKey(s) by masterKeyVersion={}", version);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) " +
                "FROM " + TABLE_NAME + " AS ac INNER JOIN tb_master_key AS mk ON ac.master_key_id = mk.id " +
                "WHERE mk.version= ?", Integer.class, version);

        logger.debug("Found {} AccessKey(s) using masterKeyVersion={}", count, version);

        return count;
    }

    @Override
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
