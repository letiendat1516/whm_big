-- ============================================================
-- PostgreSQL MIGRATIONS
-- Adds missing sync columns to existing tables.
-- Each ALTER TABLE uses a sub-select check so it's idempotent.
-- ============================================================

-- OrderItem: add sync columns
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orderitem' AND column_name='last_modified') THEN
        ALTER TABLE OrderItem ADD COLUMN last_modified TIMESTAMP NOT NULL DEFAULT NOW();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orderitem' AND column_name='sync_status') THEN
        ALTER TABLE OrderItem ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orderitem' AND column_name='version') THEN
        ALTER TABLE OrderItem ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
    END IF;
END $$;

-- CashPayment: add sync columns
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='cashpayment' AND column_name='last_modified') THEN
        ALTER TABLE CashPayment ADD COLUMN last_modified TIMESTAMP NOT NULL DEFAULT NOW();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='cashpayment' AND column_name='sync_status') THEN
        ALTER TABLE CashPayment ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='cashpayment' AND column_name='version') THEN
        ALTER TABLE CashPayment ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
    END IF;
END $$;

-- QRPayment: add sync columns
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='qrpayment' AND column_name='last_modified') THEN
        ALTER TABLE QRPayment ADD COLUMN last_modified TIMESTAMP NOT NULL DEFAULT NOW();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='qrpayment' AND column_name='sync_status') THEN
        ALTER TABLE QRPayment ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='qrpayment' AND column_name='version') THEN
        ALTER TABLE QRPayment ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
    END IF;
END $$;

-- ReturnOrderItem: add sync columns
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='returnorderitem' AND column_name='last_modified') THEN
        ALTER TABLE ReturnOrderItem ADD COLUMN last_modified TIMESTAMP NOT NULL DEFAULT NOW();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='returnorderitem' AND column_name='sync_status') THEN
        ALTER TABLE ReturnOrderItem ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='returnorderitem' AND column_name='version') THEN
        ALTER TABLE ReturnOrderItem ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
    END IF;
END $$;

-- SalesOutboundItem: add sync columns
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='salesoutbounditem' AND column_name='last_modified') THEN
        ALTER TABLE SalesOutboundItem ADD COLUMN last_modified TIMESTAMP NOT NULL DEFAULT NOW();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='salesoutbounditem' AND column_name='sync_status') THEN
        ALTER TABLE SalesOutboundItem ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='salesoutbounditem' AND column_name='version') THEN
        ALTER TABLE SalesOutboundItem ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
    END IF;
END $$;

-- OrderItem: add itemDiscount column
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orderitem' AND column_name='itemdiscount') THEN
        ALTER TABLE OrderItem ADD COLUMN itemDiscount DECIMAL(19,2) NOT NULL DEFAULT 0;
    END IF;
END $$;

-- ============================================================
-- Store Transfer Request (Yêu cầu chuyển hàng từ kho lên cửa hàng)
-- ============================================================
CREATE TABLE IF NOT EXISTS StoreTransferRequest (
    request_id          TEXT PRIMARY KEY,
    request_no          INTEGER,
    store_id            TEXT NOT NULL,
    created_by          TEXT NOT NULL,
    status              TEXT NOT NULL DEFAULT 'DRAFT',
    priority            TEXT NOT NULL DEFAULT 'NORMAL',
    need_date           TEXT,
    note                TEXT,
    reject_reason       TEXT,
    assigned_warehouse_id TEXT,
    assigned_by         TEXT,
    assigned_at         TIMESTAMP,
    shipped_at          TIMESTAMP,
    received_at         TIMESTAMP,
    received_by         TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified       TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING',
    version             INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS StoreTransferRequestItem (
    item_id             TEXT PRIMARY KEY,
    request_id          TEXT NOT NULL REFERENCES StoreTransferRequest(request_id) ON DELETE CASCADE,
    product_id          TEXT NOT NULL,
    requested_qty       INTEGER NOT NULL DEFAULT 1,
    approved_qty        INTEGER,
    note                TEXT,
    last_modified       TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING',
    version             INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_str_store ON StoreTransferRequest(store_id);
CREATE INDEX IF NOT EXISTS idx_str_status ON StoreTransferRequest(status);
CREATE INDEX IF NOT EXISTS idx_stri_request ON StoreTransferRequestItem(request_id);

-- ============================================================
-- Warehouse Overstock Transfer (Yêu cầu điều chuyển hàng tồn kho)
-- ============================================================
CREATE TABLE IF NOT EXISTS WarehouseOverstockRequest (
    request_id          TEXT PRIMARY KEY,
    request_no          INTEGER,
    warehouse_id        TEXT NOT NULL,
    created_by          TEXT NOT NULL,
    status              TEXT NOT NULL DEFAULT 'DRAFT',
    target_store_id     TEXT,
    assigned_by         TEXT,
    assigned_at         TIMESTAMP,
    accepted_by         TEXT,
    accepted_at         TIMESTAMP,
    shipped_at          TIMESTAMP,
    received_at         TIMESTAMP,
    note                TEXT,
    reject_reason       TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified       TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING',
    version             INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS WarehouseOverstockRequestItem (
    item_id             TEXT PRIMARY KEY,
    request_id          TEXT NOT NULL REFERENCES WarehouseOverstockRequest(request_id) ON DELETE CASCADE,
    product_id          TEXT NOT NULL,
    overstock_qty       INTEGER NOT NULL DEFAULT 1,
    transfer_qty        INTEGER,
    note                TEXT,
    last_modified       TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING',
    version             INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_wor_warehouse ON WarehouseOverstockRequest(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_wor_status ON WarehouseOverstockRequest(status);
CREATE INDEX IF NOT EXISTS idx_wori_request ON WarehouseOverstockRequestItem(request_id);
