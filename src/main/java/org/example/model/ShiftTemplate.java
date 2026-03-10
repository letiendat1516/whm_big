package org.example.model;

public class ShiftTemplate {
    private String shiftTemplateId;
    private String name;
    private String startTime; // HH:MM
    private String endTime;
    private int breakMinutes;
    private String status;

    public ShiftTemplate() {}

    public String getShiftTemplateId() { return shiftTemplateId; }
    public void setShiftTemplateId(String v) { this.shiftTemplateId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String v) { this.startTime = v; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String v) { this.endTime = v; }
    public int getBreakMinutes() { return breakMinutes; }
    public void setBreakMinutes(int v) { this.breakMinutes = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    @Override public String toString() { return name + " (" + startTime + "-" + endTime + ")"; }
}

