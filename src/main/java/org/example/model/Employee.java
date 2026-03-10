package org.example.model;

public class Employee {
    private String employeeId;
    private String employeeCode;
    private String fullName;
    private String hireDate;
    private double baseSalary;
    private String status; // ACTIVE / INACTIVE / TERMINATED / ON_LEAVE
    private String lastModifiedAt;

    public Employee() {}

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String v) { this.employeeId = v; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String v) { this.employeeCode = v; }
    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }
    public String getHireDate() { return hireDate; }
    public void setHireDate(String v) { this.hireDate = v; }
    public double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(double v) { this.baseSalary = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getLastModifiedAt() { return lastModifiedAt; }
    public void setLastModifiedAt(String v) { this.lastModifiedAt = v; }
    @Override public String toString() { return "[" + employeeCode + "] " + fullName; }
}

