package com.vicente.storage.mapper;

import com.vicente.storage.domain.entity.StoredFileMetadata;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class StoredFileMetadataRowMapper implements RowMapper<StoredFileMetadata> {

    @Override
    public StoredFileMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {

        Timestamp ts = rs.getTimestamp("created_at");

        return new StoredFileMetadata(
                rs.getObject("id", Long.class),
                rs.getString("object_key"),
                rs.getString("file_name"),
                rs.getString("physical_file_name"),
                rs.getString("extension"),
                rs.getString("content_type"),
                rs.getObject("size", Long.class),
                rs.getString("path"),
                rs.getString("bucket"),
                rs.getString("checksum"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
