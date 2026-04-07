CREATE TABLE IF NOT EXISTS tb_stored_file_metadata
(
    id PRIMARY KEY AUTOINCREMENT NOT NULL,
    object_key TEXT NOT NULL CHECK ( length(object_key) <= 512 ),
    file_name TEXT NOT NULL CHECK ( length(file_name) <= 255 ),
    physical_file_name TEXT NOT NULL UNIQUE CHECK ( length(physical_file_name) <= 36 ),
    extension TEXT CHECK ( length(extension) <= 10 ),
    content_type TEXT NOT NULL CHECK ( length(content_type) <= 255 ),
    size INTEGER,
    path TEXT NOT NULL CHECK ( length(path) <= 500 ),
    bucket TEXT NOT NULL CHECK ( length(bucket) <= 63 ),
    checksum TEXT NOT NULL CHECK ( length(checksum) <= 32 ),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (bucket, object_key)
);

CREATE INDEX IF NOT EXISTS idx_stored_files_path_created_at ON tb_stored_file_metadata(path, created_at);

CREATE INDEX IF NOT EXISTS idx_stored_files_created_at ON tb_stored_file_metadata(created_at);

CREATE INDEX IF NOT EXISTS idx_stored_files_bucket_path ON tb_stored_file_metadata(bucket, path);