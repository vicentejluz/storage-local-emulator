package com.vicente.storage.mapper;

import com.vicente.storage.domain.StoredFileMetadata;
import com.vicente.storage.mapper.util.JdbcDateTimeMapper;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;


public class StoredFileMetadataRowMapper implements RowMapper<StoredFileMetadata> {

    @Override
    public StoredFileMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {

        Timestamp tsCreatedAt = rs.getTimestamp("created_at");
        Timestamp tsUpdatedAt = rs.getTimestamp("updated_at");

        return new StoredFileMetadata(
                rs.getObject("id", Long.class),
                rs.getString("object_key"),
                rs.getString("virtual_file_name"),
                rs.getString("physical_file_name"),
                rs.getString("extension"),
                rs.getString("content_type"),
                rs.getString("content_disposition"),
                rs.getObject("content_length", Long.class),
                rs.getString("virtual_path"),
                rs.getObject("bucket_id", Long.class),
                rs.getString("etag"),
                JdbcDateTimeMapper.toLocalDateTime(tsCreatedAt),
                JdbcDateTimeMapper.toLocalDateTime(tsUpdatedAt)
        );
    }
}
