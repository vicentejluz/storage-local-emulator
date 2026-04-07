package com.vicente.storage.dao.impl;

import com.vicente.storage.dao.StoredFileMetadataDAO;
import com.vicente.storage.domain.entity.StoredFileMetadata;
import com.vicente.storage.mapper.StoredFileMetadataRowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public class StoredFileMetadataDAOImpl implements StoredFileMetadataDAO {
    private final JdbcTemplate jdbcTemplate;
    private static final String TABLE_NAME = "tb_stored_file_metadata";
    private final RowMapper<StoredFileMetadata> rowMapper = new StoredFileMetadataRowMapper();
    private static final Logger logger = LoggerFactory.getLogger(StoredFileMetadataDAOImpl.class);

    public StoredFileMetadataDAOImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void save(StoredFileMetadata data) {
        int rows = jdbcTemplate.update(
                "INSERT OR REPLACE INTO " + TABLE_NAME + " (" +
                        "object_key, file_name, physical_file_name, extension, content_type, size, path, bucket, checksum) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                data.objectKey(),
                data.fileName(),
                data.physicalFileName(),
                data.extension(),
                data.contentType(),
                data.size(),
                data.path(),
                data.bucket(),
                data.checksum()
        );

        if (rows <= 0) {
            logger.warn("Failed to insert stored file metadata. objectKey={}", data.objectKey());
        } else {
            logger.debug("Stored file metadata inserted. objectKey={}", data.objectKey());
        }
    }

    @Override
    @Transactional
    public Boolean deleteByBucketAndObjectKey(String bucket, String objectKey) {
       int rows = jdbcTemplate.update("DELETE FROM " + TABLE_NAME + " WHERE bucket=? AND object_key=?",
               bucket, objectKey);

       if (rows <= 0) {
            logger.debug("No stored file metadata found to delete. objectKey={}", objectKey);
            return false;
       } else {
            logger.debug("Stored file metadata deleted. objectKey={}", objectKey);
            return true;
       }
    }

    @Override
    @Transactional
    public Integer deleteAllByBucketAndPath(String bucket, String path) {
        int rows = jdbcTemplate.update("DELETE FROM " + TABLE_NAME + " WHERE bucket=? AND path=?",
                bucket, path);

        if (rows <= 0) {
            logger.debug("No stored file metadata found to delete for bucket={} and path={}", bucket, path);
            return 0;
        }

        logger.debug("Deleted {} stored file metadata entries for bucket={} and path={}", rows, bucket, path);

        return rows;
    }

    @Override
    @Transactional
    public Integer deleteAllByBucket(String bucket) {
        int rows = jdbcTemplate.update("DELETE FROM " + TABLE_NAME + " WHERE bucket=?", bucket);

        if (rows <= 0) {
            logger.debug("No stored file metadata found to delete. bucket={}", bucket);
            return 0;
        }

        logger.debug("Deleted {} stored file metadata entries for bucket={}", rows, bucket);

        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFileMetadata> findByBucketAndObjectKey(String bucket, String objectKey) {
        logger.debug("Fetching stored file metadata. objectKey={}", objectKey);
        try {
            StoredFileMetadata storedFileMetadata = jdbcTemplate.queryForObject(
                    "SELECT * FROM " + TABLE_NAME + " WHERE bucket=? AND object_key=? LIMIT 1",
                    rowMapper, bucket, objectKey);

            return Optional.ofNullable(storedFileMetadata);
        }catch (EmptyResultDataAccessException e) {
            logger.debug("No stored file metadata found for bucket={}, objectKey={}", bucket, objectKey);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean existsByBucketAndObjectKey(String bucket, String objectKey) {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM " + TABLE_NAME + " WHERE bucket=? AND object_key=?)",
                Integer.class, bucket, objectKey
        );

        boolean exists = result != null && result > 0;

        logger.debug("Checking existence. bucket={}, objectKey={}, exists={}", bucket, objectKey, exists);
        return exists;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer countByBucket(String bucket) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE bucket=?",
                Integer.class, bucket);

        if (count == null) {
            return 0;
        }

        logger.debug("Count of stored files for bucket={} is {}", bucket, count);

        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredFileMetadata> findAllByBucket(String bucket) {
        logger.debug("Fetching stored file metadata for bucket={}", bucket);
        return jdbcTemplate.query("SELECT * FROM  " + TABLE_NAME + " WHERE bucket=? ORDER BY created_at DESC",
                rowMapper, bucket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredFileMetadata> findAllByPath(String path) {
        logger.debug("Fetching stored file metadata by path={}", path);
        return jdbcTemplate.query("SELECT * FROM  " + TABLE_NAME + " WHERE path=? ORDER BY created_at DESC",
                rowMapper, path);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredFileMetadata> findAllByBucketAndPath(String bucket, String path) {
        logger.debug("Fetching stored file metadata for bucket={} and path={}", bucket, path);
        return jdbcTemplate.query("SELECT * FROM  " + TABLE_NAME + " WHERE bucket=? AND path=? ORDER BY created_at DESC",
                rowMapper,  bucket, path);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredFileMetadata> findAllByBucket(String bucket, int limit, int offset) {
        logger.debug("Fetching stored file metadata for bucket={} with limit={} and offset={}", bucket, limit, offset);
        return jdbcTemplate.query("SELECT * FROM  " + TABLE_NAME + " WHERE bucket=? ORDER BY created_at DESC " +
                "LIMIT ? OFFSET ?", rowMapper, bucket, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredFileMetadata> findAllByPath(String path, int limit, int offset) {
        logger.debug("Fetching stored file metadata for path={} with limit={} and offset={}", path, limit, offset);
        return jdbcTemplate.query("SELECT * FROM  " + TABLE_NAME + " WHERE path=? ORDER BY created_at DESC " +
                        "LIMIT ? OFFSET ?", rowMapper, path, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredFileMetadata> findAllByBucketAndPath(String bucket, String path, int limit, int offset) {
        logger.debug(
                "Fetching stored file metadata for bucket={} and path={} with limit={} and offset={}",
                bucket, path, limit, offset
        );
        return jdbcTemplate.query("SELECT * FROM  " + TABLE_NAME + " WHERE bucket=? AND path=? ORDER BY created_at DESC "
                + "LIMIT ? OFFSET ?", rowMapper,  bucket, path, limit, offset);
    }
}
