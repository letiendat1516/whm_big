-- ============================================================
-- SQLite MIGRATIONS
-- Adds missing sync columns to existing tables.
-- SQLite silently ignores duplicate column errors (handled by DatabaseManager).
-- ============================================================

-- OrderItem: add sync columns
ALTER TABLE OrderItem ADD COLUMN last_modified TEXT NOT NULL DEFAULT (datetime('now'));
ALTER TABLE OrderItem ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING';
ALTER TABLE OrderItem ADD COLUMN version INTEGER NOT NULL DEFAULT 1;

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

