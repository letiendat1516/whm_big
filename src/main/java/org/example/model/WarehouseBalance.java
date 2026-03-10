package org.example.model;

public class WarehouseBalance {
    private String warehouseId;
    private String productId;
    private int onHandQty;
    private int reservedQty;
    private String updatedAt;
    // transient
    private String warehouseName;
    private String productName;
    private String productCode;
    private int threshold;

    public WarehouseBalance() {}

    public int getAvailableQty() { return onHandQty - reservedQty; }

    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String v) { this.warehouseId = v; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { this.productId = v; }
    public int getOnHandQty() { return onHandQty; }
    public void setOnHandQty(int v) { this.onHandQty = v; }
    public int getReservedQty() { return reservedQty; }
    public void setReservedQty(int v) { this.reservedQty = v; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String v) { this.updatedAt = v; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String v) { this.productCode = v; }
    public int getThreshold() { return threshold; }
    public void setThreshold(int v) { this.threshold = v; }
}

