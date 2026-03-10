-- ============================================================
-- MODULE 2: INVENTORY (Quản lý kho)
-- Load Order: 3rd (after module3_product & module5_hr_shift)
-- ============================================================
-- NOTE: PRAGMA foreign_keys is managed by Main.java (OFF during load, ON at runtime)
PRAGMA journal_mode = WAL;

-- ------------------------------------------------------------
-- Supplier
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Supplier (
    supplierID    TEXT PRIMARY KEY,
    name_supplier TEXT NOT NULL,
    isCooperating INTEGER NOT NULL DEFAULT 1,  -- 1=active partner
    contact_phone TEXT,
    contact_email TEXT,
    address       TEXT,
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_supplier_name   ON Supplier(name_supplier);
CREATE INDEX IF NOT EXISTS idx_supplier_active ON Supplier(isCooperating);

CREATE TRIGGER IF NOT EXISTS trg_supplier_updated
    AFTER UPDATE ON Supplier
    FOR EACH ROW
BEGIN
    UPDATE Supplier SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE supplierID = OLD.supplierID;
END;

-- ------------------------------------------------------------
-- Warehouse
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS warehouse (
    warehouse_id         TEXT PRIMARY KEY,
    warehouse_code       TEXT NOT NULL UNIQUE,
    warehouse_name       TEXT NOT NULL,
    address              TEXT,
    is_active            INTEGER NOT NULL DEFAULT 1,
    remaining_capacity   INTEGER DEFAULT 0,
    is_low_stock         INTEGER NOT NULL DEFAULT 0,
    threshold            INTEGER DEFAULT 10,
    created_at           TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at           TEXT NOT NULL DEFAULT (datetime('now')),
    last_modified        TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status          TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version              INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_warehouse_code   ON warehouse(warehouse_code);
CREATE INDEX IF NOT EXISTS idx_warehouse_active ON warehouse(is_active);

CREATE TRIGGER IF NOT EXISTS trg_warehouse_updated
    AFTER UPDATE ON warehouse
    FOR EACH ROW
BEGIN
    UPDATE warehouse SET updated_at = datetime('now'), last_modified = datetime('now'), version = OLD.version + 1
    WHERE warehouse_id = OLD.warehouse_id;
END;

-- ------------------------------------------------------------
-- warehouse_balances (current stock per product per warehouse)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS warehouse_balances (
    warehouse_id  TEXT NOT NULL REFERENCES warehouse(warehouse_id) ON DELETE CASCADE,
    product_id    TEXT NOT NULL REFERENCES Product(ProductID) ON DELETE CASCADE,
    on_hand_qty   INTEGER NOT NULL DEFAULT 0,
    reserved_qty  INTEGER NOT NULL DEFAULT 0,
    updated_at    TEXT NOT NULL DEFAULT (datetime('now')),
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    PRIMARY KEY (warehouse_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_wbalance_warehouse ON warehouse_balances(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_wbalance_product   ON warehouse_balances(product_id);

CREATE TRIGGER IF NOT EXISTS trg_wbalance_updated
    AFTER UPDATE ON warehouse_balances
    FOR EACH ROW
BEGIN
    UPDATE warehouse_balances SET updated_at = datetime('now'), last_modified = datetime('now')
    WHERE warehouse_id = OLD.warehouse_id AND product_id = OLD.product_id;
END;

-- ------------------------------------------------------------
-- InventoryManager
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS InventoryManager (
    managerId     TEXT PRIMARY KEY,
    employeeId    TEXT NOT NULL UNIQUE REFERENCES Employee(employeeId) ON DELETE CASCADE,
    warehouse_id  TEXT REFERENCES warehouse(warehouse_id) ON DELETE SET NULL,
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_invmanager_warehouse ON InventoryManager(warehouse_id);

CREATE TRIGGER IF NOT EXISTS trg_invmanager_updated
    AFTER UPDATE ON InventoryManager
    FOR EACH ROW
BEGIN
    UPDATE InventoryManager SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE managerId = OLD.managerId;
END;

-- ------------------------------------------------------------
-- WarehouseSupervisor
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS WarehouseSupervisor (
    managerId     TEXT PRIMARY KEY,
    employeeId    TEXT NOT NULL UNIQUE REFERENCES Employee(employeeId) ON DELETE CASCADE,
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE TRIGGER IF NOT EXISTS trg_whsupervisor_updated
    AFTER UPDATE ON WarehouseSupervisor
    FOR EACH ROW
BEGIN
    UPDATE WarehouseSupervisor SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE managerId = OLD.managerId;
END;

-- ------------------------------------------------------------
-- InventoryStaff
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS InventoryStaff (
    inventoryStaffID TEXT PRIMARY KEY,
    employeeId       TEXT NOT NULL UNIQUE REFERENCES Employee(employeeId) ON DELETE CASCADE,
    warehouse_id     TEXT REFERENCES warehouse(warehouse_id) ON DELETE SET NULL,
    last_modified    TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status      TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version          INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_invstaff_warehouse ON InventoryStaff(warehouse_id);

CREATE TRIGGER IF NOT EXISTS trg_invstaff_updated
    AFTER UPDATE ON InventoryStaff
    FOR EACH ROW
BEGIN
    UPDATE InventoryStaff SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE inventoryStaffID = OLD.inventoryStaffID;
END;

-- ------------------------------------------------------------
-- inbound_documents (nhập kho)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inbound_documents (
    inbound_id          TEXT PRIMARY KEY,
    inbound_no          INTEGER NOT NULL,
    source_warehouse_id TEXT NOT NULL REFERENCES warehouse(warehouse_id) ON DELETE RESTRICT,
    status_code         TEXT NOT NULL DEFAULT 'DRAFT' CHECK(status_code IN ('DRAFT','SUBMITTED','APPROVED','POSTED','CANCELLED')),
    created_by          TEXT NOT NULL REFERENCES Employee(employeeId) ON DELETE RESTRICT,
    created_at          TEXT NOT NULL DEFAULT (datetime('now')),
    posted_by           TEXT REFERENCES Employee(employeeId) ON DELETE SET NULL,
    posted_at           TEXT,
    product_id          TEXT REFERENCES Product(ProductID) ON DELETE RESTRICT,
    qty                 INTEGER NOT NULL DEFAULT 0,
    acceptBy            TEXT REFERENCES Employee(employeeId) ON DELETE SET NULL,
    bySupplier          TEXT REFERENCES Supplier(supplierID) ON DELETE SET NULL,
    dateDoIn            TEXT,
    qlty                INTEGER DEFAULT 0,
    last_modified       TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version             INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_inbound_warehouse  ON inbound_documents(source_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inbound_status     ON inbound_documents(status_code);
CREATE INDEX IF NOT EXISTS idx_inbound_created_at ON inbound_documents(created_at);

CREATE TRIGGER IF NOT EXISTS trg_inbound_updated
    AFTER UPDATE ON inbound_documents
    FOR EACH ROW
BEGIN
    UPDATE inbound_documents SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE inbound_id = OLD.inbound_id;
END;

-- ------------------------------------------------------------
-- outbound_documents (xuất kho)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS outbound_documents (
    outbound_id         TEXT PRIMARY KEY,
    outbound_no         INTEGER NOT NULL,
    source_warehouse_id TEXT NOT NULL REFERENCES warehouse(warehouse_id) ON DELETE RESTRICT,
    status_code         TEXT NOT NULL DEFAULT 'DRAFT' CHECK(status_code IN ('DRAFT','SUBMITTED','APPROVED','POSTED','CANCELLED')),
    created_by          TEXT NOT NULL REFERENCES Employee(employeeId) ON DELETE RESTRICT,
    created_at          TEXT NOT NULL DEFAULT (datetime('now')),
    posted_by           TEXT REFERENCES Employee(employeeId) ON DELETE SET NULL,
    posted_at           TEXT,
    product_id          TEXT REFERENCES Product(ProductID) ON DELETE RESTRICT,
    qty                 INTEGER NOT NULL DEFAULT 0,
    acceptBy            TEXT REFERENCES Employee(employeeId) ON DELETE SET NULL,
    last_modified       TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version             INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_outbound_warehouse  ON outbound_documents(source_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_outbound_status     ON outbound_documents(status_code);

CREATE TRIGGER IF NOT EXISTS trg_outbound_updated
    AFTER UPDATE ON outbound_documents
    FOR EACH ROW
BEGIN
    UPDATE outbound_documents SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE outbound_id = OLD.outbound_id;
END;

-- ------------------------------------------------------------
-- transfer_requests (điều chuyển kho)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transfer_requests (
    transfer_id         TEXT PRIMARY KEY,
    transfer_no         INTEGER NOT NULL,
    type_code           TEXT NOT NULL DEFAULT 'TRANSFER' CHECK(type_code IN ('TRANSFER','RETURN','ADJUSTMENT')),
    status_code         TEXT NOT NULL DEFAULT 'DRAFT' CHECK(status_code IN ('DRAFT','SUBMITTED','APPROVED','IN_TRANSIT','COMPLETED','CANCELLED')),
    priority_code       TEXT NOT NULL DEFAULT 'NORMAL' CHECK(priority_code IN ('LOW','NORMAL','HIGH','URGENT')),
    created_by          TEXT NOT NULL REFERENCES Employee(employeeId) ON DELETE RESTRICT,
    required_date       TEXT,
    note                TEXT,
    created_at          TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at          TEXT NOT NULL DEFAULT (datetime('now')),
    source_warehouse_id TEXT REFERENCES warehouse(warehouse_id) ON DELETE SET NULL,
    store_id            TEXT REFERENCES Store(storeId) ON DELETE SET NULL,
    last_modified       TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version             INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_transfer_warehouse ON transfer_requests(source_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_transfer_store     ON transfer_requests(store_id);
CREATE INDEX IF NOT EXISTS idx_transfer_status    ON transfer_requests(status_code);

CREATE TRIGGER IF NOT EXISTS trg_transfer_updated
    AFTER UPDATE ON transfer_requests
    FOR EACH ROW
BEGIN
    UPDATE transfer_requests SET updated_at = datetime('now'), last_modified = datetime('now'), version = OLD.version + 1
    WHERE transfer_id = OLD.transfer_id;
END;

-- ------------------------------------------------------------
-- transfer_request_items
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transfer_request_items (
    transfer_item_id TEXT PRIMARY KEY,
    transfer_id      TEXT NOT NULL REFERENCES transfer_requests(transfer_id) ON DELETE CASCADE,
    product_id       TEXT REFERENCES Product(ProductID) ON DELETE RESTRICT,
    qty              INTEGER NOT NULL DEFAULT 0,
    by_Supplier      TEXT REFERENCES Supplier(supplierID) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_transferitem_transfer ON transfer_request_items(transfer_id);
CREATE INDEX IF NOT EXISTS idx_transferitem_product  ON transfer_request_items(product_id);

-- ------------------------------------------------------------
-- transfer_decisions
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transfer_decisions (
    decision_id   TEXT PRIMARY KEY,
    transfer_id   TEXT REFERENCES transfer_requests(transfer_id) ON DELETE CASCADE,
    decision_code TEXT NOT NULL CHECK(decision_code IN ('APPROVED','REJECTED','PARTIAL')),
    reason        TEXT,
    decided_by    TEXT REFERENCES Employee(employeeId) ON DELETE SET NULL,
    decided_at    TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_decision_transfer ON transfer_decisions(transfer_id);

-- ------------------------------------------------------------
-- inventory_adjustment_requests (yêu cầu điều chỉnh tồn kho)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_adjustment_requests (
    adj_req_id          TEXT PRIMARY KEY,
    adj_req_no          INTEGER NOT NULL,
    source_warehouse_id TEXT NOT NULL REFERENCES warehouse(warehouse_id) ON DELETE RESTRICT,
    created_by          TEXT NOT NULL REFERENCES Employee(employeeId) ON DELETE RESTRICT,
    reason_type         TEXT NOT NULL DEFAULT 'PHYSICAL_COUNT' CHECK(reason_type IN ('PHYSICAL_COUNT','DAMAGE','EXPIRY','SYSTEM_ERROR','TRANSFER')),
    status_code         TEXT NOT NULL DEFAULT 'DRAFT' CHECK(status_code IN ('DRAFT','SUBMITTED','APPROVED','REJECTED','PROCESSED')),
    note                TEXT,
    processed_by        TEXT REFERENCES Employee(employeeId) ON DELETE SET NULL,
    processed_at        TEXT,
    supervisor_note     TEXT,
    created_at          TEXT NOT NULL DEFAULT (datetime('now')),
    submitted_at        TEXT,
    updated_at          TEXT NOT NULL DEFAULT (datetime('now')),
    tranfer_id          TEXT REFERENCES transfer_requests(transfer_id) ON DELETE SET NULL,
    last_modified       TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version             INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_adjreq_warehouse ON inventory_adjustment_requests(source_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_adjreq_status    ON inventory_adjustment_requests(status_code);

CREATE TRIGGER IF NOT EXISTS trg_adjreq_updated
    AFTER UPDATE ON inventory_adjustment_requests
    FOR EACH ROW
BEGIN
    UPDATE inventory_adjustment_requests SET updated_at = datetime('now'), last_modified = datetime('now'), version = OLD.version + 1
    WHERE adj_req_id = OLD.adj_req_id;
END;

-- ------------------------------------------------------------
-- inventory_adjustment_request_items
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_adjustment_request_items (
    adj_req_item_id  TEXT PRIMARY KEY,
    adj_req_id       TEXT NOT NULL REFERENCES inventory_adjustment_requests(adj_req_id) ON DELETE CASCADE,
    product_id       TEXT REFERENCES Product(ProductID) ON DELETE RESTRICT,
    suggested_qty    INTEGER DEFAULT 0,
    requested_qty    INTEGER NOT NULL DEFAULT 0,
    aging_days       INTEGER DEFAULT 0,
    overstock_amount INTEGER DEFAULT 0,
    evidence_note    TEXT
);

CREATE INDEX IF NOT EXISTS idx_adjitem_reqid   ON inventory_adjustment_request_items(adj_req_id);
CREATE INDEX IF NOT EXISTS idx_adjitem_product ON inventory_adjustment_request_items(product_id);

-- ------------------------------------------------------------
-- store_adjustment_requests (cửa hàng yêu cầu hàng từ kho)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS store_adjustment_requests (
    store_req_id  TEXT PRIMARY KEY,
    store_id      TEXT NOT NULL REFERENCES Store(storeId) ON DELETE CASCADE,
    created_by    TEXT NOT NULL REFERENCES Employee(employeeId) ON DELETE RESTRICT,
    store_req_no  INTEGER NOT NULL,
    typeRequest   TEXT NOT NULL DEFAULT 'IMPORT' CHECK(typeRequest IN ('IMPORT','RETURN','EXCHANGE')),
    status_code   TEXT NOT NULL DEFAULT 'DRAFT' CHECK(status_code IN ('DRAFT','SUBMITTED','APPROVED','REJECTED','COMPLETED')),
    priority_code TEXT NOT NULL DEFAULT 'NORMAL' CHECK(priority_code IN ('LOW','NORMAL','HIGH','URGENT')),
    product_id    TEXT REFERENCES Product(ProductID) ON DELETE SET NULL,
    need_date     TEXT,
    note          TEXT,
    reject_reason TEXT,
    rejected_at   TEXT,
    created_at    TEXT NOT NULL DEFAULT (datetime('now')),
    submitted_at  TEXT,
    reasonReturn  TEXT,
    updated_at    TEXT NOT NULL DEFAULT (datetime('now')),
    rejected_by   TEXT REFERENCES Employee(employeeId) ON DELETE SET NULL,
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_storereq_store  ON store_adjustment_requests(store_id);
CREATE INDEX IF NOT EXISTS idx_storereq_status ON store_adjustment_requests(status_code);

CREATE TRIGGER IF NOT EXISTS trg_storereq_updated
    AFTER UPDATE ON store_adjustment_requests
    FOR EACH ROW
BEGIN
    UPDATE store_adjustment_requests SET updated_at = datetime('now'), last_modified = datetime('now'), version = OLD.version + 1
    WHERE store_req_id = OLD.store_req_id;
END;

-- ------------------------------------------------------------
-- store_adjustment_request_items
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS store_adjustment_request_items (
    store_req_item_id TEXT PRIMARY KEY,
    store_req_id      TEXT NOT NULL REFERENCES store_adjustment_requests(store_req_id) ON DELETE CASCADE,
    product_id        TEXT REFERENCES Product(ProductID) ON DELETE RESTRICT,
    requested_qty     INTEGER NOT NULL DEFAULT 0,
    note              TEXT
);

CREATE INDEX IF NOT EXISTS idx_storereqitem_req     ON store_adjustment_request_items(store_req_id);
CREATE INDEX IF NOT EXISTS idx_storereqitem_product ON store_adjustment_request_items(product_id);

-- ------------------------------------------------------------
-- inventory_reservations (đặt chỗ hàng tồn kho)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_reservations (
    reservation_id      TEXT PRIMARY KEY,
    source_warehouse_id TEXT NOT NULL REFERENCES warehouse(warehouse_id) ON DELETE CASCADE,
    adj_req_id          TEXT REFERENCES inventory_adjustment_requests(adj_req_id) ON DELETE SET NULL,
    product_id          TEXT REFERENCES Product(ProductID) ON DELETE RESTRICT,
    reserved_qty        INTEGER NOT NULL DEFAULT 0,
    status_code         TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status_code IN ('ACTIVE','RELEASED','EXPIRED','FULFILLED')),
    created_at          TEXT NOT NULL DEFAULT (datetime('now')),
    released_at         TEXT,
    released_by         TEXT REFERENCES Employee(employeeId) ON DELETE SET NULL,
    release_reason      TEXT,
    last_modified       TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version             INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_reservation_warehouse ON inventory_reservations(source_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_reservation_status    ON inventory_reservations(status_code);

CREATE TRIGGER IF NOT EXISTS trg_reservation_updated
    AFTER UPDATE ON inventory_reservations
    FOR EACH ROW
BEGIN
    UPDATE inventory_reservations SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE reservation_id = OLD.reservation_id;
END;

-- ------------------------------------------------------------
-- receipt_documents (phiếu nhận hàng tại cửa hàng)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS receipt_documents (
    receipt_id    TEXT PRIMARY KEY,
    store_id      TEXT REFERENCES Store(storeId) ON DELETE SET NULL,
    status_code   TEXT NOT NULL DEFAULT 'DRAFT' CHECK(status_code IN ('DRAFT','POSTED','CANCELLED')),
    created_by    TEXT REFERENCES Employee(employeeId) ON DELETE SET NULL,
    created_at    TEXT NOT NULL DEFAULT (datetime('now')),
    posted_by     TEXT REFERENCES Employee(employeeId) ON DELETE SET NULL,
    posted_at     TEXT,
    product_id    TEXT REFERENCES Product(ProductID) ON DELETE RESTRICT,
    qty           INTEGER NOT NULL DEFAULT 0,
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_receiptdoc_store  ON receipt_documents(store_id);
CREATE INDEX IF NOT EXISTS idx_receiptdoc_status ON receipt_documents(status_code);

CREATE TRIGGER IF NOT EXISTS trg_receiptdoc_updated
    AFTER UPDATE ON receipt_documents
    FOR EACH ROW
BEGIN
    UPDATE receipt_documents SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE receipt_id = OLD.receipt_id;
END;

-- ------------------------------------------------------------
-- InventoryInspection (kiểm tra hàng hóa)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS InventoryInspection (
    inspectionId     TEXT PRIMARY KEY,
    productId        TEXT NOT NULL REFERENCES Product(ProductID) ON DELETE CASCADE,
    warehouse_id     TEXT REFERENCES warehouse(warehouse_id) ON DELETE SET NULL,
    productName      TEXT,
    quantityChecked  INTEGER NOT NULL DEFAULT 0,
    damagedQuantity  INTEGER NOT NULL DEFAULT 0,
    status           TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','IN_PROGRESS','COMPLETED','REJECTED')),
    note             TEXT,
    inspectedBy      TEXT REFERENCES Employee(employeeId) ON DELETE SET NULL,
    processBy        TEXT REFERENCES Employee(employeeId) ON DELETE SET NULL,
    created_at       TEXT NOT NULL DEFAULT (datetime('now')),
    last_modified    TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status      TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version          INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_inspection_product   ON InventoryInspection(productId);
CREATE INDEX IF NOT EXISTS idx_inspection_warehouse ON InventoryInspection(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inspection_status    ON InventoryInspection(status);

CREATE TRIGGER IF NOT EXISTS trg_inspection_updated
    AFTER UPDATE ON InventoryInspection
    FOR EACH ROW
BEGIN
    UPDATE InventoryInspection SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE inspectionId = OLD.inspectionId;
END;

-- ------------------------------------------------------------
-- product_need_import (hàng cần nhập thêm)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_need_import (
    need_products_item_id TEXT PRIMARY KEY,
    need_products_item_no INTEGER NOT NULL,
    product_id            TEXT NOT NULL REFERENCES Product(ProductID) ON DELETE CASCADE,
    warehouse_id          TEXT REFERENCES warehouse(warehouse_id) ON DELETE SET NULL,
    bySupplier            TEXT REFERENCES Supplier(supplierID) ON DELETE SET NULL,
    addressAvailable      TEXT,
    qty                   INTEGER NOT NULL DEFAULT 0,
    create_at             TEXT NOT NULL DEFAULT (datetime('now')),
    exportable_ratio      INTEGER DEFAULT 0,
    last_modified         TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status           TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version               INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_needimport_product   ON product_need_import(product_id);
CREATE INDEX IF NOT EXISTS idx_needimport_warehouse ON product_need_import(warehouse_id);

CREATE TRIGGER IF NOT EXISTS trg_needimport_updated
    AFTER UPDATE ON product_need_import
    FOR EACH ROW
BEGIN
    UPDATE product_need_import SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE need_products_item_id = OLD.need_products_item_id;
END;

-- ------------------------------------------------------------
-- VIEW: v_warehouse_stock
-- ------------------------------------------------------------
CREATE VIEW IF NOT EXISTS v_warehouse_stock AS
SELECT
    w.warehouse_id,
    w.warehouse_code,
    w.warehouse_name,
    p.ProductID,
    p.product_code,
    p.Name AS product_name,
    wb.on_hand_qty,
    wb.reserved_qty,
    (wb.on_hand_qty - wb.reserved_qty) AS available_qty,
    w.threshold,
    CASE WHEN (wb.on_hand_qty - wb.reserved_qty) <= w.threshold THEN 1 ELSE 0 END AS is_low_stock,
    wb.updated_at
FROM warehouse_balances wb
JOIN warehouse w ON w.warehouse_id = wb.warehouse_id
JOIN Product   p ON p.ProductID    = wb.product_id
WHERE w.is_active = 1
  AND p.Status    = 'ACTIVE';

-- ------------------------------------------------------------
-- VIEW: v_low_stock_alert
-- ------------------------------------------------------------
CREATE VIEW IF NOT EXISTS v_low_stock_alert AS
SELECT * FROM v_warehouse_stock
WHERE is_low_stock = 1
ORDER BY available_qty ASC;

-- ------------------------------------------------------------
-- SEED DATA
-- ------------------------------------------------------------
INSERT OR IGNORE INTO Supplier VALUES
('SUP-001','Coca-Cola Vietnam',1,'0901234567','order@coca-cola.vn','123 Nguyen Van Cu, Quan 5, HCM',datetime('now'),'PENDING',1),
('SUP-002','Tan Hiep Phat Group',1,'0912345678','supply@thp.vn','219 Nguyen Van Cu, Binh Duong',datetime('now'),'PENDING',1),
('SUP-003','Pepsico Vietnam',1,'0923456789','sales@pepsico.vn','456 Binh Duong Avenue',datetime('now'),'PENDING',1),
('SUP-004','Unilever Vietnam',1,'0934567890','trade@unilever.vn','156 Nguyen Luong Bang, Hanoi',datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO warehouse VALUES
('WH-001','WH001','Central Warehouse - Ha Noi','10 Yen Vien Industrial Zone, Gia Lam, Ha Noi',1,5000,0,100,datetime('now'),datetime('now'),datetime('now'),'PENDING',1),
('WH-002','WH002','Central Warehouse - HCM','55 Tan Thuan, Quan 7, TP HCM',1,8000,0,150,datetime('now'),datetime('now'),datetime('now'),'PENDING',1),
('WH-003','WH003','Store S001 Storage','12 Hang Bai, Hoan Kiem, Ha Noi',1,200,0,20,datetime('now'),datetime('now'),datetime('now'),'PENDING',1),
('WH-004','WH004','Store S003 Storage','78 Le Loi, Quan 1, TP HCM',1,200,0,20,datetime('now'),datetime('now'),datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO InventoryManager VALUES
('INVMGR-001','EMP-003','WH-001',datetime('now'),'PENDING',1),
('INVMGR-002','EMP-005','WH-002',datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO InventoryStaff VALUES
('INVSTF-001','EMP-001','WH-003',datetime('now'),'PENDING',1),
('INVSTF-002','EMP-004','WH-004',datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO WarehouseSupervisor VALUES
('WHSUP-001','EMP-006',datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO warehouse_balances VALUES
('WH-001','PROD-001',500,50,datetime('now'),datetime('now'),'PENDING'),
('WH-001','PROD-002',300,30,datetime('now'),datetime('now'),'PENDING'),
('WH-001','PROD-003',400,0,datetime('now'),datetime('now'),'PENDING'),
('WH-001','PROD-004',200,20,datetime('now'),datetime('now'),'PENDING'),
('WH-001','PROD-005',150,0,datetime('now'),datetime('now'),'PENDING'),
('WH-003','PROD-001',50,5,datetime('now'),datetime('now'),'PENDING'),
('WH-003','PROD-002',30,0,datetime('now'),datetime('now'),'PENDING'),
('WH-003','PROD-003',25,0,datetime('now'),datetime('now'),'PENDING'),
('WH-004','PROD-001',45,5,datetime('now'),datetime('now'),'PENDING'),
('WH-004','PROD-004',20,2,datetime('now'),datetime('now'),'PENDING');

INSERT OR IGNORE INTO inbound_documents VALUES
('IN-001',1001,'WH-001','POSTED','EMP-003',datetime('now','start of day'),'EMP-003',datetime('now'),'PROD-001',100,NULL,'SUP-001',datetime('2026-03-05'),100,datetime('now'),'SYNCED',1),
('IN-002',1002,'WH-001','POSTED','EMP-003',datetime('now','start of day'),'EMP-003',datetime('now'),'PROD-002',60,NULL,'SUP-002',datetime('2026-03-05'),60,datetime('now'),'SYNCED',1),
('IN-003',1003,'WH-003','DRAFT','EMP-001',datetime('now'),NULL,NULL,'PROD-001',50,'EMP-003','SUP-001',NULL,50,datetime('now'),'PENDING',1);

