-- ============================================================
-- MODULE 1: POS (Bán hàng - Point of Sale)
-- Load Order: 4th (after module3, module5, module2)
-- ============================================================
-- NOTE: PRAGMA foreign_keys is managed by Main.java (OFF during load, ON at runtime)
PRAGMA journal_mode = WAL;

-- ------------------------------------------------------------
-- Cashier (thu ngân - role table linking Employee)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Cashier (
    cashierId     TEXT PRIMARY KEY,
    employeeId    TEXT NOT NULL UNIQUE REFERENCES Employee(employeeId) ON DELETE CASCADE,
    storeId       TEXT REFERENCES Store(storeId) ON DELETE SET NULL,
    is_active     INTEGER NOT NULL DEFAULT 1,
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_cashier_employee ON Cashier(employeeId);
CREATE INDEX IF NOT EXISTS idx_cashier_store    ON Cashier(storeId);

CREATE TRIGGER IF NOT EXISTS trg_cashier_updated
    AFTER UPDATE ON Cashier
    FOR EACH ROW
BEGIN
    UPDATE Cashier SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE cashierId = OLD.cashierId;
END;

-- ------------------------------------------------------------
-- Order (đơn hàng)
-- NOTE: price is stored at time of sale (frozen) via unitPrice in OrderItem
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "Order" (
    orderId        TEXT PRIMARY KEY,
    cashierId      TEXT NOT NULL REFERENCES Cashier(cashierId) ON DELETE RESTRICT,
    shiftId        TEXT REFERENCES ShiftAssignment(shiftAssignmentId) ON DELETE SET NULL,
    storeId        TEXT REFERENCES Store(storeId) ON DELETE RESTRICT,
    customerId     TEXT,  -- logical FK → Customer(CustomerID), enforced at app level
    orderDate      TEXT NOT NULL DEFAULT (datetime('now')),
    status         TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','CONFIRMED','COMPLETED','CANCELLED','REFUNDED')),
    totalAmount    REAL NOT NULL DEFAULT 0,
    discountAmount REAL NOT NULL DEFAULT 0,
    finalAmount    REAL NOT NULL DEFAULT 0,
    taxRate        REAL NOT NULL DEFAULT 0.1,
    taxAmount      REAL NOT NULL DEFAULT 0,
    paymentStatus  TEXT NOT NULL DEFAULT 'UNPAID' CHECK(paymentStatus IN ('UNPAID','PARTIAL','PAID')),
    paidAmount     REAL NOT NULL DEFAULT 0,
    debtAmount     REAL NOT NULL DEFAULT 0,
    note           TEXT,
    last_modified  TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status    TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version        INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_order_cashier   ON "Order"(cashierId);
CREATE INDEX IF NOT EXISTS idx_order_shift     ON "Order"(shiftId);
CREATE INDEX IF NOT EXISTS idx_order_store     ON "Order"(storeId);
CREATE INDEX IF NOT EXISTS idx_order_customer  ON "Order"(customerId);
CREATE INDEX IF NOT EXISTS idx_order_date      ON "Order"(orderDate);
CREATE INDEX IF NOT EXISTS idx_order_status    ON "Order"(status);

CREATE TRIGGER IF NOT EXISTS trg_order_updated
    AFTER UPDATE ON "Order"
    FOR EACH ROW
BEGIN
    UPDATE "Order" SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE orderId = OLD.orderId;
END;

-- ------------------------------------------------------------
-- OrderItem (chi tiết đơn hàng - unitPrice frozen at sale time)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS OrderItem (
    orderItemId TEXT PRIMARY KEY,
    orderId     TEXT NOT NULL REFERENCES "Order"(orderId) ON DELETE CASCADE,
    productId   TEXT NOT NULL REFERENCES Product(ProductID) ON DELETE RESTRICT,
    variantId   TEXT REFERENCES ProductVariant(variant_id) ON DELETE RESTRICT,
    quantity    INTEGER NOT NULL DEFAULT 1 CHECK(quantity > 0),
    unitPrice   REAL NOT NULL CHECK(unitPrice >= 0),  -- price snapshot at sale time
    subtotal    REAL NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_orderitem_order   ON OrderItem(orderId);
CREATE INDEX IF NOT EXISTS idx_orderitem_product ON OrderItem(productId);

-- Auto-calculate subtotal
CREATE TRIGGER IF NOT EXISTS trg_orderitem_subtotal_insert
    AFTER INSERT ON OrderItem
    FOR EACH ROW
BEGIN
    UPDATE OrderItem SET subtotal = NEW.quantity * NEW.unitPrice
    WHERE orderItemId = NEW.orderItemId;
END;

CREATE TRIGGER IF NOT EXISTS trg_orderitem_subtotal_update
    AFTER UPDATE OF quantity, unitPrice ON OrderItem
    FOR EACH ROW
BEGIN
    UPDATE OrderItem SET subtotal = NEW.quantity * NEW.unitPrice
    WHERE orderItemId = NEW.orderItemId;
END;

-- ------------------------------------------------------------
-- Payment (thanh toán)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Payment (
    paymentId     TEXT PRIMARY KEY,
    orderId       TEXT NOT NULL REFERENCES "Order"(orderId) ON DELETE CASCADE,
    paymentDate   TEXT NOT NULL DEFAULT (datetime('now')),
    method        TEXT NOT NULL DEFAULT 'CASH' CHECK(method IN ('CASH','QR','CARD','POINTS','MIXED')),
    amountPaid    REAL NOT NULL DEFAULT 0,
    status        TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','COMPLETED','FAILED','REFUNDED')),
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_payment_order  ON Payment(orderId);
CREATE INDEX IF NOT EXISTS idx_payment_method ON Payment(method);
CREATE INDEX IF NOT EXISTS idx_payment_date   ON Payment(paymentDate);

CREATE TRIGGER IF NOT EXISTS trg_payment_updated
    AFTER UPDATE ON Payment
    FOR EACH ROW
BEGIN
    UPDATE Payment SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE paymentId = OLD.paymentId;
END;

-- ------------------------------------------------------------
-- CashPayment (thanh toán tiền mặt - subtable of Payment)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS CashPayment (
    paymentId     TEXT PRIMARY KEY REFERENCES Payment(paymentId) ON DELETE CASCADE,
    cashReceived  REAL NOT NULL DEFAULT 0,
    changeAmount  REAL NOT NULL DEFAULT 0
);

-- ------------------------------------------------------------
-- QRPayment (thanh toán QR - subtable of Payment)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS QRPayment (
    paymentId      TEXT PRIMARY KEY REFERENCES Payment(paymentId) ON DELETE CASCADE,
    qrCode         TEXT,
    transactionRef TEXT UNIQUE
);

CREATE INDEX IF NOT EXISTS idx_qrpayment_ref ON QRPayment(transactionRef);

-- ------------------------------------------------------------
-- Receipt (hóa đơn / biên lai)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Receipt (
    receiptId     TEXT PRIMARY KEY,
    paymentId     TEXT NOT NULL REFERENCES Payment(paymentId) ON DELETE CASCADE,
    printDate     TEXT NOT NULL DEFAULT (datetime('now')),
    format        TEXT NOT NULL DEFAULT 'THERMAL' CHECK(format IN ('THERMAL','A4','EMAIL','SMS')),
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_receipt_payment ON Receipt(paymentId);

CREATE TRIGGER IF NOT EXISTS trg_receipt_updated
    AFTER UPDATE ON Receipt
    FOR EACH ROW
BEGIN
    UPDATE Receipt SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE receiptId = OLD.receiptId;
END;

-- ------------------------------------------------------------
-- ReturnOrder (hoàn trả hàng)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ReturnOrder (
    returnId           TEXT PRIMARY KEY,
    orderId            TEXT NOT NULL REFERENCES "Order"(orderId) ON DELETE RESTRICT,
    managerId          TEXT REFERENCES StoreManager(managerId) ON DELETE SET NULL,
    returnDate         TEXT NOT NULL DEFAULT (datetime('now')),
    status             TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','APPROVED','COMPLETED','REJECTED')),
    quantity           INTEGER NOT NULL DEFAULT 1,
    reason             TEXT,
    refundAmount       REAL NOT NULL DEFAULT 0,
    restockWarehouseId TEXT,
    last_modified      TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status        TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version            INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_return_order   ON ReturnOrder(orderId);
CREATE INDEX IF NOT EXISTS idx_return_manager ON ReturnOrder(managerId);
CREATE INDEX IF NOT EXISTS idx_return_status  ON ReturnOrder(status);

CREATE TRIGGER IF NOT EXISTS trg_return_updated
    AFTER UPDATE ON ReturnOrder
    FOR EACH ROW
BEGIN
    UPDATE ReturnOrder SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE returnId = OLD.returnId;
END;

-- ------------------------------------------------------------
-- ReturnOrderItem (chi tiết trả hàng theo từng dòng)
-- ------------------------------------------------------------
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

CREATE INDEX IF NOT EXISTS idx_returnitem_return    ON ReturnOrderItem(returnId);
CREATE INDEX IF NOT EXISTS idx_returnitem_orderitem ON ReturnOrderItem(orderItemId);

CREATE TRIGGER IF NOT EXISTS trg_returnitem_subtotal
    AFTER INSERT ON ReturnOrderItem
    FOR EACH ROW
BEGIN
    UPDATE ReturnOrderItem SET subtotal = NEW.quantity * NEW.unitPrice
    WHERE returnItemId = NEW.returnItemId;
END;

-- ------------------------------------------------------------
-- SalesOutbound (xuất kho cho đơn bán hàng)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS SalesOutbound (
    outboundId    TEXT PRIMARY KEY,
    orderId       TEXT NOT NULL REFERENCES "Order"(orderId) ON DELETE CASCADE,
    warehouseId   TEXT NOT NULL,
    status        TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','POSTED','CANCELLED')),
    createdBy     TEXT,
    createdAt     TEXT NOT NULL DEFAULT (datetime('now')),
    postedBy      TEXT,
    postedAt      TEXT,
    note          TEXT,
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_sales_outbound_order ON SalesOutbound(orderId);
CREATE INDEX IF NOT EXISTS idx_sales_outbound_wh    ON SalesOutbound(warehouseId);

-- ------------------------------------------------------------
-- SalesOutboundItem (chi tiết xuất kho)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS SalesOutboundItem (
    outboundItemId TEXT PRIMARY KEY,
    outboundId     TEXT NOT NULL REFERENCES SalesOutbound(outboundId) ON DELETE CASCADE,
    productId      TEXT NOT NULL,
    variantId      TEXT,
    quantity       INTEGER NOT NULL DEFAULT 1 CHECK(quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_sales_outbound_item ON SalesOutboundItem(outboundId);

-- ------------------------------------------------------------
-- VIEW: v_order_summary
-- ------------------------------------------------------------
CREATE VIEW IF NOT EXISTS v_order_summary AS
SELECT
    o.orderId,
    o.orderDate,
    o.status AS order_status,
    o.totalAmount,
    o.discountAmount,
    o.finalAmount,
    c.cashierId,
    e.fullName   AS cashier_name,
    s.storeId,
    s.name       AS store_name,
    p.paymentId,
    p.method     AS payment_method,
    p.amountPaid,
    p.status     AS payment_status,
    COUNT(oi.orderItemId) AS item_count,
    SUM(oi.quantity)      AS total_qty
FROM "Order" o
JOIN Cashier  c  ON c.cashierId  = o.cashierId
JOIN Employee e  ON e.employeeId = c.employeeId
JOIN Store    s  ON s.storeId    = o.storeId
LEFT JOIN Payment   p  ON p.orderId  = o.orderId
LEFT JOIN OrderItem oi ON oi.orderId = o.orderId
GROUP BY o.orderId;

-- ------------------------------------------------------------
-- VIEW: v_daily_revenue
-- ------------------------------------------------------------
CREATE VIEW IF NOT EXISTS v_daily_revenue AS
SELECT
    date(o.orderDate)  AS sale_date,
    s.storeId,
    s.name             AS store_name,
    COUNT(o.orderId)   AS total_orders,
    SUM(o.finalAmount) AS total_revenue,
    SUM(o.discountAmount) AS total_discount,
    SUM(CASE WHEN p.method = 'CASH' THEN p.amountPaid ELSE 0 END) AS cash_revenue,
    SUM(CASE WHEN p.method = 'QR'   THEN p.amountPaid ELSE 0 END) AS qr_revenue
FROM "Order" o
JOIN Store   s ON s.storeId = o.storeId
LEFT JOIN Payment p ON p.orderId = o.orderId AND p.status = 'COMPLETED'
WHERE o.status = 'COMPLETED'
GROUP BY date(o.orderDate), s.storeId;

-- ------------------------------------------------------------
-- VIEW: v_order_items_detail
-- ------------------------------------------------------------
CREATE VIEW IF NOT EXISTS v_order_items_detail AS
SELECT
    oi.orderItemId,
    oi.orderId,
    o.orderDate,
    o.status AS order_status,
    oi.productId,
    p.Name         AS product_name,
    p.product_code,
    pv.variant_id,
    pv.variant_name,
    pv.barcode,
    oi.quantity,
    oi.unitPrice,   -- historical price frozen at sale time
    oi.subtotal
FROM OrderItem      oi
JOIN "Order"        o  ON o.orderId   = oi.orderId
JOIN Product        p  ON p.ProductID = oi.productId
LEFT JOIN ProductVariant pv ON pv.variant_id = oi.variantId;

-- ------------------------------------------------------------
-- SEED DATA
-- ------------------------------------------------------------
INSERT OR IGNORE INTO Cashier VALUES
('CSH-001','EMP-001','STORE-001',1,datetime('now'),'PENDING',1),
('CSH-002','EMP-002','STORE-001',1,datetime('now'),'PENDING',1),
('CSH-003','EMP-004','STORE-003',1,datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO "Order"(orderId,cashierId,shiftId,storeId,customerId,orderDate,status,totalAmount,discountAmount,finalAmount,taxRate,taxAmount,paymentStatus,paidAmount,debtAmount,note,last_modified,sync_status,version) VALUES
('ORD-001','CSH-001',NULL,'STORE-001',NULL,datetime('2026-03-07 09:15:00'),'COMPLETED',45000.0,0.0,45000.0,0.1,0.0,'PAID',45000.0,0.0,NULL,datetime('now'),'SYNCED',1),
('ORD-002','CSH-001',NULL,'STORE-001',NULL,datetime('2026-03-07 10:30:00'),'COMPLETED',28000.0,2800.0,25200.0,0.1,0.0,'PAID',25200.0,0.0,'Member discount',datetime('now'),'SYNCED',1),
('ORD-003','CSH-002',NULL,'STORE-001',NULL,datetime('2026-03-07 14:45:00'),'COMPLETED',55000.0,0.0,55000.0,0.1,0.0,'PAID',55000.0,0.0,NULL,datetime('now'),'SYNCED',1),
('ORD-004','CSH-003',NULL,'STORE-003',NULL,datetime('2026-03-07 11:00:00'),'COMPLETED',15000.0,0.0,15000.0,0.1,0.0,'PAID',15000.0,0.0,NULL,datetime('now'),'PENDING',1),
('ORD-005','CSH-001',NULL,'STORE-001',NULL,datetime('now'),'PENDING',30000.0,0.0,30000.0,0.1,0.0,'UNPAID',0.0,30000.0,NULL,datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO OrderItem VALUES
('OI-001','ORD-001','PROD-001','VAR-001',2,10000.0,20000.0),
('OI-002','ORD-001','PROD-003','VAR-004',1,15000.0,15000.0),
('OI-003','ORD-001','PROD-002','VAR-003',1,10000.0,10000.0),
('OI-004','ORD-002','PROD-004','VAR-006',1,28000.0,28000.0),
('OI-005','ORD-003','PROD-005','VAR-007',1,25000.0,25000.0),
('OI-006','ORD-003','PROD-001','VAR-001',3,10000.0,30000.0),
('OI-007','ORD-004','PROD-002','VAR-003',1,15000.0,15000.0),
('OI-008','ORD-005','PROD-003','VAR-005',2,15000.0,30000.0);

INSERT OR IGNORE INTO Payment VALUES
('PAY-001','ORD-001',datetime('2026-03-07 09:15:30'),'CASH',45000.0,'COMPLETED',datetime('now'),'SYNCED',1),
('PAY-002','ORD-002',datetime('2026-03-07 10:30:45'),'QR',25200.0,'COMPLETED',datetime('now'),'SYNCED',1),
('PAY-003','ORD-003',datetime('2026-03-07 14:45:15'),'CASH',55000.0,'COMPLETED',datetime('now'),'SYNCED',1),
('PAY-004','ORD-004',datetime('2026-03-07 11:00:20'),'CASH',15000.0,'COMPLETED',datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO CashPayment VALUES
('PAY-001',50000.0,5000.0),
('PAY-003',60000.0,5000.0),
('PAY-004',20000.0,5000.0);

INSERT OR IGNORE INTO QRPayment VALUES
('PAY-002','QR-CODE-ABC123','TXN-MOMO-20260307-001');

INSERT OR IGNORE INTO Receipt VALUES
('RCT-001','PAY-001',datetime('2026-03-07 09:15:35'),'THERMAL',datetime('now'),'SYNCED',1),
('RCT-002','PAY-002',datetime('2026-03-07 10:30:50'),'THERMAL',datetime('now'),'SYNCED',1),
('RCT-003','PAY-003',datetime('2026-03-07 14:45:20'),'THERMAL',datetime('now'),'SYNCED',1),
('RCT-004','PAY-004',datetime('2026-03-07 11:00:25'),'THERMAL',datetime('now'),'PENDING',1);


