package org.example.model;

public class ProductVariant {
    private String variantId;
    private String productId;
    private String variantName;
    private String barcode;
    private int status;
    private String createdAt;
    // transient
    private String productName;
    private String productCode;
    private double currentPrice; // from active price list

    public ProductVariant() {}

    public String getVariantId() { return variantId; }
    public void setVariantId(String v) { this.variantId = v; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { this.productId = v; }
    public String getVariantName() { return variantName; }
    public void setVariantName(String v) { this.variantName = v; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String v) { this.barcode = v; }
    public int getStatus() { return status; }
    public void setStatus(int v) { this.status = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String v) { this.productCode = v; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double v) { this.currentPrice = v; }
    @Override public String toString() { return variantName + (barcode != null ? " [" + barcode + "]" : ""); }
}

