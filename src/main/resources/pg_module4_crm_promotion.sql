-- ============================================================
-- MODULE 4: CRM & PROMOTION (PostgreSQL)
-- ============================================================

CREATE TABLE IF NOT EXISTS MembershipRank (
    "TierID"             TEXT PRIMARY KEY,
    "TierName"           TEXT NOT NULL UNIQUE,
    "MinPointsRequired"  INTEGER NOT NULL DEFAULT 0,
    "EarningMultipliers" NUMERIC NOT NULL DEFAULT 1.0,
    description          TEXT,
    last_modified        TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status          TEXT NOT NULL DEFAULT 'PENDING',
    version              INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS Customer (
    "CustomerID"       TEXT PRIMARY KEY,
    "PhoneNum"         TEXT UNIQUE,
    "FullName"         TEXT NOT NULL,
    "Email"            TEXT,
    "RegistrationDate" TEXT NOT NULL DEFAULT CURRENT_DATE,
    last_modified      TIMESTAMP NOT NULL DEFAULT NOW(),
    "SyncStatus"       TEXT NOT NULL DEFAULT 'PENDING',
    version            INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS LoyaltyAccount (
    "AccountID"         TEXT PRIMARY KEY,
    "CustomerID"        TEXT NOT NULL UNIQUE REFERENCES Customer("CustomerID") ON DELETE CASCADE,
    "TierID"            TEXT NOT NULL REFERENCES MembershipRank("TierID") ON DELETE RESTRICT,
    "CurrentPoints"     INTEGER NOT NULL DEFAULT 0,
    "TotalPointsEarned" INTEGER NOT NULL DEFAULT 0,
    "JoinedDate"        TEXT NOT NULL DEFAULT CURRENT_DATE,
    last_modified       TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING',
    version             INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS LoyaltyPointRule (
    "RuleID"            TEXT PRIMARY KEY,
    "RuleName"          TEXT,
    "EarnRate"          NUMERIC NOT NULL DEFAULT 1.0,
    "RedeemRate"        NUMERIC NOT NULL DEFAULT 1.0,
    "MaxRedeemPerOrder" INTEGER NOT NULL DEFAULT 0,
    is_active           INTEGER NOT NULL DEFAULT 1,
    last_modified       TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING',
    version             INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS PointTransaction (
    "TransactionID"    TEXT PRIMARY KEY,
    "AccountID"        TEXT REFERENCES LoyaltyAccount("AccountID") ON DELETE CASCADE,
    "RuleID"           TEXT REFERENCES LoyaltyPointRule("RuleID") ON DELETE SET NULL,
    "ReferenceOrderID" TEXT,
    "PointsAmount"     INTEGER NOT NULL DEFAULT 0,
    "TransactionType"  TEXT NOT NULL CHECK("TransactionType" IN ('EARN','REDEEM','ADJUST','EXPIRE')),
    "TimeStamp"        TIMESTAMP NOT NULL DEFAULT NOW(),
    note               TEXT,
    "SyncStatus"       TEXT NOT NULL DEFAULT 'PENDING',
    version            INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS Campaign (
    "CampaignID"  TEXT PRIMARY KEY,
    "Name"        TEXT NOT NULL,
    "Description" TEXT,
    "StartDate"   TEXT NOT NULL,
    "EndDate"     TEXT NOT NULL,
    "IsActive"    INTEGER NOT NULL DEFAULT 1,
    "storeId"     TEXT,
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS Promotion (
    "PromotionID"       TEXT PRIMARY KEY,
    "CampaignID"        TEXT NOT NULL REFERENCES Campaign("CampaignID") ON DELETE CASCADE,
    "PromoType"         TEXT NOT NULL CHECK("PromoType" IN ('PERCENT_DISCOUNT','FIXED_DISCOUNT','BUY_X_GET_Y','FREE_ITEM','BUNDLE')),
    "Name"              TEXT NOT NULL,
    "Priority"          INTEGER NOT NULL DEFAULT 0,
    "RuleDefinition"    TEXT,
    "VoucherCode"       TEXT,
    "MaxUsageCount"     INTEGER NOT NULL DEFAULT 0,
    "CurrentUsageCount" INTEGER NOT NULL DEFAULT 0,
    "ExpiryDate"        TEXT,
    "TriggerCondition"  TEXT,
    "IsActive"          INTEGER NOT NULL DEFAULT 1,
    last_modified       TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING',
    version             INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS PromotionApplicationLog (
    "LogID"                 TEXT PRIMARY KEY,
    "PromotionID"           TEXT REFERENCES Promotion("PromotionID") ON DELETE SET NULL,
    "ReferenceOrderID"      TEXT,
    "AppliedDiscountAmount" NUMERIC NOT NULL DEFAULT 0,
    "TimeStamp"             TIMESTAMP NOT NULL DEFAULT NOW(),
    "SyncStatus"            TEXT NOT NULL DEFAULT 'PENDING',
    version                 INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS OverrideLog (
    "OverrideID"          TEXT PRIMARY KEY,
    "LogID"               TEXT REFERENCES PromotionApplicationLog("LogID") ON DELETE SET NULL,
    "ApprovedByManagerID" TEXT,
    "Reason"              TEXT,
    "TimeStamp"           TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS PromotionProductPriceMapping (
    "MappingID"   TEXT PRIMARY KEY,
    "PromotionID" TEXT REFERENCES Promotion("PromotionID") ON DELETE CASCADE,
    variant_id    TEXT REFERENCES ProductVariant(variant_id) ON DELETE CASCADE,
    "MinQuantity" INTEGER NOT NULL DEFAULT 1,
    "IsExcluded"  INTEGER NOT NULL DEFAULT 0
);

