package org.example.model;

public class Order {
    private String orderId;
    private String cashierId;
    private String shiftId;
    private String storeId;
    private String customerId;
    private String orderDate;
    private String status; // PENDING/CONFIRMED/COMPLETED/CANCELLED/REFUNDED
    private double totalAmount;
    private double discountAmount;
    private double finalAmount;
    private double taxRate;      // e.g. 0.1 = 10% VAT
    private double taxAmount;
    private String paymentStatus; // UNPAID/PARTIAL/PAID
    private double paidAmount;
    private double debtAmount;
    private String note;
    // transient
    private String cashierName;
    private String customerName;

    public Order() {
        this.taxRate = 0.1;
        this.paymentStatus = "UNPAID";
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String v) { this.orderId = v; }
    public String getCashierId() { return cashierId; }
    public void setCashierId(String v) { this.cashierId = v; }
    public String getShiftId() { return shiftId; }
    public void setShiftId(String v) { this.shiftId = v; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String v) { this.storeId = v; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String v) { this.customerId = v; }
    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String v) { this.orderDate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double v) { this.totalAmount = v; }
    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double v) { this.discountAmount = v; }
    public double getFinalAmount() { return finalAmount; }
    public void setFinalAmount(double v) { this.finalAmount = v; }
    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double v) { this.taxRate = v; }
    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double v) { this.taxAmount = v; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String v) { this.paymentStatus = v; }
    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double v) { this.paidAmount = v; }
    public double getDebtAmount() { return debtAmount; }
    public void setDebtAmount(double v) { this.debtAmount = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public String getCashierName() { return cashierName; }
    public void setCashierName(String v) { this.cashierName = v; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { this.customerName = v; }
}

