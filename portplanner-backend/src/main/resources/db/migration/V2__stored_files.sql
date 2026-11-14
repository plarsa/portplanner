-- Metadata for every uploaded file. The bytes live in the configured FileStore,
-- which owns storage_key; this table is the only place that maps a row to them.
CREATE TABLE stored_files (
    id                BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store             VARCHAR(20)  NOT NULL,
    storage_key       VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255),
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    uploaded_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);
