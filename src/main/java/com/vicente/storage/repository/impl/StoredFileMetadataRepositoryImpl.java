package com.vicente.storage.repository.impl;

import com.vicente.storage.exception.MetadataPersistenceException;
import com.vicente.storage.repository.StoredFileMetadataRepository;
import com.vicente.storage.domain.StoredFileMetadata;
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
public class StoredFileMetadataRepositoryImpl implements StoredFileMetadataRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final String TABLE_NAME = "tb_stored_file_metadata";
    private final RowMapper<StoredFileMetadata> rowMapper = new StoredFileMetadataRowMapper();
    private static final Logger logger = LoggerFactory.getLogger(StoredFileMetadataRepositoryImpl.class);

    public StoredFileMetadataRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(StoredFileMetadata data) {
        int rows = jdbcTemplate.update(
                "INSERT INTO " + TABLE_NAME + " (" +
                        "object_key, virtual_file_name, physical_file_name, extension, content_type, content_disposition," +
                        " content_length, virtual_path, bucket_id, etag) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT(bucket_id, object_key) " +
                        "DO UPDATE SET " +
                        "virtual_file_name = excluded.virtual_file_name, " +
                        "physical_file_name = excluded.physical_file_name, " +
                        "extension = excluded.extension, " +
                        "etag = excluded.etag, " +
                        "content_type = excluded.content_type," +
                        "content_length = excluded.content_length, " +
                        "virtual_path = excluded.virtual_path, " +
                        "content_disposition = excluded.content_disposition, " +
                        "updated_at = CURRENT_TIMESTAMP",
                data.getObjectKey(),
                data.getVirtualFileName(),
                data.getPhysicalFileName(),
                data.getExtension(),
                data.getContentType(),
                data.getContentDisposition(),
                data.getContentLength(),
                data.getVirtualPath(),
                data.getBucketId(),
                data.getEtag()
        );

        if (rows <= 0) {
            logger.error("Failed to insert stored file metadata. objectKey={}", data.getObjectKey());
            throw new MetadataPersistenceException("Failed to insert stored file metadata");
        }

        logger.debug("Stored file metadata upserted. objectKey={}", data.getObjectKey());
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
    public Optional<StoredFileMetadata> findByBucketIdAndObjectKey(long bucketId, String objectKey) {
        logger.debug("Fetching stored file metadata. objectKey={}", objectKey);
        try {
            StoredFileMetadata storedFileMetadata = jdbcTemplate.queryForObject(
                    "SELECT * FROM " + TABLE_NAME + " WHERE bucket_id=? AND object_key=? LIMIT 1",
                    rowMapper, bucketId, objectKey);

            return Optional.ofNullable(storedFileMetadata);
        }catch (EmptyResultDataAccessException e) {
            logger.debug("No stored file metadata found for bucketId={}, objectKey={}", bucketId, objectKey);
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
    public Boolean existsPhysicalFileName(String physicalFileName) {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM " + TABLE_NAME + " WHERE physical_file_name=?)",
                Integer.class, physicalFileName
        );

        boolean exists = result != null && result > 0;

        logger.debug("Checking existence. physicalFileName={}, exists={}", physicalFileName, exists);

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
