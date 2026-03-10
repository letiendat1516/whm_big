-- ============================================================
-- PostgreSQL INIT SCRIPT — Retail Store Management System
-- For Railway cloud deployment
-- ============================================================

CREATE TABLE IF NOT EXISTS app_config (
    key           TEXT PRIMARY KEY,
    value         TEXT NOT NULL,
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO app_config(key, value) VALUES
('store_id',       'STORE-001'),
('store_code',     'S001'),
('db_version',     '1.0.0'),
('schema_version', '2026.03'),
('last_sync',      '1970-01-01T00:00:00'),
('sync_enabled',   'true'),
('price_list_id',  'PL-001')
ON CONFLICT (key) DO NOTHING;

-- Sync queue for offline sync
CREATE TABLE IF NOT EXISTS sync_queue (
    queue_id      TEXT PRIMARY KEY DEFAULT replace(gen_random_uuid()::text, '-', ''),
    table_name    TEXT NOT NULL,
    record_id     TEXT NOT NULL,
    operation     TEXT NOT NULL CHECK(operation IN ('INSERT','UPDATE','DELETE')),
    payload       TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    retry_count   INTEGER NOT NULL DEFAULT 0,
    status        TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','PROCESSING','DONE','FAILED')),
    error_msg     TEXT
);

CREATE INDEX IF NOT EXISTS idx_syncqueue_status  ON sync_queue(status);
CREATE INDEX IF NOT EXISTS idx_syncqueue_table   ON sync_queue(table_name);

-- Audit log
CREATE TABLE IF NOT EXISTS audit_log (
    log_id      TEXT PRIMARY KEY DEFAULT replace(gen_random_uuid()::text, '-', ''),
    table_name  TEXT NOT NULL,
    record_id   TEXT NOT NULL,
    action      TEXT NOT NULL CHECK(action IN ('INSERT','UPDATE','DELETE')),
    changed_by  TEXT,
    changed_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    old_values  TEXT,
    new_values  TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_table ON audit_log(table_name);

