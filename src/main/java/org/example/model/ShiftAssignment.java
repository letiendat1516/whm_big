package org.example.model;

public class ShiftAssignment {
    private String shiftAssignmentId;
    private String employeeId;
    private String shiftTemplateId;
    private String storeId;
    private String workDate;
    private String status; // SCHEDULED/CONFIRMED/COMPLETED/ABSENT/CANCELLED
    // transient
    private String employeeName;
    private String shiftName;
    private String shiftStart;
    private String shiftEnd;

    public ShiftAssignment() {}

    public String getShiftAssignmentId() { return shiftAssignmentId; }
    public void setShiftAssignmentId(String v) { this.shiftAssignmentId = v; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String v) { this.employeeId = v; }
    public String getShiftTemplateId() { return shiftTemplateId; }
    public void setShiftTemplateId(String v) { this.shiftTemplateId = v; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String v) { this.storeId = v; }
    public String getWorkDate() { return workDate; }
    public void setWorkDate(String v) { this.workDate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String v) { this.employeeName = v; }
    public String getShiftName() { return shiftName; }
    public void setShiftName(String v) { this.shiftName = v; }
    public String getShiftStart() { return shiftStart; }
    public void setShiftStart(String v) { this.shiftStart = v; }
    public String getShiftEnd() { return shiftEnd; }
    public void setShiftEnd(String v) { this.shiftEnd = v; }
}

