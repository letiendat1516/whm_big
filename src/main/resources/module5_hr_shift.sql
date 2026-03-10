-- ============================================================
-- MODULE 5: HR & SHIFT (Nhân sự & Ca làm việc)
-- Load Order: 2nd (Employee referenced by many modules)
-- ============================================================
-- NOTE: PRAGMA foreign_keys is managed by Main.java (OFF during load, ON at runtime)
PRAGMA journal_mode = WAL;

-- ------------------------------------------------------------
-- Store
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Store (
    storeId       TEXT PRIMARY KEY,
    storeCode     TEXT NOT NULL UNIQUE,
    name          TEXT NOT NULL,
    address       TEXT,
    status        TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE','CLOSED')),
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_store_code   ON Store(storeCode);
CREATE INDEX IF NOT EXISTS idx_store_status ON Store(status);

CREATE TRIGGER IF NOT EXISTS trg_store_updated
    AFTER UPDATE ON Store
    FOR EACH ROW
BEGIN
    UPDATE Store SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE storeId = OLD.storeId;
END;

-- ------------------------------------------------------------
-- Employee
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Employee (
    employeeId     TEXT PRIMARY KEY,
    employeeCode   TEXT NOT NULL UNIQUE,
    fullName       TEXT NOT NULL,
    hireDate       TEXT NOT NULL,
    baseSalary     REAL NOT NULL DEFAULT 0,
    status         TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE','TERMINATED','ON_LEAVE')),
    lastModifiedAt TEXT NOT NULL DEFAULT (datetime('now')),
    last_modified  TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status    TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version        INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_employee_code   ON Employee(employeeCode);
CREATE INDEX IF NOT EXISTS idx_employee_status ON Employee(status);

CREATE TRIGGER IF NOT EXISTS trg_employee_updated
    AFTER UPDATE ON Employee
    FOR EACH ROW
BEGIN
    UPDATE Employee SET lastModifiedAt = datetime('now'), last_modified = datetime('now'), version = OLD.version + 1
    WHERE employeeId = OLD.employeeId;
END;

-- ------------------------------------------------------------
-- EmployeeAssignment (employee -> store mapping)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS EmployeeAssignment (
    assignmentId  TEXT PRIMARY KEY,
    employeeId    TEXT NOT NULL REFERENCES Employee(employeeId) ON DELETE CASCADE,
    storeId       TEXT NOT NULL REFERENCES Store(storeId) ON DELETE CASCADE,
    startDate     TEXT NOT NULL,
    endDate       TEXT,
    status        TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','ENDED','TRANSFERRED')),
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_assignment_employee ON EmployeeAssignment(employeeId);
CREATE INDEX IF NOT EXISTS idx_assignment_store    ON EmployeeAssignment(storeId);
CREATE INDEX IF NOT EXISTS idx_assignment_status   ON EmployeeAssignment(status);

CREATE TRIGGER IF NOT EXISTS trg_assignment_updated
    AFTER UPDATE ON EmployeeAssignment
    FOR EACH ROW
BEGIN
    UPDATE EmployeeAssignment SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE assignmentId = OLD.assignmentId;
END;

-- ------------------------------------------------------------
-- StoreManager
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS StoreManager (
    managerId     TEXT PRIMARY KEY,
    employeeId    TEXT NOT NULL UNIQUE REFERENCES Employee(employeeId) ON DELETE CASCADE,
    storeId       TEXT REFERENCES Store(storeId) ON DELETE SET NULL,
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_manager_employee ON StoreManager(employeeId);
CREATE INDEX IF NOT EXISTS idx_manager_store    ON StoreManager(storeId);

CREATE TRIGGER IF NOT EXISTS trg_manager_updated
    AFTER UPDATE ON StoreManager
    FOR EACH ROW
BEGIN
    UPDATE StoreManager SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE managerId = OLD.managerId;
END;

-- ------------------------------------------------------------
-- HR (HR record per employee)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS HR (
    hrId          TEXT PRIMARY KEY,
    employeeId    TEXT NOT NULL REFERENCES Employee(employeeId) ON DELETE CASCADE,
    startDate     TEXT NOT NULL,
    endDate       TEXT,
    status        TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','ENDED','SUSPENDED')),
    last_modified TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_hr_employee ON HR(employeeId);

CREATE TRIGGER IF NOT EXISTS trg_hr_updated
    AFTER UPDATE ON HR
    FOR EACH ROW
BEGIN
    UPDATE HR SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE hrId = OLD.hrId;
END;

-- ------------------------------------------------------------
-- ShiftTemplate (ca làm việc mẫu)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ShiftTemplate (
    shiftTemplateId TEXT PRIMARY KEY,
    name            TEXT NOT NULL,
    startTime       TEXT NOT NULL,   -- HH:MM format
    endTime         TEXT NOT NULL,
    breakMinutes    INTEGER NOT NULL DEFAULT 0,
    status          TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE')),
    last_modified   TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status     TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version         INTEGER NOT NULL DEFAULT 1
);

CREATE TRIGGER IF NOT EXISTS trg_shifttemplate_updated
    AFTER UPDATE ON ShiftTemplate
    FOR EACH ROW
BEGIN
    UPDATE ShiftTemplate SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE shiftTemplateId = OLD.shiftTemplateId;
END;

-- ------------------------------------------------------------
-- ShiftAssignment (phân ca cho nhân viên)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ShiftAssignment (
    shiftAssignmentId TEXT PRIMARY KEY,
    employeeId        TEXT NOT NULL REFERENCES Employee(employeeId) ON DELETE CASCADE,
    shiftTemplateId   TEXT NOT NULL REFERENCES ShiftTemplate(shiftTemplateId) ON DELETE RESTRICT,
    storeId           TEXT NOT NULL REFERENCES Store(storeId) ON DELETE CASCADE,
    workDate          TEXT NOT NULL,
    status            TEXT NOT NULL DEFAULT 'SCHEDULED' CHECK(status IN ('SCHEDULED','CONFIRMED','COMPLETED','ABSENT','CANCELLED')),
    lastModifiedAt    TEXT NOT NULL DEFAULT (datetime('now')),
    last_modified     TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status       TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version           INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_shiftassign_employee  ON ShiftAssignment(employeeId);
CREATE INDEX IF NOT EXISTS idx_shiftassign_store     ON ShiftAssignment(storeId);
CREATE INDEX IF NOT EXISTS idx_shiftassign_workdate  ON ShiftAssignment(workDate);
CREATE INDEX IF NOT EXISTS idx_shiftassign_template  ON ShiftAssignment(shiftTemplateId);

CREATE TRIGGER IF NOT EXISTS trg_shiftassign_updated
    AFTER UPDATE ON ShiftAssignment
    FOR EACH ROW
BEGIN
    UPDATE ShiftAssignment
    SET lastModifiedAt = datetime('now'), last_modified = datetime('now'), version = OLD.version + 1
    WHERE shiftAssignmentId = OLD.shiftAssignmentId;
END;

-- ------------------------------------------------------------
-- AttendanceRecord (chấm công)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS AttendanceRecord (
    attendanceId      TEXT PRIMARY KEY,
    shiftAssignmentId TEXT NOT NULL REFERENCES ShiftAssignment(shiftAssignmentId) ON DELETE CASCADE,
    checkInTime       TEXT,
    checkOutTime      TEXT,
    approvedBy        TEXT REFERENCES Employee(employeeId) ON DELETE SET NULL,
    status            TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','APPROVED','REJECTED','ABSENT')),
    last_modified     TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status       TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version           INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_attendance_shift    ON AttendanceRecord(shiftAssignmentId);
CREATE INDEX IF NOT EXISTS idx_attendance_status   ON AttendanceRecord(status);

CREATE TRIGGER IF NOT EXISTS trg_attendance_updated
    AFTER UPDATE ON AttendanceRecord
    FOR EACH ROW
BEGIN
    UPDATE AttendanceRecord SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE attendanceId = OLD.attendanceId;
END;

-- ------------------------------------------------------------
-- PayrollPeriod (kỳ lương)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS PayrollPeriod (
    payrollPeriodId TEXT PRIMARY KEY,
    startDate       TEXT NOT NULL,
    endDate         TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'OPEN' CHECK(status IN ('OPEN','PROCESSING','LOCKED','PAID')),
    lockedAt        TEXT,
    last_modified   TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status     TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version         INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_payroll_period_dates  ON PayrollPeriod(startDate, endDate);
CREATE INDEX IF NOT EXISTS idx_payroll_period_status ON PayrollPeriod(status);

CREATE TRIGGER IF NOT EXISTS trg_payrollperiod_updated
    AFTER UPDATE ON PayrollPeriod
    FOR EACH ROW
BEGIN
    UPDATE PayrollPeriod SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE payrollPeriodId = OLD.payrollPeriodId;
END;

-- ------------------------------------------------------------
-- PayrollSnapshot (bảng lương đã khóa - immutable after lock)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS PayrollSnapshot (
    snapshotId       TEXT PRIMARY KEY,
    employeeId       TEXT NOT NULL REFERENCES Employee(employeeId) ON DELETE RESTRICT,
    payrollPeriodId  TEXT NOT NULL REFERENCES PayrollPeriod(payrollPeriodId) ON DELETE RESTRICT,
    salaryAmount     REAL NOT NULL DEFAULT 0,
    calculationRule  TEXT,            -- JSON or description of calculation logic
    createdAt        TEXT NOT NULL DEFAULT (datetime('now')),
    locked           INTEGER NOT NULL DEFAULT 0,  -- 0=editable, 1=locked/immutable
    last_modified    TEXT NOT NULL DEFAULT (datetime('now')),
    sync_status      TEXT NOT NULL DEFAULT 'PENDING' CHECK(sync_status IN ('PENDING','SYNCED','CONFLICT')),
    version          INTEGER NOT NULL DEFAULT 1,
    UNIQUE(employeeId, payrollPeriodId)
);

CREATE INDEX IF NOT EXISTS idx_snapshot_employee ON PayrollSnapshot(employeeId);
CREATE INDEX IF NOT EXISTS idx_snapshot_period   ON PayrollSnapshot(payrollPeriodId);

CREATE TRIGGER IF NOT EXISTS trg_snapshot_updated
    AFTER UPDATE ON PayrollSnapshot
    FOR EACH ROW
    WHEN OLD.locked = 0  -- prevent update if already locked
BEGIN
    UPDATE PayrollSnapshot SET last_modified = datetime('now'), version = OLD.version + 1
    WHERE snapshotId = OLD.snapshotId;
END;

-- ------------------------------------------------------------
-- VIEW: v_shift_attendance
-- ------------------------------------------------------------
CREATE VIEW IF NOT EXISTS v_shift_attendance AS
SELECT
    sa.shiftAssignmentId,
    sa.workDate,
    e.employeeId,
    e.fullName,
    e.employeeCode,
    st.shiftTemplateId,
    st.name AS shift_name,
    st.startTime,
    st.endTime,
    s.storeId,
    s.name AS store_name,
    ar.attendanceId,
    ar.checkInTime,
    ar.checkOutTime,
    ar.status AS attendance_status,
    sa.status AS shift_status
FROM ShiftAssignment sa
JOIN Employee      e  ON e.employeeId      = sa.employeeId
JOIN ShiftTemplate st ON st.shiftTemplateId = sa.shiftTemplateId
JOIN Store         s  ON s.storeId          = sa.storeId
LEFT JOIN AttendanceRecord ar ON ar.shiftAssignmentId = sa.shiftAssignmentId;

-- ------------------------------------------------------------
-- VIEW: v_payroll_summary
-- ------------------------------------------------------------
CREATE VIEW IF NOT EXISTS v_payroll_summary AS
SELECT
    ps.snapshotId,
    e.employeeId,
    e.fullName,
    e.employeeCode,
    pp.payrollPeriodId,
    pp.startDate,
    pp.endDate,
    pp.status AS period_status,
    ps.salaryAmount,
    ps.locked,
    ps.createdAt
FROM PayrollSnapshot ps
JOIN Employee      e  ON e.employeeId      = ps.employeeId
JOIN PayrollPeriod pp ON pp.payrollPeriodId = ps.payrollPeriodId;

-- ------------------------------------------------------------
-- SEED DATA
-- ------------------------------------------------------------
INSERT OR IGNORE INTO Store VALUES
('STORE-001','S001','Cửa hàng VLXD Trung Tâm','123 Nguyễn Trãi, Q.1','ACTIVE',datetime('now'),'PENDING',1),
('STORE-002','S002','Chi nhánh Quận 7','45 Nguyễn Thị Thập, Q.7','ACTIVE',datetime('now'),'PENDING',1),
('STORE-003','S003','Chi nhánh Thủ Đức','789 Xa lộ Hà Nội, TP.Thủ Đức','ACTIVE',datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO Employee VALUES
('EMP-001','NV001','Nguyễn Văn An','2024-01-15',8000000.0,'ACTIVE',datetime('now'),datetime('now'),'PENDING',1),
('EMP-002','NV002','Trần Thị Bình','2024-02-01',7500000.0,'ACTIVE',datetime('now'),datetime('now'),'PENDING',1),
('EMP-003','NV003','Lê Minh Cường','2024-03-01',9000000.0,'ACTIVE',datetime('now'),datetime('now'),'PENDING',1),
('EMP-004','NV004','Phạm Thị Dung','2024-04-01',7500000.0,'ACTIVE',datetime('now'),datetime('now'),'PENDING',1),
('EMP-005','NV005','Hoàng Văn Em','2023-06-01',10000000.0,'ACTIVE',datetime('now'),datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO StoreManager VALUES
('MGR-001','EMP-005','STORE-001',datetime('now'),'PENDING',1),
('MGR-002','EMP-003','STORE-002',datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO EmployeeAssignment VALUES
('ASSIGN-001','EMP-001','STORE-001','2024-01-15',NULL,'ACTIVE',datetime('now'),'PENDING',1),
('ASSIGN-002','EMP-002','STORE-001','2024-02-01',NULL,'ACTIVE',datetime('now'),'PENDING',1),
('ASSIGN-003','EMP-003','STORE-002','2024-03-01',NULL,'ACTIVE',datetime('now'),'PENDING',1),
('ASSIGN-004','EMP-004','STORE-003','2024-04-01',NULL,'ACTIVE',datetime('now'),'PENDING',1),
('ASSIGN-005','EMP-005','STORE-001','2023-06-01',NULL,'ACTIVE',datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO HR VALUES
('HR-001','EMP-001','2024-01-15',NULL,'ACTIVE',datetime('now'),'PENDING',1),
('HR-002','EMP-002','2024-02-01',NULL,'ACTIVE',datetime('now'),'PENDING',1),
('HR-003','EMP-003','2024-03-01',NULL,'ACTIVE',datetime('now'),'PENDING',1),
('HR-004','EMP-004','2024-04-01',NULL,'ACTIVE',datetime('now'),'PENDING',1),
('HR-005','EMP-005','2023-06-01',NULL,'ACTIVE',datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO ShiftTemplate VALUES
('SHIFT-T1','Ca Sáng','06:00','14:00',30,'ACTIVE',datetime('now'),'PENDING',1),
('SHIFT-T2','Ca Chiều','14:00','22:00',30,'ACTIVE',datetime('now'),'PENDING',1),
('SHIFT-T3','Ca Đêm','22:00','06:00',30,'ACTIVE',datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO ShiftAssignment VALUES
('SA-001','EMP-001','SHIFT-T1','STORE-001','2026-03-07','SCHEDULED',datetime('now'),datetime('now'),'PENDING',1),
('SA-002','EMP-002','SHIFT-T2','STORE-001','2026-03-07','SCHEDULED',datetime('now'),datetime('now'),'PENDING',1),
('SA-003','EMP-004','SHIFT-T1','STORE-003','2026-03-07','COMPLETED',datetime('now'),datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO AttendanceRecord VALUES
('ATT-001','SA-001','2026-03-07 06:05:00',NULL,NULL,'PENDING',datetime('now'),'PENDING',1),
('ATT-002','SA-002','2026-03-07 14:03:00',NULL,NULL,'PENDING',datetime('now'),'PENDING',1),
('ATT-003','SA-003','2026-03-07 06:01:00','2026-03-07 14:00:00','EMP-003','APPROVED',datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO PayrollPeriod VALUES
('PP-2026-02','2026-02-01','2026-02-28','LOCKED','2026-03-02 09:00:00',datetime('now'),'PENDING',1),
('PP-2026-03','2026-03-01','2026-03-31','OPEN',NULL,datetime('now'),'PENDING',1);

INSERT OR IGNORE INTO PayrollSnapshot VALUES
('SNAP-001','EMP-001','PP-2026-02',8000000.0,'BaseSalary * (WorkedDays/TotalWorkDays)','2026-03-02 09:00:00',1,datetime('now'),'SYNCED',1),
('SNAP-002','EMP-002','PP-2026-02',7500000.0,'BaseSalary * (WorkedDays/TotalWorkDays)','2026-03-02 09:00:00',1,datetime('now'),'SYNCED',1);

