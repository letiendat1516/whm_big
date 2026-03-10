-- ============================================================
-- MODULE 0: Authentication & RBAC
-- Load Order: FIRST (before all other modules)
-- ============================================================

-- ── Roles ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Role (
    roleId      TEXT PRIMARY KEY,
    roleName    TEXT NOT NULL UNIQUE,  -- ADMIN, CASHIER, WAREHOUSE, HR_MANAGER, STORE_MANAGER, VIEWER
    description TEXT,
    permissions TEXT NOT NULL DEFAULT '[]'  -- JSON array: ["POS","INVENTORY","PRODUCT","CRM","HR","PAYMENT"]
);

-- ── Accounts ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Account (
    accountId       TEXT PRIMARY KEY,
    username        TEXT NOT NULL UNIQUE COLLATE NOCASE,
    passwordHash    TEXT NOT NULL,   -- BCrypt hash
    roleId          TEXT NOT NULL REFERENCES Role(roleId) ON DELETE RESTRICT,
    employeeId      TEXT,            -- optional link to Employee
    storeId         TEXT,            -- null = HQ / all stores
    fullName        TEXT NOT NULL,
    isActive        INTEGER NOT NULL DEFAULT 1 CHECK(isActive IN (0,1)),
    lastLoginAt     TEXT,
    createdAt       TEXT NOT NULL DEFAULT (datetime('now')),
    updatedAt       TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_account_username ON Account(username);
CREATE INDEX IF NOT EXISTS idx_account_role     ON Account(roleId);

-- ── Login Audit Log ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS LoginLog (
    logId       TEXT PRIMARY KEY,
    accountId   TEXT NOT NULL REFERENCES Account(accountId) ON DELETE CASCADE,
    loginAt     TEXT NOT NULL DEFAULT (datetime('now')),
    logoutAt    TEXT,
    ipAddress   TEXT,
    success     INTEGER NOT NULL DEFAULT 1 CHECK(success IN (0,1)),
    note        TEXT
);

-- ── Session (current login state) ────────────────────────────
CREATE TABLE IF NOT EXISTS AppSession (
    sessionKey  TEXT PRIMARY KEY DEFAULT 'current',
    accountId   TEXT REFERENCES Account(accountId) ON DELETE SET NULL,
    loginAt     TEXT,
    expiresAt   TEXT
);

-- ── Seed default roles ───────────────────────────────────────
INSERT OR IGNORE INTO Role(roleId, roleName, description, permissions) VALUES
('ROLE-ADMIN',   'ADMIN',         'Toàn quyền hệ thống',              '["POS","PAYMENT","INVENTORY","PRODUCT","CRM","HR"]'),
('ROLE-MGR',     'STORE_MANAGER', 'Quản lý cửa hàng',                 '["POS","PAYMENT","INVENTORY","PRODUCT","CRM"]'),
('ROLE-CASH',    'CASHIER',       'Thu ngân - chỉ bán hàng',          '["POS","PAYMENT"]'),
('ROLE-WH',      'WAREHOUSE',     'Quản lý kho - chỉ kho hàng',       '["INVENTORY"]'),
('ROLE-HR',      'HR_MANAGER',    'Quản lý nhân sự',                  '["HR"]'),
('ROLE-PROD',    'PRODUCT_MGR',   'Quản lý sản phẩm & giá',           '["PRODUCT","CRM"]'),
('ROLE-VIEW',    'VIEWER',        'Chỉ xem báo cáo',                  '[]');

-- ── Seed default admin account (password set by Java at startup) ────────
INSERT OR IGNORE INTO Account(accountId, username, passwordHash, roleId, fullName, isActive) VALUES
('ACC-ADMIN-001', 'admin', 'NEEDS_HASH', 'ROLE-ADMIN', 'Administrator', 1);

-- Seed demo accounts (password set by Java at startup)
INSERT OR IGNORE INTO Account(accountId, username, passwordHash, roleId, fullName, isActive) VALUES
('ACC-CASH-001',  'cashier1',  'NEEDS_HASH', 'ROLE-CASH',  'Nguyễn Thu Ngân',  1),
('ACC-WH-001',    'warehouse1','NEEDS_HASH', 'ROLE-WH',    'Trần Quản Kho',    1),
('ACC-HR-001',    'hr1',       'NEEDS_HASH', 'ROLE-HR',    'Lê Nhân Sự',       1),
('ACC-MGR-001',   'manager1',  'NEEDS_HASH', 'ROLE-MGR',   'Phạm Quản Lý',     1);

-- Init session slot
INSERT OR IGNORE INTO AppSession(sessionKey) VALUES('current');

