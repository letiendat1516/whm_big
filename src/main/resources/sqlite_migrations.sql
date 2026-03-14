-- ============================================================
-- SQLite MIGRATIONS
-- Adds missing sync columns to existing tables.
-- SQLite silently ignores duplicate column errors (handled by DatabaseManager).
-- ============================================================

-- OrderItem: add sync columns
ALTER TABLE OrderItem ADD COLUMN last_modified TEXT NOT NULL DEFAULT (datetime('now'));
ALTER TABLE OrderItem ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
ALTER TABLE OrderItem ADD COLUMN version INTEGER NOT NULL DEFAULT 1;

-- OrderItem: add per-item discount
ALTER TABLE OrderItem ADD COLUMN itemDiscount REAL NOT NULL DEFAULT 0;

-- CashPayment: add sync columns
ALTER TABLE CashPayment ADD COLUMN last_modified TEXT NOT NULL DEFAULT (datetime('now'));
ALTER TABLE CashPayment ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
ALTER TABLE CashPayment ADD COLUMN version INTEGER NOT NULL DEFAULT 1;

-- QRPayment: add sync columns
ALTER TABLE QRPayment ADD COLUMN last_modified TEXT NOT NULL DEFAULT (datetime('now'));
ALTER TABLE QRPayment ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
ALTER TABLE QRPayment ADD COLUMN version INTEGER NOT NULL DEFAULT 1;

-- ReturnOrderItem: add sync columns
ALTER TABLE ReturnOrderItem ADD COLUMN last_modified TEXT NOT NULL DEFAULT (datetime('now'));
ALTER TABLE ReturnOrderItem ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
ALTER TABLE ReturnOrderItem ADD COLUMN version INTEGER NOT NULL DEFAULT 1;

-- SalesOutboundItem: add sync columns
ALTER TABLE SalesOutboundItem ADD COLUMN last_modified TEXT NOT NULL DEFAULT (datetime('now'));
ALTER TABLE SalesOutboundItem ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
ALTER TABLE SalesOutboundItem ADD COLUMN version INTEGER NOT NULL DEFAULT 1;

-- ============================================================
-- Store Transfer Request (Yêu cầu chuyển hàng từ kho lên cửa hàng)
-- Flow: StoreManager tạo → WarehouseSupervisor nhận → InventoryManager xử lý
--       → Outbound (kho) → Inbound (cửa hàng)
-- ============================================================
CREATE TABLE IF NOT EXISTS StoreTransferRequest (
    request_id      TEXT PRIMARY KEY,
    request_no      INTEGER,
    store_id        TEXT NOT NULL,
    created_by      TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','SUBMITTED','APPROVED','ASSIGNED','SHIPPING','RECEIVED','REJECTED','CANCELLED')),
    priority        TEXT NOT NULL DEFAULT 'NORMAL' CHECK(priority IN ('LOW','NORMAL','HIGH','URGENT')),
    need_date       TEXT,
    note            TEXT,
    reject_reason   TEXT,
    assigned_warehouse_id TEXT,
    assigned_by     TEXT,
    assigned_at     TEXT,
    shipped_at      TEXT,
    received_at     TEXT,
    received_by     TEXT,
    created_at      TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT NOT NULL DEFAULT (datetime('now')),
    last_modified   TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status     TEXT NOT NULL DEFAULT 'PENDING',
    version         INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS StoreTransferRequestItem (
    item_id         TEXT PRIMARY KEY,
    request_id      TEXT NOT NULL REFERENCES StoreTransferRequest(request_id) ON DELETE CASCADE,
    product_id      TEXT NOT NULL,
    requested_qty   INTEGER NOT NULL DEFAULT 1,
    approved_qty    INTEGER,
    note            TEXT,
    last_modified   TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status     TEXT NOT NULL DEFAULT 'PENDING',
    version         INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_str_store ON StoreTransferRequest(store_id);
CREATE INDEX IF NOT EXISTS idx_str_status ON StoreTransferRequest(status);
CREATE INDEX IF NOT EXISTS idx_stri_request ON StoreTransferRequestItem(request_id);

-- ============================================================
-- Warehouse Overstock Transfer (Yêu cầu điều chuyển hàng tồn kho)
-- Flow: InventoryManager tạo → WarehouseSupervisor nhận → tìm cửa hàng phù hợp
--       → StoreManager accept → Outbound (kho) → Inbound (cửa hàng)
-- ============================================================
CREATE TABLE IF NOT EXISTS WarehouseOverstockRequest (
    request_id      TEXT PRIMARY KEY,
    request_no      INTEGER,
    warehouse_id    TEXT NOT NULL,
    created_by      TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','SUBMITTED','ASSIGNED','ACCEPTED','SHIPPING','RECEIVED','REJECTED','CANCELLED')),
    target_store_id TEXT,
    assigned_by     TEXT,
    assigned_at     TEXT,
    accepted_by     TEXT,
    accepted_at     TEXT,
    shipped_at      TEXT,
    received_at     TEXT,
    note            TEXT,
    reject_reason   TEXT,
    created_at      TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT NOT NULL DEFAULT (datetime('now')),
    last_modified   TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status     TEXT NOT NULL DEFAULT 'PENDING',
    version         INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS WarehouseOverstockRequestItem (
    item_id         TEXT PRIMARY KEY,
    request_id      TEXT NOT NULL REFERENCES WarehouseOverstockRequest(request_id) ON DELETE CASCADE,
    product_id      TEXT NOT NULL,
    overstock_qty   INTEGER NOT NULL DEFAULT 1,
    transfer_qty    INTEGER,
    note            TEXT,
    last_modified   TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status     TEXT NOT NULL DEFAULT 'PENDING',
    version         INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_wor_warehouse ON WarehouseOverstockRequest(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_wor_status ON WarehouseOverstockRequest(status);
CREATE INDEX IF NOT EXISTS idx_wori_request ON WarehouseOverstockRequestItem(request_id);

