-- ============================================================
-- MODULE 3: PRODUCT (Sản phẩm & Giá)
-- Load Order: 1st (referenced by all other modules)
-- ============================================================
-- NOTE: PRAGMA foreign_keys is managed by Main.java (OFF during load, ON at runtime)
PRAGMA journal_mode = WAL;

-- ------------------------------------------------------------
-- ProductCategory
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ProductCategory (
    category_id   TEXT PRIMARY KEY,
    category_name TEXT NOT NULL,
    description   TEXT,
    status        INTEGER NOT NULL DEFAULT 1,  -- 1=active, 0=inactive
    created_at    TEXT NOT NULL DEFAULT (datetime('now')),
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_category_status ON ProductCategory(status);

CREATE TRIGGER IF NOT EXISTS trg_category_updated
    AFTER UPDATE ON ProductCategory
    FOR EACH ROW
BEGIN
    UPDATE ProductCategory SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE category_id = OLD.category_id;
END;

-- ------------------------------------------------------------
-- Product
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Product (
    ProductID     TEXT PRIMARY KEY,
    product_code  TEXT NOT NULL UNIQUE,
    Name          TEXT NOT NULL,
    category_id   TEXT REFERENCES ProductCategory(category_id) ON DELETE SET NULL,
    brand         TEXT,
    unit          TEXT,
    Description   TEXT,
    Status        TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(Status IN ('ACTIVE','INACTIVE','DISCONTINUED')),
    sales_rate    INTEGER DEFAULT 0,
    CreatedAt     TEXT NOT NULL DEFAULT (datetime('now')),
    UpdatedAt     TEXT NOT NULL DEFAULT (datetime('now')),
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_product_category ON Product(category_id);
CREATE INDEX IF NOT EXISTS idx_product_code     ON Product(product_code);
CREATE INDEX IF NOT EXISTS idx_product_status   ON Product(Status);

CREATE TRIGGER IF NOT EXISTS trg_product_updated
    AFTER UPDATE ON Product
    FOR EACH ROW
BEGIN
    UPDATE Product SET UpdatedAt = datetime('now'), last_modified = datetime('now'), version = OLD.version + 1
    WHERE ProductID = OLD.ProductID;
END;

-- ------------------------------------------------------------
-- ProductVariant
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ProductVariant (
    variant_id    TEXT PRIMARY KEY,
    product_id    TEXT NOT NULL REFERENCES Product(ProductID) ON DELETE CASCADE,
    variant_name  TEXT NOT NULL,
    barcode       TEXT UNIQUE,
    status        INTEGER NOT NULL DEFAULT 1,  -- 1=active
    created_at    TEXT NOT NULL DEFAULT (datetime('now')),
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_variant_product ON ProductVariant(product_id);
CREATE INDEX IF NOT EXISTS idx_variant_barcode ON ProductVariant(barcode);

CREATE TRIGGER IF NOT EXISTS trg_variant_updated
    AFTER UPDATE ON ProductVariant
    FOR EACH ROW
BEGIN
    UPDATE ProductVariant SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE variant_id = OLD.variant_id;
END;

-- ------------------------------------------------------------
-- PriceList
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS PriceList (
    price_list_id   TEXT PRIMARY KEY,
    price_list_name TEXT NOT NULL,
    description     TEXT,
    status          TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE')),
    created_at      TEXT NOT NULL DEFAULT (datetime('now')),
    last_modified   TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status     TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version         INTEGER NOT NULL DEFAULT 1
);

CREATE TRIGGER IF NOT EXISTS trg_pricelist_updated
    AFTER UPDATE ON PriceList
    FOR EACH ROW
BEGIN
    UPDATE PriceList SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE price_list_id = OLD.price_list_id;
END;

-- ------------------------------------------------------------
-- ProductPrice  (historical: price frozen at order time)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ProductPrice (
    price_id      TEXT PRIMARY KEY,
    variant_id    TEXT NOT NULL REFERENCES ProductVariant(variant_id) ON DELETE CASCADE,
    price_list_id TEXT NOT NULL REFERENCES PriceList(price_list_id) ON DELETE RESTRICT,
    price         REAL NOT NULL CHECK(price >= 0),
    start_time    TEXT NOT NULL,           -- datetime: when this price becomes effective
    end_time      TEXT,                    -- NULL = open-ended (current price)
    created_at    TEXT NOT NULL DEFAULT (datetime('now')),
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_price_variant    ON ProductPrice(variant_id);
CREATE INDEX IF NOT EXISTS idx_price_pricelist  ON ProductPrice(price_list_id);
CREATE INDEX IF NOT EXISTS idx_price_start_end  ON ProductPrice(start_time, end_time);

CREATE TRIGGER IF NOT EXISTS trg_productprice_updated
    AFTER UPDATE ON ProductPrice
    FOR EACH ROW
BEGIN
    UPDATE ProductPrice SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE price_id = OLD.price_id;
END;

-- ------------------------------------------------------------
-- VIEW: v_active_price — current effective price per variant
-- ------------------------------------------------------------
CREATE VIEW IF NOT EXISTS v_active_price AS
SELECT
    pp.price_id,
    pp.variant_id,
    pv.variant_name,
    pv.barcode,
    p.ProductID,
    p.Name   AS product_name,
    p.product_code,
    pl.price_list_id,
    pl.price_list_name,
    pp.price,
    pp.start_time,
    pp.end_time
FROM ProductPrice pp
JOIN ProductVariant pv ON pv.variant_id = pp.variant_id
JOIN Product        p  ON p.ProductID   = pv.product_id
JOIN PriceList      pl ON pl.price_list_id = pp.price_list_id
WHERE pp.start_time <= datetime('now')
  AND (pp.end_time IS NULL OR pp.end_time > datetime('now'))
  AND pv.status = 1
  AND p.Status  = 'ACTIVE'
  AND pl.status = 'ACTIVE';

-- ------------------------------------------------------------
-- SEED DATA
-- ------------------------------------------------------------
INSERT OR IGNORE INTO ProductCategory VALUES
('CAT-001','Xi măng','Các loại xi măng',1,datetime('now'),datetime('now'),'PENDING',1),
('CAT-002','Sắt thép','Thép xây dựng các loại',1,datetime('now'),datetime('now'),'PENDING',1),
('CAT-003','Gạch','Gạch xây, gạch ốp lát',1,datetime('now'),datetime('now'),'PENDING',1),
('CAT-004','Cát đá','Cát xây, đá dăm',1,datetime('now'),datetime('now'),'PENDING',1),
('CAT-005','Sơn','Sơn tường, sơn chống thấm',1,datetime('now'),datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO Product VALUES
('PROD-001','XM-001','Xi măng Hà Tiên PCB40','CAT-001','Hà Tiên','bao','Xi măng PCB40 50kg','ACTIVE',150,datetime('now'),datetime('now'),datetime('now'),'PENDING',1),
('PROD-002','XM-002','Xi măng INSEE','CAT-001','INSEE','bao','Xi măng INSEE 50kg','ACTIVE',120,datetime('now'),datetime('now'),datetime('now'),'PENDING',1),
('PROD-003','ST-001','Thép Pomina D10','CAT-002','Pomina','cây','Thép cuộn D10 dài 11.7m','ACTIVE',90,datetime('now'),datetime('now'),datetime('now'),'PENDING',1),
('PROD-004','ST-002','Thép Việt Nhật D12','CAT-002','Việt Nhật','cây','Thép vằn D12 dài 11.7m','ACTIVE',60,datetime('now'),datetime('now'),datetime('now'),'PENDING',1),
('PROD-005','GA-001','Gạch Đồng Tâm 40x40','CAT-003','Đồng Tâm','viên','Gạch lát nền 40x40cm','ACTIVE',80,datetime('now'),datetime('now'),datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO PriceList VALUES
('PL-001','Giá bán lẻ','Bảng giá bán lẻ tiêu chuẩn','ACTIVE',datetime('now'),datetime('now'),'PENDING',1),
('PL-002','Giá hội viên','Giá ưu đãi cho khách hàng thân thiết','ACTIVE',datetime('now'),datetime('now'),'PENDING',1),
('PL-003','Giá sỉ','Giá bán sỉ cho đại lý','INACTIVE',datetime('now'),datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO ProductVariant VALUES
('VAR-001','PROD-001','50kg/bao','8935001001001',1,datetime('now'),datetime('now'),'PENDING',1),
('VAR-002','PROD-001','Pallet 50 bao','8935001001002',1,datetime('now'),datetime('now'),'PENDING',1),
('VAR-003','PROD-002','50kg/bao','8935002001001',1,datetime('now'),datetime('now'),'PENDING',1),
('VAR-004','PROD-003','Cây 11.7m','8935003001001',1,datetime('now'),datetime('now'),'PENDING',1),
('VAR-005','PROD-004','Cây 11.7m','8935004001001',1,datetime('now'),datetime('now'),'PENDING',1),
('VAR-006','PROD-005','Thùng 16 viên','8935005001001',1,datetime('now'),datetime('now'),'PENDING',1),
('VAR-007','PROD-005','Viên lẻ','8935005001002',1,datetime('now'),datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO ProductPrice VALUES
('PRC-001','VAR-001','PL-001',95000.0,datetime('2024-01-01'),NULL,datetime('now'),datetime('now'),'PENDING',1),
('PRC-002','VAR-001','PL-002',90000.0,datetime('2024-01-01'),NULL,datetime('now'),datetime('now'),'PENDING',1),
('PRC-003','VAR-002','PL-001',4500000.0,datetime('2024-01-01'),NULL,datetime('now'),datetime('now'),'PENDING',1),
('PRC-004','VAR-003','PL-001',98000.0,datetime('2024-01-01'),NULL,datetime('now'),datetime('now'),'PENDING',1),
('PRC-005','VAR-004','PL-001',85000.0,datetime('2024-01-01'),NULL,datetime('now'),datetime('now'),'PENDING',1),
('PRC-006','VAR-005','PL-001',95000.0,datetime('2024-01-01'),NULL,datetime('now'),datetime('now'),'PENDING',1),
('PRC-007','VAR-006','PL-001',120000.0,datetime('2024-01-01'),NULL,datetime('now'),datetime('now'),'PENDING',1),
('PRC-008','VAR-007','PL-001',8000.0,datetime('2024-01-01'),NULL,datetime('now'),datetime('now'),'PENDING',1);

