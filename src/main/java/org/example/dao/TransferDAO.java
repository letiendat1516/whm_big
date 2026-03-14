package org.example.dao;

import org.example.db.DatabaseManager;
import org.example.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for Store Transfer Requests and Warehouse Overstock Requests.
 *
 * Flow 1 — Store Transfer (kho → cửa hàng):
 *   StoreManager tạo đơn → submit → WarehouseSupervisor approve & gán kho →
 *   InventoryManager tạo outbound → StoreManager nhận hàng → tạo inbound
 *
 * Flow 2 — Overstock Transfer (hàng tồn kho → cửa hàng):
 *   InventoryManager tạo đơn → submit → WarehouseSupervisor gán cửa hàng →
 *   StoreManager accept → InventoryManager tạo outbound → StoreManager tạo inbound
 */
public class TransferDAO {

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ═══════════════════════════════════════════════════════════
    //  STORE TRANSFER REQUEST (yêu cầu chuyển hàng từ kho lên cửa hàng)
    // ═══════════════════════════════════════════════════════════

    public void createStoreTransferRequest(StoreTransferRequest r) throws SQLException {
        if (r.getRequestId() == null) r.setRequestId(DatabaseManager.newId());
        r.setRequestNo(getNextStoreTransferNo());
        String sql = "INSERT INTO StoreTransferRequest(request_id,request_no,store_id,created_by,status,priority,need_date,note,created_at,updated_at) " +
                     "VALUES(?,?,?,?,?,?,?,?,datetime('now'),datetime('now'))";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, r.getRequestId()); ps.setInt(2, r.getRequestNo());
            ps.setString(3, r.getStoreId()); ps.setString(4, r.getCreatedBy());
            ps.setString(5, r.getStatus()); ps.setString(6, r.getPriority());
            ps.setString(7, r.getNeedDate()); ps.setString(8, r.getNote());
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("StoreTransferRequest", r.getRequestId(), "INSERT", null);
    }

    public void addStoreTransferItem(StoreTransferRequestItem item) throws SQLException {
        if (item.getItemId() == null) item.setItemId(DatabaseManager.newId());
        String sql = "INSERT INTO StoreTransferRequestItem(item_id,request_id,product_id,requested_qty,note) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, item.getItemId()); ps.setString(2, item.getRequestId());
            ps.setString(3, item.getProductId()); ps.setInt(4, item.getRequestedQty());
            ps.setString(5, item.getNote());
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("StoreTransferRequestItem", item.getItemId(), "INSERT", null);
    }

    /** Submit request (DRAFT → SUBMITTED). */
    public void submitStoreTransfer(String requestId) throws SQLException {
        updateStoreTransferStatus(requestId, "SUBMITTED");
    }

    /** Approve and assign warehouse (SUBMITTED → ASSIGNED). */
    public void assignWarehouse(String requestId, String warehouseId, String assignedBy) throws SQLException {
        String sql = "UPDATE StoreTransferRequest SET status='ASSIGNED', assigned_warehouse_id=?, assigned_by=?, assigned_at=datetime('now'), updated_at=datetime('now') WHERE request_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, warehouseId); ps.setString(2, assignedBy); ps.setString(3, requestId);
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("StoreTransferRequest", requestId, "UPDATE", null);
    }

    /** Mark as shipped (warehouse created outbound). */
    public void markShipped(String requestId) throws SQLException {
        String sql = "UPDATE StoreTransferRequest SET status='SHIPPING', shipped_at=datetime('now'), updated_at=datetime('now') WHERE request_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, requestId); ps.executeUpdate();
        }
        db.addSyncQueueEntry("StoreTransferRequest", requestId, "UPDATE", null);
    }

    /** Mark as received (store created inbound). */
    public void markReceived(String requestId, String receivedBy) throws SQLException {
        String sql = "UPDATE StoreTransferRequest SET status='RECEIVED', received_at=datetime('now'), received_by=?, updated_at=datetime('now') WHERE request_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, receivedBy); ps.setString(2, requestId);
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("StoreTransferRequest", requestId, "UPDATE", null);
    }

    /** Reject a store transfer request. */
    public void rejectStoreTransfer(String requestId, String reason) throws SQLException {
        String sql = "UPDATE StoreTransferRequest SET status='REJECTED', reject_reason=?, updated_at=datetime('now') WHERE request_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, reason); ps.setString(2, requestId);
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("StoreTransferRequest", requestId, "UPDATE", null);
    }

    public List<StoreTransferRequest> findAllStoreTransfers() throws SQLException {
        List<StoreTransferRequest> list = new ArrayList<>();
        String sql = "SELECT r.*, s.name as store_name, w.warehouse_name, e.fullName as created_by_name " +
                     "FROM StoreTransferRequest r " +
                     "LEFT JOIN Store s ON s.storeId=r.store_id " +
                     "LEFT JOIN warehouse w ON w.warehouse_id=r.assigned_warehouse_id " +
                     "LEFT JOIN Employee e ON e.employeeId=r.created_by " +
                     "ORDER BY r.created_at DESC";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapStoreTransfer(rs));
        }
        return list;
    }

    public List<StoreTransferRequest> findStoreTransfersByStatus(String status) throws SQLException {
        List<StoreTransferRequest> list = new ArrayList<>();
        String sql = "SELECT r.*, s.name as store_name, w.warehouse_name, e.fullName as created_by_name " +
                     "FROM StoreTransferRequest r " +
                     "LEFT JOIN Store s ON s.storeId=r.store_id " +
                     "LEFT JOIN warehouse w ON w.warehouse_id=r.assigned_warehouse_id " +
                     "LEFT JOIN Employee e ON e.employeeId=r.created_by " +
                     "WHERE r.status=? ORDER BY r.created_at DESC";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapStoreTransfer(rs)); }
        }
        return list;
    }

    public StoreTransferRequest findStoreTransferById(String id) throws SQLException {
        String sql = "SELECT r.*, s.name as store_name, w.warehouse_name, e.fullName as created_by_name " +
                     "FROM StoreTransferRequest r " +
                     "LEFT JOIN Store s ON s.storeId=r.store_id " +
                     "LEFT JOIN warehouse w ON w.warehouse_id=r.assigned_warehouse_id " +
                     "LEFT JOIN Employee e ON e.employeeId=r.created_by " +
                     "WHERE r.request_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    StoreTransferRequest r = mapStoreTransfer(rs);
                    r.setItems(findStoreTransferItems(id));
                    return r;
                }
            }
        }
        return null;
    }

    public List<StoreTransferRequestItem> findStoreTransferItems(String requestId) throws SQLException {
        List<StoreTransferRequestItem> list = new ArrayList<>();
        String sql = "SELECT i.*, p.Name as product_name, p.product_code " +
                     "FROM StoreTransferRequestItem i " +
                     "JOIN Product p ON p.ProductID=i.product_id " +
                     "WHERE i.request_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StoreTransferRequestItem item = new StoreTransferRequestItem();
                    item.setItemId(rs.getString("item_id"));
                    item.setRequestId(rs.getString("request_id"));
                    item.setProductId(rs.getString("product_id"));
                    item.setRequestedQty(rs.getInt("requested_qty"));
                    try { item.setApprovedQty(rs.getInt("approved_qty")); if (rs.wasNull()) item.setApprovedQty(null); } catch (SQLException ignored) {}
                    item.setNote(rs.getString("note"));
                    item.setProductName(rs.getString("product_name"));
                    item.setProductCode(rs.getString("product_code"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════
    //  WAREHOUSE OVERSTOCK REQUEST (yêu cầu điều chuyển hàng tồn kho)
    // ═══════════════════════════════════════════════════════════

    public void createOverstockRequest(WarehouseOverstockRequest r) throws SQLException {
        if (r.getRequestId() == null) r.setRequestId(DatabaseManager.newId());
        r.setRequestNo(getNextOverstockNo());
        String sql = "INSERT INTO WarehouseOverstockRequest(request_id,request_no,warehouse_id,created_by,status,note,created_at,updated_at) " +
                     "VALUES(?,?,?,?,?,?,datetime('now'),datetime('now'))";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, r.getRequestId()); ps.setInt(2, r.getRequestNo());
            ps.setString(3, r.getWarehouseId()); ps.setString(4, r.getCreatedBy());
            ps.setString(5, r.getStatus()); ps.setString(6, r.getNote());
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("WarehouseOverstockRequest", r.getRequestId(), "INSERT", null);
    }

    public void addOverstockItem(WarehouseOverstockRequestItem item) throws SQLException {
        if (item.getItemId() == null) item.setItemId(DatabaseManager.newId());
        String sql = "INSERT INTO WarehouseOverstockRequestItem(item_id,request_id,product_id,overstock_qty,note) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, item.getItemId()); ps.setString(2, item.getRequestId());
            ps.setString(3, item.getProductId()); ps.setInt(4, item.getOverstockQty());
            ps.setString(5, item.getNote());
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("WarehouseOverstockRequestItem", item.getItemId(), "INSERT", null);
    }

    /** Submit overstock request (DRAFT → SUBMITTED). */
    public void submitOverstockRequest(String requestId) throws SQLException {
        updateOverstockStatus(requestId, "SUBMITTED");
    }

    /** Assign target store for overstock (SUBMITTED → ASSIGNED). */
    public void assignTargetStore(String requestId, String storeId, String assignedBy) throws SQLException {
        String sql = "UPDATE WarehouseOverstockRequest SET status='ASSIGNED', target_store_id=?, assigned_by=?, assigned_at=datetime('now'), updated_at=datetime('now') WHERE request_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, storeId); ps.setString(2, assignedBy); ps.setString(3, requestId);
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("WarehouseOverstockRequest", requestId, "UPDATE", null);
    }

    /** Store manager accepts overstock (ASSIGNED → ACCEPTED). */
    public void acceptOverstock(String requestId, String acceptedBy) throws SQLException {
        String sql = "UPDATE WarehouseOverstockRequest SET status='ACCEPTED', accepted_by=?, accepted_at=datetime('now'), updated_at=datetime('now') WHERE request_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, acceptedBy); ps.setString(2, requestId);
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("WarehouseOverstockRequest", requestId, "UPDATE", null);
    }

    /** Mark overstock as shipped. */
    public void markOverstockShipped(String requestId) throws SQLException {
        String sql = "UPDATE WarehouseOverstockRequest SET status='SHIPPING', shipped_at=datetime('now'), updated_at=datetime('now') WHERE request_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, requestId); ps.executeUpdate();
        }
        db.addSyncQueueEntry("WarehouseOverstockRequest", requestId, "UPDATE", null);
    }

    /** Mark overstock as received. */
    public void markOverstockReceived(String requestId) throws SQLException {
        String sql = "UPDATE WarehouseOverstockRequest SET status='RECEIVED', received_at=datetime('now'), updated_at=datetime('now') WHERE request_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, requestId); ps.executeUpdate();
        }
        db.addSyncQueueEntry("WarehouseOverstockRequest", requestId, "UPDATE", null);
    }

    /** Reject overstock request. */
    public void rejectOverstock(String requestId, String reason) throws SQLException {
        String sql = "UPDATE WarehouseOverstockRequest SET status='REJECTED', reject_reason=?, updated_at=datetime('now') WHERE request_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, reason); ps.setString(2, requestId);
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("WarehouseOverstockRequest", requestId, "UPDATE", null);
    }

    public List<WarehouseOverstockRequest> findAllOverstockRequests() throws SQLException {
        List<WarehouseOverstockRequest> list = new ArrayList<>();
        String sql = "SELECT r.*, w.warehouse_name, e.fullName as created_by_name, s.name as target_store_name " +
                     "FROM WarehouseOverstockRequest r " +
                     "LEFT JOIN warehouse w ON w.warehouse_id=r.warehouse_id " +
                     "LEFT JOIN Employee e ON e.employeeId=r.created_by " +
                     "LEFT JOIN Store s ON s.storeId=r.target_store_id " +
                     "ORDER BY r.created_at DESC";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapOverstock(rs));
        }
        return list;
    }

    public WarehouseOverstockRequest findOverstockById(String id) throws SQLException {
        String sql = "SELECT r.*, w.warehouse_name, e.fullName as created_by_name, s.name as target_store_name " +
                     "FROM WarehouseOverstockRequest r " +
                     "LEFT JOIN warehouse w ON w.warehouse_id=r.warehouse_id " +
                     "LEFT JOIN Employee e ON e.employeeId=r.created_by " +
                     "LEFT JOIN Store s ON s.storeId=r.target_store_id " +
                     "WHERE r.request_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    WarehouseOverstockRequest r = mapOverstock(rs);
                    r.setItems(findOverstockItems(id));
                    return r;
                }
            }
        }
        return null;
    }

    public List<WarehouseOverstockRequestItem> findOverstockItems(String requestId) throws SQLException {
        List<WarehouseOverstockRequestItem> list = new ArrayList<>();
        String sql = "SELECT i.*, p.Name as product_name, p.product_code " +
                     "FROM WarehouseOverstockRequestItem i " +
                     "JOIN Product p ON p.ProductID=i.product_id " +
                     "WHERE i.request_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WarehouseOverstockRequestItem item = new WarehouseOverstockRequestItem();
                    item.setItemId(rs.getString("item_id"));
                    item.setRequestId(rs.getString("request_id"));
                    item.setProductId(rs.getString("product_id"));
                    item.setOverstockQty(rs.getInt("overstock_qty"));
                    try { item.setTransferQty(rs.getInt("transfer_qty")); if (rs.wasNull()) item.setTransferQty(null); } catch (SQLException ignored) {}
                    item.setNote(rs.getString("note"));
                    item.setProductName(rs.getString("product_name"));
                    item.setProductCode(rs.getString("product_code"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /** Find stores that have highest sales rate for a given product — for overstock recommendation. */
    public List<Object[]> suggestStoresForProduct(String productId) throws SQLException {
        List<Object[]> list = new ArrayList<>();
        // Find stores that sell this product most (by total quantity sold)
        String sql = "SELECT s.storeId, s.name, COALESCE(SUM(oi.quantity),0) as total_sold " +
                     "FROM Store s " +
                     "LEFT JOIN \"Order\" o ON o.storeId=s.storeId AND o.status='COMPLETED' " +
                     "LEFT JOIN OrderItem oi ON oi.orderId=o.orderId AND oi.productId=? " +
                     "WHERE s.status='ACTIVE' " +
                     "GROUP BY s.storeId, s.name " +
                     "ORDER BY total_sold DESC LIMIT 10";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                        rs.getString("storeId"),
                        rs.getString("name"),
                        rs.getInt("total_sold")
                    });
                }
            }
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════

    private void updateStoreTransferStatus(String requestId, String status) throws SQLException {
        String sql = "UPDATE StoreTransferRequest SET status=?, updated_at=datetime('now') WHERE request_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, status); ps.setString(2, requestId);
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("StoreTransferRequest", requestId, "UPDATE", null);
    }

    private void updateOverstockStatus(String requestId, String status) throws SQLException {
        String sql = "UPDATE WarehouseOverstockRequest SET status=?, updated_at=datetime('now') WHERE request_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, status); ps.setString(2, requestId);
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("WarehouseOverstockRequest", requestId, "UPDATE", null);
    }

    // ── DELETE ITEMS ──────────────────────────────────────────

    public void deleteStoreTransferItem(String itemId) throws SQLException {
        String sql = "DELETE FROM StoreTransferRequestItem WHERE item_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, itemId); ps.executeUpdate();
        }
        db.addSyncQueueEntry("StoreTransferRequestItem", itemId, "DELETE", null);
    }

    public void deleteOverstockItem(String itemId) throws SQLException {
        String sql = "DELETE FROM WarehouseOverstockRequestItem WHERE item_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, itemId); ps.executeUpdate();
        }
        db.addSyncQueueEntry("WarehouseOverstockRequestItem", itemId, "DELETE", null);
    }

    private int getNextStoreTransferNo() throws SQLException {
        String sql = "SELECT COALESCE(MAX(request_no),0)+1 FROM StoreTransferRequest";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 1;
        }
    }

    private int getNextOverstockNo() throws SQLException {
        String sql = "SELECT COALESCE(MAX(request_no),0)+1 FROM WarehouseOverstockRequest";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 1;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  MAPPERS
    // ═══════════════════════════════════════════════════════════

    private StoreTransferRequest mapStoreTransfer(ResultSet rs) throws SQLException {
        StoreTransferRequest r = new StoreTransferRequest();
        r.setRequestId(rs.getString("request_id"));
        r.setRequestNo(rs.getInt("request_no"));
        r.setStoreId(rs.getString("store_id"));
        r.setCreatedBy(rs.getString("created_by"));
        r.setStatus(rs.getString("status"));
        r.setPriority(rs.getString("priority"));
        r.setNeedDate(rs.getString("need_date"));
        r.setNote(rs.getString("note"));
        r.setRejectReason(rs.getString("reject_reason"));
        r.setAssignedWarehouseId(rs.getString("assigned_warehouse_id"));
        r.setAssignedBy(rs.getString("assigned_by"));
        r.setAssignedAt(rs.getString("assigned_at"));
        r.setShippedAt(rs.getString("shipped_at"));
        r.setReceivedAt(rs.getString("received_at"));
        r.setReceivedBy(rs.getString("received_by"));
        r.setCreatedAt(rs.getString("created_at"));
        r.setUpdatedAt(rs.getString("updated_at"));
        try { r.setStoreName(rs.getString("store_name")); } catch (SQLException ignored) {}
        try { r.setWarehouseName(rs.getString("warehouse_name")); } catch (SQLException ignored) {}
        try { r.setCreatedByName(rs.getString("created_by_name")); } catch (SQLException ignored) {}
        return r;
    }

    private WarehouseOverstockRequest mapOverstock(ResultSet rs) throws SQLException {
        WarehouseOverstockRequest r = new WarehouseOverstockRequest();
        r.setRequestId(rs.getString("request_id"));
        r.setRequestNo(rs.getInt("request_no"));
        r.setWarehouseId(rs.getString("warehouse_id"));
        r.setCreatedBy(rs.getString("created_by"));
        r.setStatus(rs.getString("status"));
        r.setTargetStoreId(rs.getString("target_store_id"));
        r.setAssignedBy(rs.getString("assigned_by"));
        r.setAssignedAt(rs.getString("assigned_at"));
        r.setAcceptedBy(rs.getString("accepted_by"));
        r.setAcceptedAt(rs.getString("accepted_at"));
        r.setShippedAt(rs.getString("shipped_at"));
        r.setReceivedAt(rs.getString("received_at"));
        r.setNote(rs.getString("note"));
        r.setRejectReason(rs.getString("reject_reason"));
        r.setCreatedAt(rs.getString("created_at"));
        r.setUpdatedAt(rs.getString("updated_at"));
        try { r.setWarehouseName(rs.getString("warehouse_name")); } catch (SQLException ignored) {}
        try { r.setCreatedByName(rs.getString("created_by_name")); } catch (SQLException ignored) {}
        try { r.setTargetStoreName(rs.getString("target_store_name")); } catch (SQLException ignored) {}
        return r;
    }
}

