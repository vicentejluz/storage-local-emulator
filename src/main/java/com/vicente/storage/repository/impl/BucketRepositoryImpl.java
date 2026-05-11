package com.vicente.storage.repository.impl;

import com.vicente.storage.domain.Bucket;
import com.vicente.storage.exception.BucketAlreadyExistsException;
import com.vicente.storage.exception.BucketPersistenceException;
import com.vicente.storage.repository.BucketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class BucketRepositoryImpl implements BucketRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final String TABLE_NAME = "tb_bucket";
    private static final Logger logger = LoggerFactory.getLogger(BucketRepositoryImpl.class);

    public BucketRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveIfNotExists(Bucket data) {
        logger.debug("Inserting Bucket | name={} | accessKeyId={}", data.getName(), data.getAccessKeyId());
        int rows = jdbcTemplate.update(
                "INSERT INTO " + TABLE_NAME + " (name, access_key_id) VALUES (?, ?) ON CONFLICT(name) DO NOTHING",
                data.getName(), data.getAccessKeyId());

        if (rows > 0) {
            logger.debug("Bucket inserted successfully | name={} | accessKeyId={}", data.getName(), data.getAccessKeyId());
        } else {
            logger.debug("Bucket insert skipped (no rows affected) | name={} | accessKeyId={}", data.getName(), data.getAccessKeyId());
        }
    }

    @Override
    public void save(Bucket data) {
        logger.debug("Inserting Bucket | bucketName={} | accessKeyId={}", data.getName(), data.getAccessKeyId());
        try{
            int rows = jdbcTemplate.update(
                    "INSERT INTO " + TABLE_NAME + " (name, access_key_id) VALUES (?, ?)",
                    data.getName(), data.getAccessKeyId());

            if (rows <= 0) {
                logger.error("Failed to insert bucket. name={}", data.getName());
                throw new BucketPersistenceException("Failed to insert bucket");
            }

            logger.debug("Bucket inserted successfully | bucketName={} | accessKeyId={}", data.getName(), data.getAccessKeyId());
        }catch(DataAccessException e){
            if (isUniqueConstraintViolation(e)) {
                logger.warn("Bucket already exists: {}", data.getName());
                throw new BucketAlreadyExistsException("Bucket already exists: " + data.getName());
            }

            logger.error("Unexpected database error while inserting bucket | bucketName={} | accessKeyId={}",
                    data.getName(), data.getAccessKeyId(), e);
            throw new BucketPersistenceException("Error saving bucket", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String bucketName) {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM " + TABLE_NAME + " WHERE name=?)",
                Integer.class, bucketName
        );

        boolean exists = result != null && result > 0;

        logger.debug("Checking existence. bucketName={}, exists={}", bucketName, exists);

        return exists;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findIdByNameAndAccessKeyId(String bucketName, long accessKeyId){
        logger.debug("Searching bucket id by name and accessKeyId. bucketName={}, accessKeyId={}", bucketName, accessKeyId);
        try {
            Long id = jdbcTemplate.queryForObject("SELECT id FROM " + TABLE_NAME + " WHERE name=? AND access_key_id=? LIMIT 1",
                    Long.class, bucketName, accessKeyId);

            logger.debug("Bucket found. bucketName={}, accessKeyId={}, bucketId={}", bucketName, accessKeyId, id);

            return Optional.ofNullable(id);
        }catch (EmptyResultDataAccessException e) {
            logger.debug("Bucket not found. bucketName={}, accessKeyId={}", bucketName, accessKeyId);
            return Optional.empty();
        }
    }

    /**
     * Verifica se uma exceção está relacionada a violação de constraint UNIQUE no SQLite.
     * <p>
     * Por que isso é necessário?
     * <p>
     * - O Spring (JdbcTemplate) encapsula exceções do banco em DataAccessException.
     * <p>
     * - Nem sempre recebemos diretamente a exceção do driver (SQLiteException).
     * <p>
     * - Precisamos "desempacotar" (percorrer a cadeia de causas) para encontrar a causa real.
     * <p>
     * O que esse método faz:
     * <p>
     * - Percorre toda a cadeia de causas (Throwable.getCause())
     * <p>
     * - Procura por uma exceção específica do SQLite
     * <p>
     * - Valida se o erro corresponde a UNIQUE constraint (duplicidade)
     *
     * @param cause original exception (typically a DataAccessException)
     * @return true if it is a UNIQUE constraint violation, false otherwise
     */
    private boolean isUniqueConstraintViolation(Throwable cause) {
        // Percorre a cadeia de exceções (exception wrapping)
        // Exemplo real:
        // DataAccessException -> UncategorizedSQLException -> SQLiteException
        while (cause != null) {
            // Verifica se a exceção atual é do tipo específico do driver SQLite
            // (só conseguimos acessar getResultCode() se for essa classe)
            if (cause instanceof org.sqlite.SQLiteException sqliteEx) {
                // getResultCode() retorna um enum com o código de erro interno do SQLite
                // SQLITE_CONSTRAINT_UNIQUE indica violação de chave única (duplicidade)
                return sqliteEx.getResultCode() == org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE;
            }
            // Vai para a próxima causa (desce na cadeia de exceções)
            cause = cause.getCause();
        }

        // Se percorreu tudo e não encontrou SQLiteException,
        // então não é erro de UNIQUE constraint
        return false;
    }
}
