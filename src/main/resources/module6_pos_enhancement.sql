-- ============================================================
-- MODULE 6: POS Enhancement — Tax, Partial Payment, Return Items, Outbound link
-- Load Order: after seed_data.sql
-- ============================================================

-- ── Tax & Partial Payment columns on Order ──────────────────
-- SQLite ALTER TABLE ADD COLUMN is safe to re-run (will error "duplicate" which is silently ignored)
ALTER TABLE "Order" ADD COLUMN taxRate REAL NOT NULL DEFAULT 0.1;
ALTER TABLE "Order" ADD COLUMN taxAmount REAL NOT NULL DEFAULT 0;
ALTER TABLE "Order" ADD COLUMN paymentStatus TEXT NOT NULL DEFAULT 'UNPAID' CHECK(paymentStatus IN ('UNPAID','PARTIAL','PAID'));
ALTER TABLE "Order" ADD COLUMN paidAmount REAL NOT NULL DEFAULT 0;
ALTER TABLE "Order" ADD COLUMN debtAmount REAL NOT NULL DEFAULT 0;

-- ── Tax columns on OrderItem ────────────────────────────────
ALTER TABLE OrderItem ADD COLUMN taxRate REAL NOT NULL DEFAULT 0.1;
ALTER TABLE OrderItem ADD COLUMN taxAmount REAL NOT NULL DEFAULT 0;

-- ── ReturnOrderItem (chi tiết trả hàng theo từng dòng) ─────
CREATE TABLE IF NOT EXISTS ReturnOrderItem (
    returnItemId TEXT PRIMARY KEY,
    returnId     TEXT NOT NULL REFERENCES ReturnOrder(returnId) ON DELETE CASCADE,
    orderItemId  TEXT NOT NULL REFERENCES OrderItem(orderItemId) ON DELETE RESTRICT,
    productId    TEXT NOT NULL REFERENCES Product(ProductID) ON DELETE RESTRICT,
    variantId    TEXT REFERENCES ProductVariant(variant_id),
    quantity     INTEGER NOT NULL DEFAULT 1 CHECK(quantity > 0),
    unitPrice    REAL NOT NULL DEFAULT 0,
    subtotal     REAL NOT NULL DEFAULT 0,
    reason       TEXT
);

CREATE INDEX IF NOT EXISTS idx_returnitem_return ON ReturnOrderItem(returnId);
CREATE INDEX IF NOT EXISTS idx_returnitem_orderitem ON ReturnOrderItem(orderItemId);

-- Auto-calc subtotal on ReturnOrderItem
CREATE TRIGGER IF NOT EXISTS trg_returnitem_subtotal
    AFTER INSERT ON ReturnOrderItem
    FOR EACH ROW
BEGIN
    UPDATE ReturnOrderItem SET subtotal = NEW.quantity * NEW.unitPrice
    WHERE returnItemId = NEW.returnItemId;
END;

-- ── Outbound linked to sales order ──────────────────────────
-- SalesOutbound: xuất kho cho đơn bán hàng
CREATE TABLE IF NOT EXISTS SalesOutbound (
    outboundId       TEXT PRIMARY KEY,
    orderId          TEXT NOT NULL REFERENCES "Order"(orderId) ON DELETE CASCADE,
    warehouseId      TEXT NOT NULL REFERENCES warehouse(warehouse_id) ON DELETE RESTRICT,
    status           TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','POSTED','CANCELLED')),
    createdBy        TEXT,
    createdAt        TEXT NOT NULL DEFAULT (datetime('now')),
    postedBy         TEXT,
    postedAt         TEXT,
    note             TEXT,
    last_modified    TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status      TEXT NOT NULL DEFAULT 'PENDING',
    version          INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_sales_outbound_order ON SalesOutbound(orderId);
CREATE INDEX IF NOT EXISTS idx_sales_outbound_wh ON SalesOutbound(warehouseId);

CREATE TABLE IF NOT EXISTS SalesOutboundItem (
    outboundItemId TEXT PRIMARY KEY,
    outboundId     TEXT NOT NULL REFERENCES SalesOutbound(outboundId) ON DELETE CASCADE,
    productId      TEXT NOT NULL REFERENCES Product(ProductID),
    variantId      TEXT REFERENCES ProductVariant(variant_id),
    quantity       INTEGER NOT NULL DEFAULT 1 CHECK(quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_sales_outbound_item ON SalesOutboundItem(outboundId);

-- ── Update existing completed orders to PAID status ─────────
UPDATE "Order" SET paymentStatus = 'PAID', paidAmount = finalAmount, debtAmount = 0
WHERE status = 'COMPLETED' AND paymentStatus = 'UNPAID';

-- ── Add restockWarehouseId to ReturnOrder if not exists ─────
ALTER TABLE ReturnOrder ADD COLUMN restockWarehouseId TEXT;

