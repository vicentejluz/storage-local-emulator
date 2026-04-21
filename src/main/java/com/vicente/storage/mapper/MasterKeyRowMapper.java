package com.vicente.storage.mapper;

import com.vicente.storage.domain.MasterKey;
import com.vicente.storage.domain.enums.MasterKeyStatus;
import com.vicente.storage.mapper.util.JdbcDateTimeMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class MasterKeyRowMapper implements RowMapper<MasterKey> {
    @Override
    public MasterKey mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        Timestamp tsCreatedAt = rs.getTimestamp("created_at");
        Timestamp tsUpdatedAt = rs.getTimestamp("updated_at");

        return new MasterKey(
                rs.getObject("id", Long.class),
                rs.getObject("version", Long.class),
                MasterKeyStatus.valueOf(rs.getString("status")),
                JdbcDateTimeMapper.toLocalDateTime(tsCreatedAt),
                JdbcDateTimeMapper.toLocalDateTime(tsUpdatedAt)
        );
    }
}
