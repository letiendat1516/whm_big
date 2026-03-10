-- ============================================================
-- MODULE 3: PRODUCT (PostgreSQL)
-- ============================================================

CREATE TABLE IF NOT EXISTS ProductCategory (
    category_id   TEXT PRIMARY KEY,
    category_name TEXT NOT NULL,
    description   TEXT,
    status        INTEGER NOT NULL DEFAULT 1,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS Product (
    "ProductID"   TEXT PRIMARY KEY,
    product_code  TEXT NOT NULL UNIQUE,
    "Name"        TEXT NOT NULL,
    category_id   TEXT REFERENCES ProductCategory(category_id) ON DELETE SET NULL,
    brand         TEXT,
    unit          TEXT,
    "Description" TEXT,
    "Status"      TEXT NOT NULL DEFAULT 'ACTIVE' CHECK("Status" IN ('ACTIVE','INACTIVE','DISCONTINUED')),
    sales_rate    INTEGER DEFAULT 0,
    "CreatedAt"   TIMESTAMP NOT NULL DEFAULT NOW(),
    "UpdatedAt"   TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_product_category ON Product(category_id);
CREATE INDEX IF NOT EXISTS idx_product_code     ON Product(product_code);

CREATE TABLE IF NOT EXISTS ProductVariant (
    variant_id    TEXT PRIMARY KEY,
    product_id    TEXT NOT NULL REFERENCES Product("ProductID") ON DELETE CASCADE,
    variant_name  TEXT NOT NULL,
    barcode       TEXT UNIQUE,
    status        INTEGER NOT NULL DEFAULT 1,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_variant_product ON ProductVariant(product_id);
CREATE INDEX IF NOT EXISTS idx_variant_barcode ON ProductVariant(barcode);

CREATE TABLE IF NOT EXISTS PriceList (
    price_list_id   TEXT PRIMARY KEY,
    price_list_name TEXT NOT NULL,
    description     TEXT,
    status          TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE')),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified   TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status     TEXT NOT NULL DEFAULT 'PENDING',
    version         INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS ProductPrice (
    price_id      TEXT PRIMARY KEY,
    variant_id    TEXT NOT NULL REFERENCES ProductVariant(variant_id) ON DELETE CASCADE,
    price_list_id TEXT NOT NULL REFERENCES PriceList(price_list_id) ON DELETE RESTRICT,
    price         NUMERIC NOT NULL CHECK(price >= 0),
    start_time    TIMESTAMP NOT NULL,
    end_time      TIMESTAMP,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_price_variant   ON ProductPrice(variant_id);
CREATE INDEX IF NOT EXISTS idx_price_pricelist ON ProductPrice(price_list_id);

-- View: active prices
DROP VIEW IF EXISTS v_active_price;
CREATE VIEW v_active_price AS
SELECT pp.price_id, pp.variant_id, pv.variant_name, pv.barcode,
       p."ProductID", p."Name" AS product_name, p.product_code,
       pl.price_list_id, pl.price_list_name, pp.price, pp.start_time, pp.end_time
FROM ProductPrice pp
JOIN ProductVariant pv ON pv.variant_id = pp.variant_id
JOIN Product p ON p."ProductID" = pv.product_id
JOIN PriceList pl ON pl.price_list_id = pp.price_list_id
WHERE pp.start_time <= NOW()
  AND (pp.end_time IS NULL OR pp.end_time > NOW())
  AND pv.status = 1 AND p."Status" = 'ACTIVE' AND pl.status = 'ACTIVE';

