package org.example.model;

public class ProductPrice {
    private String priceId;
    private String variantId;
    private String priceListId;
    private double price;
    private String startTime;
    private String endTime; // null = open-ended
    private String createdAt;

    public ProductPrice() {}

    public String getPriceId() { return priceId; }
    public void setPriceId(String v) { this.priceId = v; }
    public String getVariantId() { return variantId; }
    public void setVariantId(String v) { this.variantId = v; }
    public String getPriceListId() { return priceListId; }
    public void setPriceListId(String v) { this.priceListId = v; }
    public double getPrice() { return price; }
    public void setPrice(double v) { this.price = v; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String v) { this.startTime = v; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String v) { this.endTime = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}

