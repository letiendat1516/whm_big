-- ============================================================
-- MODULE 1: POS (PostgreSQL)
-- ============================================================

CREATE TABLE IF NOT EXISTS Cashier (
    "cashierId"   TEXT PRIMARY KEY,
    "employeeId"  TEXT NOT NULL UNIQUE REFERENCES Employee("employeeId") ON DELETE CASCADE,
    "storeId"     TEXT REFERENCES Store("storeId") ON DELETE SET NULL,
    is_active     INTEGER NOT NULL DEFAULT 1,
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS "Order" (
    "orderId"        TEXT PRIMARY KEY,
    "cashierId"      TEXT NOT NULL REFERENCES Cashier("cashierId") ON DELETE RESTRICT,
    "shiftId"        TEXT,
    "storeId"        TEXT REFERENCES Store("storeId") ON DELETE RESTRICT,
    "customerId"     TEXT,
    "orderDate"      TIMESTAMP NOT NULL DEFAULT NOW(),
    status           TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','CONFIRMED','COMPLETED','CANCELLED','REFUNDED')),
    "totalAmount"    NUMERIC NOT NULL DEFAULT 0,
    "discountAmount" NUMERIC NOT NULL DEFAULT 0,
    "finalAmount"    NUMERIC NOT NULL DEFAULT 0,
    "taxRate"        NUMERIC NOT NULL DEFAULT 0.1,
    "taxAmount"      NUMERIC NOT NULL DEFAULT 0,
    "paymentStatus"  TEXT NOT NULL DEFAULT 'UNPAID' CHECK("paymentStatus" IN ('UNPAID','PARTIAL','PAID')),
    "paidAmount"     NUMERIC NOT NULL DEFAULT 0,
    "debtAmount"     NUMERIC NOT NULL DEFAULT 0,
    note             TEXT,
    last_modified    TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status      TEXT NOT NULL DEFAULT 'PENDING',
    version          INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_order_cashier  ON "Order"("cashierId");
CREATE INDEX IF NOT EXISTS idx_order_store    ON "Order"("storeId");
CREATE INDEX IF NOT EXISTS idx_order_date     ON "Order"("orderDate");
CREATE INDEX IF NOT EXISTS idx_order_status   ON "Order"(status);

CREATE TABLE IF NOT EXISTS OrderItem (
    "orderItemId" TEXT PRIMARY KEY,
    "orderId"     TEXT NOT NULL REFERENCES "Order"("orderId") ON DELETE CASCADE,
    "productId"   TEXT NOT NULL REFERENCES Product("ProductID") ON DELETE RESTRICT,
    "variantId"   TEXT REFERENCES ProductVariant(variant_id) ON DELETE RESTRICT,
    quantity      INTEGER NOT NULL DEFAULT 1 CHECK(quantity > 0),
    "unitPrice"   NUMERIC NOT NULL CHECK("unitPrice" >= 0),
    subtotal      NUMERIC NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_orderitem_order ON OrderItem("orderId");

CREATE TABLE IF NOT EXISTS Payment (
    "paymentId"   TEXT PRIMARY KEY,
    "orderId"     TEXT NOT NULL REFERENCES "Order"("orderId") ON DELETE CASCADE,
    "paymentDate" TIMESTAMP NOT NULL DEFAULT NOW(),
    method        TEXT NOT NULL DEFAULT 'CASH' CHECK(method IN ('CASH','QR','CARD','POINTS','MIXED')),
    "amountPaid"  NUMERIC NOT NULL DEFAULT 0,
    status        TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','COMPLETED','FAILED','REFUNDED')),
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_payment_order ON Payment("orderId");

CREATE TABLE IF NOT EXISTS CashPayment (
    "paymentId"    TEXT PRIMARY KEY REFERENCES Payment("paymentId") ON DELETE CASCADE,
    "cashReceived" NUMERIC NOT NULL DEFAULT 0,
    "changeAmount" NUMERIC NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS QRPayment (
    "paymentId"      TEXT PRIMARY KEY REFERENCES Payment("paymentId") ON DELETE CASCADE,
    "qrCode"         TEXT,
    "transactionRef" TEXT UNIQUE
);

CREATE TABLE IF NOT EXISTS Receipt (
    "receiptId"   TEXT PRIMARY KEY,
    "paymentId"   TEXT NOT NULL REFERENCES Payment("paymentId") ON DELETE CASCADE,
    "printDate"   TIMESTAMP NOT NULL DEFAULT NOW(),
    format        TEXT NOT NULL DEFAULT 'THERMAL',
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS ReturnOrder (
    "returnId"           TEXT PRIMARY KEY,
    "orderId"            TEXT NOT NULL REFERENCES "Order"("orderId") ON DELETE RESTRICT,
    "managerId"          TEXT,
    "returnDate"         TIMESTAMP NOT NULL DEFAULT NOW(),
    status               TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','APPROVED','COMPLETED','REJECTED')),
    quantity             INTEGER NOT NULL DEFAULT 1,
    reason               TEXT,
    "refundAmount"       NUMERIC NOT NULL DEFAULT 0,
    "restockWarehouseId" TEXT,
    last_modified        TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status          TEXT NOT NULL DEFAULT 'PENDING',
    version              INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS ReturnOrderItem (
    "returnItemId" TEXT PRIMARY KEY,
    "returnId"     TEXT NOT NULL REFERENCES ReturnOrder("returnId") ON DELETE CASCADE,
    "orderItemId"  TEXT NOT NULL,
    "productId"    TEXT NOT NULL,
    "variantId"    TEXT,
    quantity       INTEGER NOT NULL DEFAULT 1,
    "unitPrice"    NUMERIC NOT NULL DEFAULT 0,
    subtotal       NUMERIC NOT NULL DEFAULT 0,
    reason         TEXT,
    last_modified  TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status    TEXT NOT NULL DEFAULT 'PENDING',
    version        INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS SalesOutbound (
    "outboundId"  TEXT PRIMARY KEY,
    "orderId"     TEXT NOT NULL REFERENCES "Order"("orderId") ON DELETE CASCADE,
    "warehouseId" TEXT NOT NULL,
    status        TEXT NOT NULL DEFAULT 'PENDING',
    "createdBy"   TEXT,
    "createdAt"   TIMESTAMP NOT NULL DEFAULT NOW(),
    "postedBy"    TEXT,
    "postedAt"    TIMESTAMP,
    note          TEXT,
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS SalesOutboundItem (
    "outboundItemId" TEXT PRIMARY KEY,
    "outboundId"     TEXT NOT NULL REFERENCES SalesOutbound("outboundId") ON DELETE CASCADE,
    "productId"      TEXT NOT NULL,
    "variantId"      TEXT,
    quantity         INTEGER NOT NULL DEFAULT 1,
    last_modified    TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status      TEXT NOT NULL DEFAULT 'PENDING',
    version          INTEGER NOT NULL DEFAULT 1
);

