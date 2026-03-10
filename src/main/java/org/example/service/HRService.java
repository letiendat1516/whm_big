package org.example.service;

import org.example.dao.EmployeeDAO;
import org.example.model.*;

import java.sql.SQLException;
import java.util.List;

/** HR: employee management, shift scheduling and attendance. */
public class HRService {

    private final EmployeeDAO dao = new EmployeeDAO();

    public List<Employee> getAllEmployees() throws SQLException {
        return dao.findAll();
    }

    public void saveEmployee(Employee e) throws SQLException {
        if (e.getFullName() == null || e.getFullName().isBlank())
            throw new IllegalArgumentException("Họ tên nhân viên không được để trống.");
        if (e.getEmployeeCode() == null || e.getEmployeeCode().isBlank())
            throw new IllegalArgumentException("Mã nhân viên không được để trống.");
        dao.saveEmployee(e);
    }

    public List<ShiftTemplate> getShiftTemplates() throws SQLException {
        return dao.findAllShiftTemplates();
    }

    public List<ShiftAssignment> getSchedule(String storeId, String date) throws SQLException {
        return dao.findShiftsByDate(storeId, date);
    }

    public void assignShift(ShiftAssignment sa) throws SQLException {
        if (sa.getEmployeeId() == null || sa.getShiftTemplateId() == null || sa.getWorkDate() == null)
            throw new IllegalArgumentException("Thiếu thông tin phân ca.");
        dao.assignShift(sa);
    }

    public void clockIn(String shiftAssignmentId) throws SQLException {
        dao.clockIn(shiftAssignmentId);
    }

    public void clockOut(String shiftAssignmentId) throws SQLException {
        dao.clockOut(shiftAssignmentId);
    }

    public List<AttendanceRecord> getAttendance(String storeId, String date) throws SQLException {
        return dao.findAttendanceByDate(storeId, date);
    }
}

