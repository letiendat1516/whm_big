package org.example.model;

public class Warehouse {
    private String warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String address;
    private int isActive;
    private int remainingCapacity;
    private int isLowStock;
    private int threshold;

    public Warehouse() {}

    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String v) { this.warehouseId = v; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String v) { this.warehouseCode = v; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = v; }
    public int getIsActive() { return isActive; }
    public void setIsActive(int v) { this.isActive = v; }
    public int getRemainingCapacity() { return remainingCapacity; }
    public void setRemainingCapacity(int v) { this.remainingCapacity = v; }
    public int getIsLowStock() { return isLowStock; }
    public void setIsLowStock(int v) { this.isLowStock = v; }
    public int getThreshold() { return threshold; }
    public void setThreshold(int v) { this.threshold = v; }
    @Override public String toString() { return "[" + warehouseCode + "] " + warehouseName; }
}

