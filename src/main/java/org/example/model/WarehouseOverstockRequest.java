package org.example.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Yêu cầu điều chuyển hàng tồn kho.
 * Flow: InventoryManager tạo → WarehouseSupervisor nhận →
 *       tìm cửa hàng phù hợp → StoreManager accept →
 *       Outbound (kho) → Inbound (cửa hàng)
 */
public class WarehouseOverstockRequest {
    private String requestId;
    private int requestNo;
    private String warehouseId;
    private String createdBy;
    private String status; // DRAFT, SUBMITTED, ASSIGNED, ACCEPTED, SHIPPING, RECEIVED, REJECTED, CANCELLED
    private String targetStoreId;
    private String assignedBy;
    private String assignedAt;
    private String acceptedBy;
    private String acceptedAt;
    private String shippedAt;
    private String receivedAt;
    private String note;
    private String rejectReason;
    private String createdAt;
    private String updatedAt;

    // transient display
    private String warehouseName;
    private String createdByName;
    private String targetStoreName;

    private List<WarehouseOverstockRequestItem> items = new ArrayList<>();

    public WarehouseOverstockRequest() {
        this.status = "DRAFT";
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String v) { this.requestId = v; }
    public int getRequestNo() { return requestNo; }
    public void setRequestNo(int v) { this.requestNo = v; }
    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String v) { this.warehouseId = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getTargetStoreId() { return targetStoreId; }
    public void setTargetStoreId(String v) { this.targetStoreId = v; }
    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String v) { this.assignedBy = v; }
    public String getAssignedAt() { return assignedAt; }
    public void setAssignedAt(String v) { this.assignedAt = v; }
    public String getAcceptedBy() { return acceptedBy; }
    public void setAcceptedBy(String v) { this.acceptedBy = v; }
    public String getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(String v) { this.acceptedAt = v; }
    public String getShippedAt() { return shippedAt; }
    public void setShippedAt(String v) { this.shippedAt = v; }
    public String getReceivedAt() { return receivedAt; }
    public void setReceivedAt(String v) { this.receivedAt = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String v) { this.rejectReason = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String v) { this.updatedAt = v; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String v) { this.createdByName = v; }
    public String getTargetStoreName() { return targetStoreName; }
    public void setTargetStoreName(String v) { this.targetStoreName = v; }
    public List<WarehouseOverstockRequestItem> getItems() { return items; }
    public void setItems(List<WarehouseOverstockRequestItem> v) { this.items = v; }
}

