package org.example.model;

public class AttendanceRecord {
    private String attendanceId;
    private String shiftAssignmentId;
    private String checkInTime;
    private String checkOutTime;
    private String approvedBy;
    private String status; // PENDING/APPROVED/REJECTED/ABSENT
    // transient
    private String employeeName;
    private String workDate;
    private String shiftName;

    public AttendanceRecord() {}

    public String getAttendanceId() { return attendanceId; }
    public void setAttendanceId(String v) { this.attendanceId = v; }
    public String getShiftAssignmentId() { return shiftAssignmentId; }
    public void setShiftAssignmentId(String v) { this.shiftAssignmentId = v; }
    public String getCheckInTime() { return checkInTime; }
    public void setCheckInTime(String v) { this.checkInTime = v; }
    public String getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(String v) { this.checkOutTime = v; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String v) { this.approvedBy = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String v) { this.employeeName = v; }
    public String getWorkDate() { return workDate; }
    public void setWorkDate(String v) { this.workDate = v; }
    public String getShiftName() { return shiftName; }
    public void setShiftName(String v) { this.shiftName = v; }
}

