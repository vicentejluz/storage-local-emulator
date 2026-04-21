CREATE TABLE IF NOT EXISTS tb_stored_file_metadata
(
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    object_key TEXT NOT NULL CHECK ( length(object_key) <= 512 ),
    virtual_file_name TEXT NOT NULL,
    physical_file_name TEXT NOT NULL UNIQUE,
    extension TEXT,
    content_type TEXT NOT NULL CHECK ( length(content_type) <= 255 ),
    content_disposition TEXT,
    content_length INTEGER NOT NULL,
    virtual_path TEXT NOT NULL,
    bucket_id INTEGER NOT NULL,
    etag TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stored_file_metadata_bucket_id FOREIGN KEY (bucket_id) REFERENCES tb_buckets(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_stored_file_metadata_bucket_id_object_key UNIQUE (bucket_id, object_key)
);

CREATE TABLE IF NOT EXISTS tb_buckets
(
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL UNIQUE CHECK ( length(name) <= 63 ),
    access_key_id INTEGER NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_buckets_access_key_id FOREIGN KEY (access_key_id) REFERENCES tb_access_key(id)
        ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS tb_access_key
(
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    access_key TEXT NOT NULL UNIQUE,
    secret_key TEXT NOT NULL,
    master_key_id INTEGER NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_access_key_master_key_id FOREIGN KEY (master_key_id) REFERENCES tb_master_key(id)
        ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS tb_master_key
(
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    version INTEGER NOT NULL UNIQUE DEFAULT 1,
    status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'REVOKED')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_stored_files_created_at ON tb_stored_file_metadata(created_at);

CREATE INDEX IF NOT EXISTS idx_stored_files_bucket_id ON tb_stored_file_metadata(bucket_id);

CREATE INDEX IF NOT EXISTS idx_stored_files_object_key_etag ON tb_stored_file_metadata(object_key, etag);

CREATE INDEX IF NOT EXISTS idx_buckets_access_key_id ON tb_buckets(access_key_id);

CREATE INDEX IF NOT EXISTS idx_access_key_master_key_id ON tb_access_key(master_key_id);

CREATE UNIQUE INDEX IF NOT EXISTS  uq_master_key_active_status ON tb_master_key(status) WHERE status = 'ACTIVE';