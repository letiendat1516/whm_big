package org.example.dao;

import org.example.db.DatabaseManager;
import org.example.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Employee, shift scheduling and attendance operations. */
public class EmployeeDAO {

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ── Employee ───────────────────────────────────────────────────
    public List<Employee> findAllActive() throws SQLException {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM Employee WHERE status='ACTIVE' ORDER BY fullName";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapEmployee(rs));
        }
        return list;
    }

    public List<Employee> findAll() throws SQLException {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM Employee ORDER BY fullName";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapEmployee(rs));
        }
        return list;
    }

    public Employee findById(String id) throws SQLException {
        String sql = "SELECT * FROM Employee WHERE employeeId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapEmployee(rs) : null; }
        }
    }

    public void saveEmployee(Employee e) throws SQLException {
        boolean isNew = e.getEmployeeId() == null || e.getEmployeeId().isEmpty();
        if (isNew) e.setEmployeeId(DatabaseManager.newId());
        if (isNew) {
            String sql = "INSERT INTO Employee(employeeId,employeeCode,fullName,hireDate,baseSalary,status) VALUES(?,?,?,?,?,?)";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, e.getEmployeeId()); ps.setString(2, e.getEmployeeCode());
                ps.setString(3, e.getFullName()); ps.setString(4, e.getHireDate() != null ? e.getHireDate() : "");
                ps.setDouble(5, e.getBaseSalary()); ps.setString(6, e.getStatus() != null ? e.getStatus() : "ACTIVE");
                ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE Employee SET employeeCode=?,fullName=?,baseSalary=?,status=? WHERE employeeId=?";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, e.getEmployeeCode()); ps.setString(2, e.getFullName());
                ps.setDouble(3, e.getBaseSalary()); ps.setString(4, e.getStatus());
                ps.setString(5, e.getEmployeeId()); ps.executeUpdate();
            }
        }
        db.addSyncQueueEntry("Employee", e.getEmployeeId(), isNew ? "INSERT" : "UPDATE", null);
    }

    // ── ShiftTemplate ──────────────────────────────────────────────
    public List<ShiftTemplate> findAllShiftTemplates() throws SQLException {
        List<ShiftTemplate> list = new ArrayList<>();
        String sql = "SELECT * FROM ShiftTemplate WHERE status='ACTIVE' ORDER BY startTime";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapShiftTemplate(rs));
        }
        return list;
    }

    // ── ShiftAssignment ────────────────────────────────────────────
    public List<ShiftAssignment> findShiftsByDate(String storeId, String workDate) throws SQLException {
        List<ShiftAssignment> list = new ArrayList<>();
        String sql = "SELECT sa.*, e.fullName as emp_name, st.name as shift_name, st.startTime as sh_start, st.endTime as sh_end " +
                     "FROM ShiftAssignment sa " +
                     "JOIN Employee e ON e.employeeId=sa.employeeId " +
                     "JOIN ShiftTemplate st ON st.shiftTemplateId=sa.shiftTemplateId " +
                     "WHERE sa.storeId=? AND sa.workDate=? ORDER BY st.startTime";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, storeId); ps.setString(2, workDate);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapShiftAssignment(rs)); }
        }
        return list;
    }

    public void assignShift(ShiftAssignment sa) throws SQLException {
        if (sa.getShiftAssignmentId() == null) sa.setShiftAssignmentId(DatabaseManager.newId());
        String sql = "INSERT OR REPLACE INTO ShiftAssignment(shiftAssignmentId,employeeId,shiftTemplateId,storeId,workDate,status) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, sa.getShiftAssignmentId()); ps.setString(2, sa.getEmployeeId());
            ps.setString(3, sa.getShiftTemplateId()); ps.setString(4, sa.getStoreId());
            ps.setString(5, sa.getWorkDate()); ps.setString(6, sa.getStatus() != null ? sa.getStatus() : "SCHEDULED");
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("ShiftAssignment", sa.getShiftAssignmentId(), "INSERT", null);
    }

    // ── Attendance ─────────────────────────────────────────────────
    public void clockIn(String shiftAssignmentId) throws SQLException {
        // Upsert attendance record
        String check = "SELECT attendanceId FROM AttendanceRecord WHERE shiftAssignmentId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(check)) {
            ps.setString(1, shiftAssignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String upd = "UPDATE AttendanceRecord SET checkInTime=datetime('now'),status='PRESENT' WHERE shiftAssignmentId=?";
                    try (PreparedStatement pu = db.getConnection().prepareStatement(upd)) {
                        pu.setString(1, shiftAssignmentId); pu.executeUpdate();
                    }
                } else {
                    String attId = DatabaseManager.newId();
                    String ins = "INSERT INTO AttendanceRecord(attendanceId,shiftAssignmentId,checkInTime,status) VALUES(?,?,datetime('now'),'PRESENT')";
                    try (PreparedStatement pu = db.getConnection().prepareStatement(ins)) {
                        pu.setString(1, attId); pu.setString(2, shiftAssignmentId); pu.executeUpdate();
                    }
                }
            }
        }
        String updShift = "UPDATE ShiftAssignment SET status='CONFIRMED' WHERE shiftAssignmentId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(updShift)) {
            ps.setString(1, shiftAssignmentId); ps.executeUpdate();
        }
    }

    public void clockOut(String shiftAssignmentId) throws SQLException {
        String sql = "UPDATE AttendanceRecord SET checkOutTime=datetime('now'), status='APPROVED' WHERE shiftAssignmentId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, shiftAssignmentId); ps.executeUpdate();
        }
        String updShift = "UPDATE ShiftAssignment SET status='COMPLETED' WHERE shiftAssignmentId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(updShift)) {
            ps.setString(1, shiftAssignmentId); ps.executeUpdate();
        }
    }

    public List<AttendanceRecord> findAttendanceByDate(String storeId, String date) throws SQLException {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = "SELECT ar.*, e.fullName as emp_name, sa.workDate, st.name as shift_name " +
                     "FROM AttendanceRecord ar " +
                     "JOIN ShiftAssignment sa ON sa.shiftAssignmentId=ar.shiftAssignmentId " +
                     "JOIN Employee e ON e.employeeId=sa.employeeId " +
                     "JOIN ShiftTemplate st ON st.shiftTemplateId=sa.shiftTemplateId " +
                     "WHERE sa.storeId=? AND sa.workDate=? ORDER BY e.fullName";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, storeId); ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapAttendance(rs)); }
        }
        return list;
    }

    // ── Mappers ────────────────────────────────────────────────────
    private Employee mapEmployee(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setEmployeeId(rs.getString("employeeId")); e.setEmployeeCode(rs.getString("employeeCode"));
        e.setFullName(rs.getString("fullName")); e.setHireDate(rs.getString("hireDate"));
        e.setBaseSalary(rs.getDouble("baseSalary")); e.setStatus(rs.getString("status"));
        return e;
    }

    private ShiftTemplate mapShiftTemplate(ResultSet rs) throws SQLException {
        ShiftTemplate t = new ShiftTemplate();
        t.setShiftTemplateId(rs.getString("shiftTemplateId")); t.setName(rs.getString("name"));
        t.setStartTime(rs.getString("startTime")); t.setEndTime(rs.getString("endTime"));
        t.setBreakMinutes(rs.getInt("breakMinutes")); t.setStatus(rs.getString("status"));
        return t;
    }

    private ShiftAssignment mapShiftAssignment(ResultSet rs) throws SQLException {
        ShiftAssignment sa = new ShiftAssignment();
        sa.setShiftAssignmentId(rs.getString("shiftAssignmentId"));
        sa.setEmployeeId(rs.getString("employeeId")); sa.setShiftTemplateId(rs.getString("shiftTemplateId"));
        sa.setStoreId(rs.getString("storeId")); sa.setWorkDate(rs.getString("workDate"));
        sa.setStatus(rs.getString("status"));
        try { sa.setEmployeeName(rs.getString("emp_name")); } catch (SQLException ignored){}
        try { sa.setShiftName(rs.getString("shift_name")); } catch (SQLException ignored){}
        try { sa.setShiftStart(rs.getString("sh_start")); } catch (SQLException ignored){}
        try { sa.setShiftEnd(rs.getString("sh_end")); } catch (SQLException ignored){}
        return sa;
    }

    private AttendanceRecord mapAttendance(ResultSet rs) throws SQLException {
        AttendanceRecord a = new AttendanceRecord();
        a.setAttendanceId(rs.getString("attendanceId"));
        a.setShiftAssignmentId(rs.getString("shiftAssignmentId"));
        a.setCheckInTime(rs.getString("checkInTime")); a.setCheckOutTime(rs.getString("checkOutTime"));
        a.setStatus(rs.getString("status"));
        try { a.setEmployeeName(rs.getString("emp_name")); } catch (SQLException ignored){}
        try { a.setWorkDate(rs.getString("workDate")); } catch (SQLException ignored){}
        try { a.setShiftName(rs.getString("shift_name")); } catch (SQLException ignored){}
        return a;
    }
}

