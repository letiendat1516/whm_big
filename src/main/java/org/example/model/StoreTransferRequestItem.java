package org.example.model;

/**
 * Item line in a StoreTransferRequest.
 */
public class StoreTransferRequestItem {
    private String itemId;
    private String requestId;
    private String productId;
    private int requestedQty;
    private Integer approvedQty;
    private String note;

    // transient display
    private String productName;
    private String productCode;

    public StoreTransferRequestItem() {}

    public String getItemId() { return itemId; }
    public void setItemId(String v) { this.itemId = v; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String v) { this.requestId = v; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { this.productId = v; }
    public int getRequestedQty() { return requestedQty; }
    public void setRequestedQty(int v) { this.requestedQty = v; }
    public Integer getApprovedQty() { return approvedQty; }
    public void setApprovedQty(Integer v) { this.approvedQty = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String v) { this.productCode = v; }
}

