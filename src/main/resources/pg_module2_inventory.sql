-- ============================================================
-- MODULE 2: INVENTORY (PostgreSQL)
-- ============================================================

CREATE TABLE IF NOT EXISTS Supplier (
    "supplierID"    TEXT PRIMARY KEY,
    name_supplier   TEXT NOT NULL,
    "isCooperating" INTEGER NOT NULL DEFAULT 1,
    contact_phone   TEXT,
    contact_email   TEXT,
    address         TEXT,
    last_modified   TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status     TEXT NOT NULL DEFAULT 'PENDING',
    version         INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS warehouse (
    warehouse_id       TEXT PRIMARY KEY,
    warehouse_code     TEXT NOT NULL UNIQUE,
    warehouse_name     TEXT NOT NULL,
    address            TEXT,
    is_active          INTEGER NOT NULL DEFAULT 1,
    remaining_capacity INTEGER DEFAULT 0,
    is_low_stock       INTEGER NOT NULL DEFAULT 0,
    threshold          INTEGER DEFAULT 10,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified      TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status        TEXT NOT NULL DEFAULT 'PENDING',
    version            INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS warehouse_balances (
    warehouse_id  TEXT NOT NULL REFERENCES warehouse(warehouse_id) ON DELETE CASCADE,
    product_id    TEXT NOT NULL REFERENCES Product("ProductID") ON DELETE CASCADE,
    on_hand_qty   INTEGER NOT NULL DEFAULT 0,
    reserved_qty  INTEGER NOT NULL DEFAULT 0,
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (warehouse_id, product_id)
);

CREATE TABLE IF NOT EXISTS InventoryManager (
    "managerId"   TEXT PRIMARY KEY,
    "employeeId"  TEXT NOT NULL UNIQUE REFERENCES Employee("employeeId") ON DELETE CASCADE,
    warehouse_id  TEXT REFERENCES warehouse(warehouse_id) ON DELETE SET NULL,
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS WarehouseSupervisor (
    "managerId"   TEXT PRIMARY KEY,
    "employeeId"  TEXT NOT NULL UNIQUE REFERENCES Employee("employeeId") ON DELETE CASCADE,
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS InventoryStaff (
    "inventoryStaffID" TEXT PRIMARY KEY,
    "employeeId"       TEXT NOT NULL REFERENCES Employee("employeeId") ON DELETE CASCADE,
    last_modified      TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status        TEXT NOT NULL DEFAULT 'PENDING',
    version            INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS transfer_requests (
    transfer_id         TEXT PRIMARY KEY,
    transfer_no         INTEGER,
    type_code           TEXT,
    status_code         TEXT NOT NULL DEFAULT 'DRAFT',
    priority_code       TEXT,
    created_by          TEXT,
    required_date       TIMESTAMP,
    note                TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    source_warehouse_id TEXT REFERENCES warehouse(warehouse_id),
    store_id            TEXT,
    last_modified       TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE IF NOT EXISTS transfer_request_items (
    transfer_item_id TEXT PRIMARY KEY,
    transfer_id      TEXT REFERENCES transfer_requests(transfer_id) ON DELETE CASCADE,
    product_id       TEXT,
    qty              INTEGER NOT NULL DEFAULT 0,
    "by_Supplier"    TEXT,
    last_modified    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS transfer_decisions (
    decision_id   TEXT PRIMARY KEY,
    decision_code TEXT,
    reason        TEXT,
    decided_by    TEXT,
    decided_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS inbound_documents (
    inbound_id          TEXT PRIMARY KEY,
    inbound_no          INTEGER,
    source_warehouse_id TEXT REFERENCES warehouse(warehouse_id),
    status_code         TEXT NOT NULL DEFAULT 'DRAFT',
    created_by          TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    posted_by           TEXT,
    posted_at           TIMESTAMP,
    product_id          TEXT,
    qty                 INTEGER NOT NULL DEFAULT 0,
    "acceptBy"          TEXT,
    "bySupplier"        TEXT,
    "dateDoIn"          TIMESTAMP,
    qlty                INTEGER DEFAULT 0,
    last_modified       TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE IF NOT EXISTS outbound_documents (
    outbound_id         TEXT PRIMARY KEY,
    outbound_no         INTEGER,
    source_warehouse_id TEXT REFERENCES warehouse(warehouse_id),
    status_code         TEXT NOT NULL DEFAULT 'DRAFT',
    created_by          TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    posted_by           TEXT,
    posted_at           TIMESTAMP,
    product_id          TEXT,
    qty                 INTEGER NOT NULL DEFAULT 0,
    "acceptBy"          TEXT,
    last_modified       TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE IF NOT EXISTS inventory_adjustment_requests (
    adj_req_id          TEXT PRIMARY KEY,
    adj_req_no          INTEGER,
    source_warehouse_id TEXT REFERENCES warehouse(warehouse_id),
    created_by          TEXT,
    reason_type         TEXT,
    status_code         TEXT NOT NULL DEFAULT 'DRAFT',
    note                TEXT,
    processed_by        TEXT,
    processed_at        TIMESTAMP,
    supervisor_note     TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    submitted_at        TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    tranfer_id          TEXT,
    last_modified       TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE IF NOT EXISTS inventory_adjustment_request_items (
    adj_req_item_id  TEXT PRIMARY KEY,
    suggested_qty    INTEGER DEFAULT 0,
    aging_days       INTEGER DEFAULT 0,
    requested_qty    INTEGER DEFAULT 0,
    overstock_amount INTEGER DEFAULT 0,
    evidence_note    TEXT,
    adj_req_id       TEXT REFERENCES inventory_adjustment_requests(adj_req_id) ON DELETE CASCADE,
    last_modified    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS inventory_reservations (
    reservation_id      TEXT PRIMARY KEY,
    source_warehouse_id TEXT REFERENCES warehouse(warehouse_id),
    status_code         TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    released_at         TIMESTAMP,
    released_by         TEXT,
    release_reason      TEXT,
    adj_req_id          TEXT,
    last_modified       TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE IF NOT EXISTS store_adjustment_requests (
    store_req_id  TEXT PRIMARY KEY,
    store_id      TEXT,
    created_by    TEXT,
    store_req_no  INTEGER,
    "typeRequest" TEXT,
    status_code   TEXT NOT NULL DEFAULT 'DRAFT',
    priority_code TEXT,
    product_id    TEXT,
    need_date     TIMESTAMP,
    note          TEXT,
    reject_reason TEXT,
    rejected_at   TIMESTAMP,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    submitted_at  TIMESTAMP,
    "reasonReturn" TEXT,
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    rejected_by   TEXT,
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE IF NOT EXISTS store_adjustment_request_items (
    store_req_item_id TEXT PRIMARY KEY,
    requested_qty     INTEGER DEFAULT 0,
    note              TEXT,
    product_id        TEXT,
    store_req_id      TEXT REFERENCES store_adjustment_requests(store_req_id) ON DELETE CASCADE,
    last_modified     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS receipt_documents (
    receipt_id    TEXT PRIMARY KEY,
    store_id      TEXT,
    status_code   TEXT NOT NULL DEFAULT 'DRAFT',
    created_by    TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    posted_by     TEXT,
    posted_at     TIMESTAMP,
    product_id    TEXT,
    qty           INTEGER NOT NULL DEFAULT 0,
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE IF NOT EXISTS InventoryInspection (
    "inspectionId"    TEXT PRIMARY KEY,
    "productId"       TEXT,
    "productName"     TEXT,
    "quantityChecked" INTEGER DEFAULT 0,
    "damagedQuantity" INTEGER DEFAULT 0,
    status            TEXT,
    note              TEXT,
    "inspectedBy"     TEXT,
    "processBy"       TEXT,
    last_modified     TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status       TEXT NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE IF NOT EXISTS product_need_import (
    need_products_item_id TEXT PRIMARY KEY,
    need_products_item_no INTEGER,
    product_id            TEXT,
    "bySupplier"          TEXT,
    "addressAvailable"    TEXT,
    qty                   INTEGER DEFAULT 0,
    create_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    exportable_ratio      INTEGER DEFAULT 0,
    last_modified         TIMESTAMP NOT NULL DEFAULT NOW()
);

