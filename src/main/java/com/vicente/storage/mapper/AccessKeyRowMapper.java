package com.vicente.storage.mapper;

import com.vicente.storage.domain.AccessKey;
import com.vicente.storage.mapper.util.JdbcDateTimeMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AccessKeyRowMapper implements RowMapper<AccessKey> {
    @Override
    public AccessKey mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        Timestamp tsCreatedAt = rs.getTimestamp("created_at");
        Timestamp tsUpdatedAt = rs.getTimestamp("updated_at");

        return new AccessKey(
                rs.getObject("id", Long.class),
                rs.getString("access_key"),
                rs.getString("secret_key"),
                rs.getObject("master_key_id", Long.class),
                JdbcDateTimeMapper.toLocalDateTime(tsCreatedAt),
                JdbcDateTimeMapper.toLocalDateTime(tsUpdatedAt)
        );
    }
}
