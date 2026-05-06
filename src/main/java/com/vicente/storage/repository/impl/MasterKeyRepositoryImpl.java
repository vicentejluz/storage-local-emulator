package com.vicente.storage.repository.impl;

import com.vicente.storage.domain.MasterKey;
import com.vicente.storage.domain.enums.MasterKeyStatus;
import com.vicente.storage.mapper.MasterKeyRowMapper;
import com.vicente.storage.repository.MasterKeyRepository;
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
public class MasterKeyRepositoryImpl implements MasterKeyRepository {
    private final JdbcTemplate jdbcTemplate;
    private final MasterKeyRowMapper masterKeyRowMapper = new MasterKeyRowMapper();
    private static final String TABLE_NAME = "tb_master_key";
    private static final Logger logger = LoggerFactory.getLogger(MasterKeyRepositoryImpl.class);

    public MasterKeyRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long save(MasterKey data) {
        try {
            Long version = jdbcTemplate.queryForObject(
                    "INSERT INTO " + TABLE_NAME + " (version, status) VALUES (" +
                            "(SELECT IFNULL(MAX(version), 0) + 1 FROM " + TABLE_NAME + "), ?) RETURNING version",
                    Long.class,
                    data.getStatus().name()
            );

            logger.debug("MasterKey inserted successfully | status={} | id={}", data.getStatus(), version);

            return version;
        }catch (DataAccessException e){
            logger.error("Error inserting MasterKey | status={}", data.getStatus(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MasterKey> findByStatusActive() {
        logger.debug("Fetching master key. status={}", MasterKeyStatus.ACTIVE.name());
        try {
            MasterKey masterKey = jdbcTemplate.queryForObject(
                    "SELECT * FROM " + TABLE_NAME + " WHERE status = 'ACTIVE' ORDER BY created_at DESC LIMIT 1",
                    masterKeyRowMapper);

            return Optional.ofNullable(masterKey);
        }catch (EmptyResultDataAccessException e) {
            logger.debug("No master key found for status={}", MasterKeyStatus.ACTIVE.name());
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findRotatingVersions() {
        logger.debug("Fetching master keys. status={}", MasterKeyStatus.ROTATING.name());

        return jdbcTemplate.queryForList("SELECT version FROM " + TABLE_NAME + " WHERE status = 'ROTATING'", Long.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findIdByActiveVersion(Long version) {
        logger.debug("Fetching id with status=ACTIVE and version={}", version);
        try {
            Long id = jdbcTemplate.queryForObject(
                    "SELECT id FROM " + TABLE_NAME + " WHERE status = 'ACTIVE' AND version = ? LIMIT 1",
                    Long.class, version);

            logger.debug("MasterKey ACTIVE found | version={}", version);

            return Optional.ofNullable(id);
        }catch (EmptyResultDataAccessException e) {
            logger.debug("No MasterKey found with status=ACTIVE and version={}", version);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findStatusById(Long id) {
        logger.debug("Fetching master key status by id={}", id);
        try {
            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM " + TABLE_NAME + " WHERE id = ? LIMIT 1",
                    String.class, id);

            logger.debug("Master key status found | id={} | status={}", id, status);

            return Optional.ofNullable(status);
        }catch (EmptyResultDataAccessException e) {
            logger.debug("No master key found for id={}", id);
            return Optional.empty();
        }
    }

    @Override
    public void transitionStatus(Long masterKeyVersion, MasterKeyStatus from, MasterKeyStatus to) {
        int rows = jdbcTemplate.update("UPDATE " + TABLE_NAME + " SET status=?, updated_at=CURRENT_TIMESTAMP " +
                "WHERE status=? AND version =?", to.name(), from.name(), masterKeyVersion);

        if (rows > 0) {
            logger.debug("MasterKey status updated successfully | version={} | newStatus={} | affected_rows={}",
                    masterKeyVersion, to, rows);
        } else {
            logger.debug("No MasterKey status updated. No matching row found | version={} | currentStatus={}",
                    masterKeyVersion, from);
        }
    }
}
