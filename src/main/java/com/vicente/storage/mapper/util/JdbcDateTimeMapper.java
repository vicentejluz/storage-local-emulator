package com.vicente.storage.mapper.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public final class JdbcDateTimeMapper {
    private JdbcDateTimeMapper() {}

    public static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
