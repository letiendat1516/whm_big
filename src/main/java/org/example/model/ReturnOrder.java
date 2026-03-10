package org.example.model;

public class ReturnOrder {
    private String returnId;
    private String orderId;
    private String managerId;
    private String returnDate;
    private String status; // PENDING/APPROVED/COMPLETED/REJECTED
    private int quantity;
    private String reason;
    private double refundAmount;
    private String restockWarehouseId;
    // transient
    private String customerName;

    public ReturnOrder() {}

    public String getReturnId() { return returnId; }
    public void setReturnId(String v) { this.returnId = v; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { this.orderId = v; }
    public String getManagerId() { return managerId; }
    public void setManagerId(String v) { this.managerId = v; }
    public String getReturnDate() { return returnDate; }
    public void setReturnDate(String v) { this.returnDate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { this.quantity = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(double v) { this.refundAmount = v; }
    public String getRestockWarehouseId() { return restockWarehouseId; }
    public void setRestockWarehouseId(String v) { this.restockWarehouseId = v; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { this.customerName = v; }
}

