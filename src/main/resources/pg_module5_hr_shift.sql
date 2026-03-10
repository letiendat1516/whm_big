-- ============================================================
-- MODULE 5: HR & SHIFT (PostgreSQL)
-- ============================================================

CREATE TABLE IF NOT EXISTS Store (
    "storeId"     TEXT PRIMARY KEY,
    "storeCode"   TEXT NOT NULL UNIQUE,
    name          TEXT NOT NULL,
    address       TEXT,
    status        TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE','CLOSED')),
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS Employee (
    "employeeId"     TEXT PRIMARY KEY,
    "employeeCode"   TEXT NOT NULL UNIQUE,
    "fullName"       TEXT NOT NULL,
    "hireDate"       TEXT NOT NULL,
    "baseSalary"     NUMERIC NOT NULL DEFAULT 0,
    status           TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE','TERMINATED','ON_LEAVE')),
    "lastModifiedAt" TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified    TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status      TEXT NOT NULL DEFAULT 'PENDING',
    version          INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS EmployeeAssignment (
    "assignmentId" TEXT PRIMARY KEY,
    "employeeId"   TEXT NOT NULL REFERENCES Employee("employeeId") ON DELETE CASCADE,
    "storeId"      TEXT NOT NULL REFERENCES Store("storeId") ON DELETE CASCADE,
    "startDate"    TEXT NOT NULL,
    "endDate"      TEXT,
    status         TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','ENDED','TRANSFERRED')),
    last_modified  TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status    TEXT NOT NULL DEFAULT 'PENDING',
    version        INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS StoreManager (
    "managerId"   TEXT PRIMARY KEY,
    "employeeId"  TEXT NOT NULL UNIQUE REFERENCES Employee("employeeId") ON DELETE CASCADE,
    "storeId"     TEXT REFERENCES Store("storeId") ON DELETE SET NULL,
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS HR (
    "hrId"        TEXT PRIMARY KEY,
    "employeeId"  TEXT NOT NULL REFERENCES Employee("employeeId") ON DELETE CASCADE,
    "startDate"   TEXT NOT NULL,
    "endDate"     TEXT,
    status        TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','ENDED','SUSPENDED')),
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status   TEXT NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS ShiftTemplate (
    "shiftTemplateId" TEXT PRIMARY KEY,
    name              TEXT NOT NULL,
    "startTime"       TEXT NOT NULL,
    "endTime"         TEXT NOT NULL,
    "breakMinutes"    INTEGER NOT NULL DEFAULT 0,
    status            TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE')),
    last_modified     TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status       TEXT NOT NULL DEFAULT 'PENDING',
    version           INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS ShiftAssignment (
    "shiftAssignmentId" TEXT PRIMARY KEY,
    "employeeId"        TEXT NOT NULL REFERENCES Employee("employeeId") ON DELETE CASCADE,
    "shiftTemplateId"   TEXT NOT NULL REFERENCES ShiftTemplate("shiftTemplateId") ON DELETE RESTRICT,
    "storeId"           TEXT NOT NULL REFERENCES Store("storeId") ON DELETE CASCADE,
    "workDate"          TEXT NOT NULL,
    status              TEXT NOT NULL DEFAULT 'SCHEDULED',
    "lastModifiedAt"    TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified       TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING',
    version             INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS AttendanceRecord (
    "attendanceId"      TEXT PRIMARY KEY,
    "shiftAssignmentId" TEXT NOT NULL REFERENCES ShiftAssignment("shiftAssignmentId") ON DELETE CASCADE,
    "checkInTime"       TIMESTAMP,
    "checkOutTime"      TIMESTAMP,
    "approvedBy"        TEXT,
    status              TEXT NOT NULL DEFAULT 'PENDING',
    last_modified       TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status         TEXT NOT NULL DEFAULT 'PENDING',
    version             INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS PayrollPeriod (
    "payrollPeriodId" TEXT PRIMARY KEY,
    "startDate"       TEXT NOT NULL,
    "endDate"         TEXT NOT NULL,
    status            TEXT NOT NULL DEFAULT 'OPEN' CHECK(status IN ('OPEN','PROCESSING','LOCKED','PAID')),
    "lockedAt"        TIMESTAMP,
    last_modified     TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status       TEXT NOT NULL DEFAULT 'PENDING',
    version           INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS PayrollSnapshot (
    "snapshotId"      TEXT PRIMARY KEY,
    "employeeId"      TEXT NOT NULL REFERENCES Employee("employeeId") ON DELETE RESTRICT,
    "payrollPeriodId" TEXT NOT NULL REFERENCES PayrollPeriod("payrollPeriodId") ON DELETE RESTRICT,
    "salaryAmount"    NUMERIC NOT NULL DEFAULT 0,
    "calculationRule" TEXT,
    "createdAt"       TIMESTAMP NOT NULL DEFAULT NOW(),
    locked            INTEGER NOT NULL DEFAULT 0,
    last_modified     TIMESTAMP NOT NULL DEFAULT NOW(),
    sync_status       TEXT NOT NULL DEFAULT 'PENDING',
    version           INTEGER NOT NULL DEFAULT 1,
    UNIQUE("employeeId", "payrollPeriodId")
);

