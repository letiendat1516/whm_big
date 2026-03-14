package org.example.model;

/**
 * Item line in a WarehouseOverstockRequest.
 */
public class WarehouseOverstockRequestItem {
    private String itemId;
    private String requestId;
    private String productId;
    private int overstockQty;
    private Integer transferQty;
    private String note;

    // transient display
    private String productName;
    private String productCode;

    public WarehouseOverstockRequestItem() {}

    public String getItemId() { return itemId; }
    public void setItemId(String v) { this.itemId = v; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String v) { this.requestId = v; }
    public String getProductId() { return productId; }
    public void setProductId(String v) { this.productId = v; }
    public int getOverstockQty() { return overstockQty; }
    public void setOverstockQty(int v) { this.overstockQty = v; }
    public Integer getTransferQty() { return transferQty; }
    public void setTransferQty(Integer v) { this.transferQty = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String v) { this.productCode = v; }
}

