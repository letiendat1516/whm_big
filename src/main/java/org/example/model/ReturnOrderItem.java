package org.example.model;

public class ReturnOrderItem {
    private String returnItemId;
    private String returnId;
    private String orderItemId;
    private String productId;
    private String variantId;
    private int quantity;
    private double unitPrice;
    private double subtotal;
    private String reason;
    // transient
    private String productName;
    private String variantName;

    public ReturnOrderItem() {}

    public String getReturnItemId() { return returnItemId; }
    public void setReturnItemId(String v) { this.returnItemId = v; }
    public String getReturnId() { return returnId; }
    public void setReturnId(String v) { this.returnId = v; }
    public String getOrderItemId() { return orderItemId; }
    public void setOrderItemId(String v) { this.orderItemId = v; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { this.productId = v; }
    public String getVariantId() { return variantId; }
    public void setVariantId(String v) { this.variantId = v; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { this.quantity = v; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double v) { this.unitPrice = v; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double v) { this.subtotal = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public String getVariantName() { return variantName; }
    public void setVariantName(String v) { this.variantName = v; }
}

