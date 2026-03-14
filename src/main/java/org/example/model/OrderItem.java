package org.example.model;

public class OrderItem {
    private String orderItemId;
    private String orderId;
    private String productId;
    private String variantId;
    private int quantity;
    private double unitPrice;   // FROZEN at time of sale — never recalculated
    private double itemDiscount;// per-item discount amount
    private double subtotal;
    private double taxRate;     // e.g. 0.1 = 10%
    private double taxAmount;
    // transient display
    private String productName;
    private String variantName;
    private String barcode;

    public OrderItem() { this.taxRate = 0.1; }

    public String getOrderItemId() { return orderItemId; }
    public void setOrderItemId(String v) { this.orderItemId = v; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { this.orderId = v; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { this.productId = v; }
    public String getVariantId() { return variantId; }
    public void setVariantId(String v) { this.variantId = v; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { this.quantity = v; this.subtotal = v * this.unitPrice - this.itemDiscount; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double v) { this.unitPrice = v; this.subtotal = this.quantity * v - this.itemDiscount; }
    public double getItemDiscount() { return itemDiscount; }
    public void setItemDiscount(double v) { this.itemDiscount = v; this.subtotal = this.quantity * this.unitPrice - v; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double v) { this.subtotal = v; }
    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double v) { this.taxRate = v; }
    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double v) { this.taxAmount = v; }
    /** Subtotal + tax */
    public double getLineTotal() { return subtotal + taxAmount; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public String getVariantName() { return variantName; }
    public void setVariantName(String v) { this.variantName = v; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String v) { this.barcode = v; }
}

