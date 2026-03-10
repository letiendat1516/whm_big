package org.example.model;

public class Product {
    private String productId;
    private String productCode;
    private String name;
    private String categoryId;
    private String brand;
    private String unit;
    private String description;
    private String status; // ACTIVE / INACTIVE / DISCONTINUED
    private int salesRate;
    private String createdAt;
    private String updatedAt;
    // transient display
    private String categoryName;

    public Product() {}

    public String getProductId() { return productId; }
    public void setProductId(String v) { this.productId = v; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String v) { this.productCode = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String v) { this.categoryId = v; }
    public String getBrand() { return brand; }
    public void setBrand(String v) { this.brand = v; }
    public String getUnit() { return unit; }
    public void setUnit(String v) { this.unit = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public int getSalesRate() { return salesRate; }
    public void setSalesRate(int v) { this.salesRate = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String v) { this.updatedAt = v; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String v) { this.categoryName = v; }
    @Override public String toString() { return "[" + productCode + "] " + name; }
}

