-- ============================================================
-- MASTER INIT SCRIPT - Retail Store Management System
-- Run Order: module3 -> module5 -> module2 -> module1 -> module4
-- ============================================================
--
-- System: Offline-First Retail Chain Management (50+ stores)
-- Architecture: Local SQLite per store + Cloud sync
-- Conflict Resolution: version + last_modified columns
-- Sync Status: PENDING | SYNCED | CONFLICT
--
-- DATABASE FILE: store_management.db
-- ============================================================
-- NOTE: PRAGMA foreign_keys is managed by Main.java (OFF during load, ON at runtime)
PRAGMA journal_mode = WAL;     -- Write-Ahead Logging for concurrency
PRAGMA synchronous = NORMAL;   -- Balance between safety and performance
PRAGMA cache_size = -64000;    -- 64MB cache
PRAGMA temp_store = MEMORY;

-- ============================================================
-- METADATA TABLE (store identity & sync config)
-- ============================================================
CREATE TABLE IF NOT EXISTS app_config (
    key           TEXT PRIMARY KEY,
    value         TEXT NOT NULL,
    updated_at    TEXT NOT NULL DEFAULT (datetime('now'))
);

INSERT OR IGNORE INTO app_config VALUES
('store_id',       'STORE-001',         datetime('now')),
('store_code',     'S001',              datetime('now')),
('db_version',     '1.0.0',             datetime('now')),
('schema_version', '2026.03',           datetime('now')),
('last_sync',      '1970-01-01T00:00:00', datetime('now')),
('sync_enabled',   'true',              datetime('now')),
('price_list_id',  'PL-001',            datetime('now'));

-- ============================================================
-- SYNC QUEUE TABLE (tracks pending changes for cloud sync)
-- ============================================================
CREATE TABLE IF NOT EXISTS sync_queue (
    queue_id      TEXT PRIMARY KEY DEFAULT (lower(hex(randomblob(16)))),
    table_name    TEXT NOT NULL,
    record_id     TEXT NOT NULL,
    operation     TEXT NOT NULL CHECK(operation IN ('INSERT','UPDATE','DELETE')),
    payload       TEXT,               -- JSON of changed fields
    created_at    TEXT NOT NULL DEFAULT (datetime('now')),
    retry_count   INTEGER NOT NULL DEFAULT 0,
    status        TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','PROCESSING','DONE','FAILED')),
    error_msg     TEXT
);

CREATE INDEX IF NOT EXISTS idx_syncqueue_status    ON sync_queue(status);
CREATE INDEX IF NOT EXISTS idx_syncqueue_table     ON sync_queue(table_name);
CREATE INDEX IF NOT EXISTS idx_syncqueue_created   ON sync_queue(created_at);

-- ============================================================
-- AUDIT LOG TABLE (local audit trail)
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_log (
    log_id      TEXT PRIMARY KEY DEFAULT (lower(hex(randomblob(16)))),
    table_name  TEXT NOT NULL,
    record_id   TEXT NOT NULL,
    action      TEXT NOT NULL CHECK(action IN ('INSERT','UPDATE','DELETE')),
    changed_by  TEXT,                  -- employeeId
    changed_at  TEXT NOT NULL DEFAULT (datetime('now')),
    old_values  TEXT,                  -- JSON
    new_values  TEXT                   -- JSON
);

CREATE INDEX IF NOT EXISTS idx_audit_table   ON audit_log(table_name);
CREATE INDEX IF NOT EXISTS idx_audit_changed ON audit_log(changed_at);
CREATE INDEX IF NOT EXISTS idx_audit_user    ON audit_log(changed_by);

-- ============================================================
-- LOAD ORDER REMINDER (for reference):
--  1. module3_product.sql     -> ProductCategory, Product, Variant, Price
--  2. module5_hr_shift.sql    -> Store, Employee, Shift, Payroll
--  3. module2_inventory.sql   -> Warehouse, Stock, Documents
--  4. module1_pos.sql         -> Cashier, Order, Payment, Receipt
--  5. module4_crm_promotion.sql -> Customer, Loyalty, Campaign, Promotion
-- ============================================================

