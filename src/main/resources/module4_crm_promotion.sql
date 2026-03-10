-- ============================================================
-- MODULE 4: CRM & PROMOTION (Khách hàng & Khuyến mãi)
-- Load Order: 5th (after module1_pos for Order reference)
-- ============================================================
-- NOTE: PRAGMA foreign_keys is managed by Main.java (OFF during load, ON at runtime)
PRAGMA journal_mode = WAL;

-- ------------------------------------------------------------
-- MembershipRank (hạng thành viên)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS MembershipRank (
    TierID             TEXT PRIMARY KEY,
    TierName           TEXT NOT NULL UNIQUE,
    MinPointsRequired  INTEGER NOT NULL DEFAULT 0,
    EarningMultipliers REAL NOT NULL DEFAULT 1.0,
    description        TEXT,
    last_modified      TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status        TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version            INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_tier_minpoints ON MembershipRank(MinPointsRequired);

CREATE TRIGGER IF NOT EXISTS trg_tier_updated
    AFTER UPDATE ON MembershipRank
    FOR EACH ROW
BEGIN
    UPDATE MembershipRank SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE TierID = OLD.TierID;
END;

-- ------------------------------------------------------------
-- Customer (khách hàng)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Customer (
    CustomerID       TEXT PRIMARY KEY,
    PhoneNum         TEXT UNIQUE,
    FullName         TEXT NOT NULL,
    Email            TEXT,
    RegistrationDate TEXT NOT NULL DEFAULT (date('now')),
    last_modified    TEXT NOT NULL DEFAULT (datetime('now')),
    SyncStatus       TEXT NOT NULL DEFAULT 'PENDING' CHECK(SyncStatus IN ('PENDING','SYNCED','CONFLICT')),
    version          INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_customer_phone ON Customer(PhoneNum);
CREATE INDEX IF NOT EXISTS idx_customer_name  ON Customer(FullName);

CREATE TRIGGER IF NOT EXISTS trg_customer_updated
    AFTER UPDATE ON Customer
    FOR EACH ROW
BEGIN
    UPDATE Customer SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE CustomerID = OLD.CustomerID;
END;

-- ------------------------------------------------------------
-- LoyaltyAccount (tài khoản tích điểm)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS LoyaltyAccount (
    AccountID          TEXT PRIMARY KEY,
    CustomerID         TEXT NOT NULL UNIQUE REFERENCES Customer(CustomerID) ON DELETE CASCADE,
    TierID             TEXT NOT NULL REFERENCES MembershipRank(TierID) ON DELETE RESTRICT,
    CurrentPoints      INTEGER NOT NULL DEFAULT 0,
    TotalPointsEarned  INTEGER NOT NULL DEFAULT 0,
    JoinedDate         TEXT NOT NULL DEFAULT (date('now')),
    last_modified      TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status        TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version            INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_loyalty_customer ON LoyaltyAccount(CustomerID);
CREATE INDEX IF NOT EXISTS idx_loyalty_tier     ON LoyaltyAccount(TierID);

CREATE TRIGGER IF NOT EXISTS trg_loyalty_updated
    AFTER UPDATE ON LoyaltyAccount
    FOR EACH ROW
BEGIN
    UPDATE LoyaltyAccount SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE AccountID = OLD.AccountID;
END;

-- ------------------------------------------------------------
-- LoyaltyPointRule (quy tắc tích điểm)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS LoyaltyPointRule (
    RuleID              TEXT PRIMARY KEY,
    RuleName            TEXT NOT NULL,
    EarnRate            REAL NOT NULL DEFAULT 1.0,   -- points per VND (e.g., 1 point per 10,000 VND)
    RedeemRate          REAL NOT NULL DEFAULT 1.0,   -- VND per point
    MaxRedeemPerOrder   INTEGER NOT NULL DEFAULT 0,  -- 0 = no limit
    is_active           INTEGER NOT NULL DEFAULT 1,
    last_modified       TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version             INTEGER NOT NULL DEFAULT 1
);

CREATE TRIGGER IF NOT EXISTS trg_rule_updated
    AFTER UPDATE ON LoyaltyPointRule
    FOR EACH ROW
BEGIN
    UPDATE LoyaltyPointRule SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE RuleID = OLD.RuleID;
END;

-- ------------------------------------------------------------
-- PointTransaction (lịch sử giao dịch điểm)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS PointTransaction (
    TransactionID    TEXT PRIMARY KEY,
    AccountID        TEXT NOT NULL REFERENCES LoyaltyAccount(AccountID) ON DELETE CASCADE,
    RuleID           TEXT REFERENCES LoyaltyPointRule(RuleID) ON DELETE SET NULL,
    ReferenceOrderID TEXT,  -- logical FK → "Order"(orderId), enforced at app level
    PointsAmount     INTEGER NOT NULL DEFAULT 0,
    TransactionType  TEXT NOT NULL CHECK(TransactionType IN ('EARN','REDEEM','ADJUST','EXPIRE','BONUS')),
    TimeStamp        TEXT NOT NULL DEFAULT (datetime('now')),
    note             TEXT,
    last_modified    TEXT NOT NULL DEFAULT (datetime('now')),
    SyncStatus       TEXT NOT NULL DEFAULT 'PENDING' CHECK(SyncStatus IN ('PENDING','SYNCED','CONFLICT')),
    version          INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_pointtxn_account   ON PointTransaction(AccountID);
CREATE INDEX IF NOT EXISTS idx_pointtxn_order     ON PointTransaction(ReferenceOrderID);
CREATE INDEX IF NOT EXISTS idx_pointtxn_type      ON PointTransaction(TransactionType);
CREATE INDEX IF NOT EXISTS idx_pointtxn_timestamp ON PointTransaction(TimeStamp);

CREATE TRIGGER IF NOT EXISTS trg_pointtxn_updated
    AFTER UPDATE ON PointTransaction
    FOR EACH ROW
BEGIN
    UPDATE PointTransaction SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE TransactionID = OLD.TransactionID;
END;

-- ------------------------------------------------------------
-- Campaign (chiến dịch khuyến mãi)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Campaign (
    CampaignID    TEXT PRIMARY KEY,
    Name          TEXT NOT NULL,
    Description   TEXT,
    StartDate     TEXT NOT NULL,
    EndDate       TEXT NOT NULL,
    IsActive      INTEGER NOT NULL DEFAULT 1,
    storeId       TEXT REFERENCES Store(storeId) ON DELETE SET NULL,  -- NULL = all stores
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_campaign_dates  ON Campaign(StartDate, EndDate);
CREATE INDEX IF NOT EXISTS idx_campaign_active ON Campaign(IsActive);

CREATE TRIGGER IF NOT EXISTS trg_campaign_updated
    AFTER UPDATE ON Campaign
    FOR EACH ROW
BEGIN
    UPDATE Campaign SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE CampaignID = OLD.CampaignID;
END;

-- ------------------------------------------------------------
-- Promotion (khuyến mãi chi tiết)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Promotion (
    PromotionID        TEXT PRIMARY KEY,
    CampaignID         TEXT NOT NULL REFERENCES Campaign(CampaignID) ON DELETE CASCADE,
    PromoType          TEXT NOT NULL CHECK(PromoType IN ('PERCENT_DISCOUNT','FIXED_DISCOUNT','BUY_X_GET_Y','FREE_GIFT','VOUCHER','POINTS_MULTIPLIER')),
    Name               TEXT NOT NULL,
    Priority           INTEGER NOT NULL DEFAULT 0,    -- higher = applied first
    RuleDefinition     TEXT,                          -- JSON rule definition
    VoucherCode        TEXT UNIQUE,
    MaxUsageCount      INTEGER DEFAULT 0,             -- 0 = unlimited
    CurrentUsageCount  INTEGER NOT NULL DEFAULT 0,
    ExpiryDate         TEXT,
    TriggerCondition   TEXT,                          -- JSON condition
    IsActive           INTEGER NOT NULL DEFAULT 1,
    last_modified      TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status        TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version            INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_promo_campaign    ON Promotion(CampaignID);
CREATE INDEX IF NOT EXISTS idx_promo_voucher     ON Promotion(VoucherCode);
CREATE INDEX IF NOT EXISTS idx_promo_active      ON Promotion(IsActive);
CREATE INDEX IF NOT EXISTS idx_promo_priority    ON Promotion(Priority DESC);

CREATE TRIGGER IF NOT EXISTS trg_promotion_updated
    AFTER UPDATE ON Promotion
    FOR EACH ROW
BEGIN
    UPDATE Promotion SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE PromotionID = OLD.PromotionID;
END;


-- ------------------------------------------------------------
-- PromotionApplicationLog (log áp dụng khuyến mãi)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS PromotionApplicationLog (
    LogID                TEXT PRIMARY KEY,
    PromotionID          TEXT NOT NULL REFERENCES Promotion(PromotionID) ON DELETE CASCADE,
    ReferenceOrderID     TEXT,  -- logical FK → "Order"(orderId), enforced at app level
    AppliedDiscountAmount REAL NOT NULL DEFAULT 0,
    TimeStamp            TEXT NOT NULL DEFAULT (datetime('now')),
    last_modified        TEXT NOT NULL DEFAULT (datetime('now')),
    SyncStatus           TEXT NOT NULL DEFAULT 'PENDING' CHECK(SyncStatus IN ('PENDING','SYNCED','CONFLICT')),
    version              INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_promolog_promotion ON PromotionApplicationLog(PromotionID);
CREATE INDEX IF NOT EXISTS idx_promolog_order     ON PromotionApplicationLog(ReferenceOrderID);
CREATE INDEX IF NOT EXISTS idx_promolog_timestamp ON PromotionApplicationLog(TimeStamp);

-- Auto-increment CurrentUsageCount on application log insert
CREATE TRIGGER IF NOT EXISTS trg_promo_usage_count
    AFTER INSERT ON PromotionApplicationLog
    FOR EACH ROW
BEGIN
    UPDATE Promotion SET CurrentUsageCount = CurrentUsageCount + 1
    WHERE PromotionID = NEW.PromotionID;
END;

CREATE TRIGGER IF NOT EXISTS trg_promolog_updated
    AFTER UPDATE ON PromotionApplicationLog
    FOR EACH ROW
BEGIN
    UPDATE PromotionApplicationLog SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE LogID = OLD.LogID;
END;

-- ------------------------------------------------------------
-- OverrideLog (log ghi đè / phê duyệt đặc biệt)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS OverrideLog (
    OverrideID           TEXT PRIMARY KEY,
    LogID                TEXT NOT NULL REFERENCES PromotionApplicationLog(LogID) ON DELETE CASCADE,
    ApprovedByManagerID  TEXT REFERENCES StoreManager(managerId) ON DELETE SET NULL,
    Reason               TEXT,
    TimeStamp            TEXT NOT NULL DEFAULT (datetime('now')),
    last_modified        TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status          TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version              INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_override_log     ON OverrideLog(LogID);
CREATE INDEX IF NOT EXISTS idx_override_manager ON OverrideLog(ApprovedByManagerID);

CREATE TRIGGER IF NOT EXISTS trg_override_updated
    AFTER UPDATE ON OverrideLog
    FOR EACH ROW
BEGIN
    UPDATE OverrideLog SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE OverrideID = OLD.OverrideID;
END;

-- ------------------------------------------------------------
-- PromotionProductPriceMapping (sản phẩm áp dụng / loại trừ khỏi KM)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS PromotionProductPriceMapping (
    MappingID   TEXT PRIMARY KEY,
    PromotionID TEXT NOT NULL REFERENCES Promotion(PromotionID) ON DELETE CASCADE,
    variant_id  TEXT REFERENCES ProductVariant(variant_id) ON DELETE CASCADE,
    product_id  TEXT REFERENCES Product(ProductID) ON DELETE CASCADE,
    MinQuantity INTEGER NOT NULL DEFAULT 1,
    IsExcluded  INTEGER NOT NULL DEFAULT 0   -- 0=included, 1=excluded
);

CREATE INDEX IF NOT EXISTS idx_promapping_promo   ON PromotionProductPriceMapping(PromotionID);
CREATE INDEX IF NOT EXISTS idx_promapping_variant ON PromotionProductPriceMapping(variant_id);
CREATE INDEX IF NOT EXISTS idx_promapping_product ON PromotionProductPriceMapping(product_id);

-- ------------------------------------------------------------
-- VIEW: v_customer_loyalty
-- ------------------------------------------------------------
CREATE VIEW IF NOT EXISTS v_customer_loyalty AS
SELECT
    c.CustomerID,
    c.FullName,
    c.PhoneNum,
    c.Email,
    c.RegistrationDate,
    la.AccountID,
    la.CurrentPoints,
    la.TotalPointsEarned,
    la.JoinedDate,
    mr.TierID,
    mr.TierName,
    mr.EarningMultipliers,
    mr.MinPointsRequired
FROM Customer     c
JOIN LoyaltyAccount la ON la.CustomerID = c.CustomerID
JOIN MembershipRank mr ON mr.TierID     = la.TierID;

-- ------------------------------------------------------------
-- VIEW: v_active_promotions
-- ------------------------------------------------------------
CREATE VIEW IF NOT EXISTS v_active_promotions AS
SELECT
    p.PromotionID,
    p.Name         AS promo_name,
    p.PromoType,
    p.Priority,
    p.VoucherCode,
    p.MaxUsageCount,
    p.CurrentUsageCount,
    p.ExpiryDate,
    p.RuleDefinition,
    p.TriggerCondition,
    c.CampaignID,
    c.Name         AS campaign_name,
    c.StartDate,
    c.EndDate,
    c.storeId
FROM Promotion p
JOIN Campaign  c ON c.CampaignID = p.CampaignID
WHERE p.IsActive = 1
  AND c.IsActive = 1
  AND c.StartDate <= date('now')
  AND c.EndDate   >= date('now')
  AND (p.MaxUsageCount = 0 OR p.CurrentUsageCount < p.MaxUsageCount)
  AND (p.ExpiryDate IS NULL OR p.ExpiryDate >= date('now'))
ORDER BY p.Priority DESC;

-- ------------------------------------------------------------
-- VIEW: v_customer_points_history
-- ------------------------------------------------------------
CREATE VIEW IF NOT EXISTS v_customer_points_history AS
SELECT
    c.CustomerID,
    c.FullName,
    c.PhoneNum,
    pt.TransactionID,
    pt.TransactionType,
    pt.PointsAmount,
    pt.TimeStamp,
    pt.ReferenceOrderID,
    pt.note,
    la.CurrentPoints
FROM PointTransaction pt
JOIN LoyaltyAccount la ON la.AccountID  = pt.AccountID
JOIN Customer       c  ON c.CustomerID  = la.CustomerID
ORDER BY pt.TimeStamp DESC;

-- ------------------------------------------------------------
-- SEED DATA
-- ------------------------------------------------------------
INSERT OR IGNORE INTO MembershipRank VALUES
('TIER-001','Bronze',0,1.0,'Entry level membership',datetime('now'),'PENDING',1),
('TIER-002','Silver',1000,1.5,'Mid-level membership',datetime('now'),'PENDING',1),
('TIER-003','Gold',5000,2.0,'Premium membership',datetime('now'),'PENDING',1),
('TIER-004','Platinum',15000,3.0,'Elite membership',datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO Customer VALUES
('CUST-001','0901111111','Nguyen Thi Lan','lan.nguyen@email.com','2024-06-01',datetime('now'),'SYNCED',1),
('CUST-002','0902222222','Tran Van Minh','minh.tran@email.com','2024-08-15',datetime('now'),'SYNCED',1),
('CUST-003','0903333333','Le Thi Hoa','hoa.le@email.com','2025-01-10',datetime('now'),'SYNCED',1),
('CUST-004','0904444444','Pham Van Duc',NULL,'2025-03-20',datetime('now'),'PENDING',1),
('CUST-005','0905555555','Hoang Thi Mai','mai.hoang@email.com','2023-11-05',datetime('now'),'SYNCED',1);

INSERT OR IGNORE INTO LoyaltyAccount VALUES
('ACC-001','CUST-001','TIER-002',750,1500,'2024-06-01',datetime('now'),'SYNCED',1),
('ACC-002','CUST-002','TIER-001',200,200,'2024-08-15',datetime('now'),'SYNCED',1),
('ACC-003','CUST-003','TIER-001',50,50,'2025-01-10',datetime('now'),'SYNCED',1),
('ACC-004','CUST-004','TIER-001',0,0,'2025-03-20',datetime('now'),'PENDING',1),
('ACC-005','CUST-005','TIER-003',6200,12500,'2023-11-05',datetime('now'),'SYNCED',1);

INSERT OR IGNORE INTO LoyaltyPointRule VALUES
('RULE-001','Standard Earn & Redeem',1.0,100.0,500,1,datetime('now'),'PENDING',1),
('RULE-002','Double Points Weekend',2.0,100.0,1000,1,datetime('now'),'PENDING',1),
('RULE-003','Member Redeem',1.0,200.0,2000,1,datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO Campaign VALUES
('CAMP-001','Spring Sale 2026','Discounts for spring season','2026-03-01','2026-03-31',1,NULL,datetime('now'),'PENDING',1),
('CAMP-002','Member Appreciation Day','Special deals for members','2026-03-07','2026-03-07',1,NULL,datetime('now'),'PENDING',1),
('CAMP-003','New Product Launch','Launch promotion for new items','2026-02-15','2026-03-15',1,'STORE-001',datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO Promotion VALUES
('PROMO-001','CAMP-001','PERCENT_DISCOUNT','10% Off All Beverages',10,'{"categories":["CAT-001"],"discount_pct":10}',NULL,0,0,'2026-03-31','{"min_amount":50000}',1,datetime('now'),'PENDING',1),
('PROMO-002','CAMP-001','FIXED_DISCOUNT','5,000 VND Off Orders Over 100K',5,'{"discount_amount":5000}','SPRING5K',1000,0,'2026-03-31','{"min_amount":100000}',1,datetime('now'),'PENDING',1),
('PROMO-003','CAMP-002','POINTS_MULTIPLIER','Double Points for Members',20,'{"multiplier":2}',NULL,0,0,'2026-03-07','{"member_only":true}',1,datetime('now'),'PENDING',1),
('PROMO-004','CAMP-003','VOUCHER','New Product Free Sample',1,'{"free_product":"PROD-001","qty":1}','NEWPROD2026',500,0,'2026-03-15',NULL,1,datetime('now'),'PENDING',1);

-- NOTE: PromotionApplicationLog and PointTransaction seeds that reference
-- "Order" rows are placed in module1_pos.sql (which loads after this file)

INSERT OR IGNORE INTO PromotionProductPriceMapping VALUES
('MAP-001','PROMO-001','VAR-001','PROD-001',1,0),
('MAP-002','PROMO-001','VAR-002','PROD-001',1,0),
('MAP-003','PROMO-001','VAR-003','PROD-002',1,0),
('MAP-004','PROMO-004','VAR-001','PROD-001',1,0);

