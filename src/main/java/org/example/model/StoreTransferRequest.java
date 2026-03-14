package org.example.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Yêu cầu chuyển hàng từ kho lên cửa hàng.
 * Flow: StoreManager tạo → WarehouseSupervisor nhận & gán kho →
 *       InventoryManager xử lý → Outbound (kho) → Inbound (cửa hàng)
 */
public class StoreTransferRequest {
    private String requestId;
    private int requestNo;
    private String storeId;
    private String createdBy;
    private String status; // DRAFT, SUBMITTED, APPROVED, ASSIGNED, SHIPPING, RECEIVED, REJECTED, CANCELLED
    private String priority; // LOW, NORMAL, HIGH, URGENT
    private String needDate;
    private String note;
    private String rejectReason;
    private String assignedWarehouseId;
    private String assignedBy;
    private String assignedAt;
    private String shippedAt;
    private String receivedAt;
    private String receivedBy;
    private String createdAt;
    private String updatedAt;

    // transient display
    private String storeName;
    private String createdByName;
    private String warehouseName;

    private List<StoreTransferRequestItem> items = new ArrayList<>();

    public StoreTransferRequest() {
        this.status = "DRAFT";
        this.priority = "NORMAL";
    }

    // Getters & setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String v) { this.requestId = v; }
    public int getRequestNo() { return requestNo; }
    public void setRequestNo(int v) { this.requestNo = v; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String v) { this.storeId = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getPriority() { return priority; }
    public void setPriority(String v) { this.priority = v; }
    public String getNeedDate() { return needDate; }
    public void setNeedDate(String v) { this.needDate = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String v) { this.rejectReason = v; }
    public String getAssignedWarehouseId() { return assignedWarehouseId; }
    public void setAssignedWarehouseId(String v) { this.assignedWarehouseId = v; }
    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String v) { this.assignedBy = v; }
    public String getAssignedAt() { return assignedAt; }
    public void setAssignedAt(String v) { this.assignedAt = v; }
    public String getShippedAt() { return shippedAt; }
    public void setShippedAt(String v) { this.shippedAt = v; }
    public String getReceivedAt() { return receivedAt; }
    public void setReceivedAt(String v) { this.receivedAt = v; }
    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String v) { this.receivedBy = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String v) { this.updatedAt = v; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String v) { this.storeName = v; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String v) { this.createdByName = v; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }
    public List<StoreTransferRequestItem> getItems() { return items; }
    public void setItems(List<StoreTransferRequestItem> v) { this.items = v; }
}

