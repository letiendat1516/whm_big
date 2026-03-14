package org.example.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.db.PgDatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * Transfer REST API:
 *   - Store Transfer Requests (kho → cửa hàng)
 *   - Warehouse Overstock Requests (hàng tồn kho → cửa hàng)
 */
public class WebTransferController {

    private static final PgDatabaseManager pg = PgDatabaseManager.getInstance();

    public static void register(Javalin app) {
        // Store Transfer Requests CRUD
        app.get("/api/transfers/store",              WebTransferController::listStoreTransfers);
        app.get("/api/transfers/store/{id}",         WebTransferController::getStoreTransfer);
        app.post("/api/transfers/store",             WebTransferController::createStoreTransfer);
        app.put("/api/transfers/store/{id}",         WebTransferController::updateStoreTransfer);
        app.delete("/api/transfers/store/{id}",      WebTransferController::deleteStoreTransfer);
        app.post("/api/transfers/store/{id}/submit", WebTransferController::submitStoreTransfer);
        app.post("/api/transfers/store/{id}/assign", WebTransferController::assignWarehouse);
        app.post("/api/transfers/store/{id}/ship",   WebTransferController::markStoreShipped);
        app.post("/api/transfers/store/{id}/receive",WebTransferController::markStoreReceived);
        app.post("/api/transfers/store/{id}/reject", WebTransferController::rejectStoreTransfer);

        // Store Transfer Items
        app.get("/api/transfers/store/{id}/items",       WebTransferController::listStoreTransferItems);
        app.post("/api/transfers/store/{id}/items",      WebTransferController::addStoreTransferItem);
        app.delete("/api/transfers/store/items/{itemId}",WebTransferController::deleteStoreTransferItem);

        // Warehouse Overstock Requests CRUD
        app.get("/api/transfers/overstock",              WebTransferController::listOverstockRequests);
        app.get("/api/transfers/overstock/{id}",         WebTransferController::getOverstockRequest);
        app.post("/api/transfers/overstock",             WebTransferController::createOverstockRequest);
        app.put("/api/transfers/overstock/{id}",         WebTransferController::updateOverstockRequest);
        app.delete("/api/transfers/overstock/{id}",      WebTransferController::deleteOverstockRequest);
        app.post("/api/transfers/overstock/{id}/submit", WebTransferController::submitOverstock);
        app.post("/api/transfers/overstock/{id}/assign", WebTransferController::assignTargetStore);
        app.post("/api/transfers/overstock/{id}/accept", WebTransferController::acceptOverstock);
        app.post("/api/transfers/overstock/{id}/ship",   WebTransferController::markOverstockShipped);
        app.post("/api/transfers/overstock/{id}/receive",WebTransferController::markOverstockReceived);
        app.post("/api/transfers/overstock/{id}/reject", WebTransferController::rejectOverstock);

        // Overstock Items
        app.get("/api/transfers/overstock/{id}/items",       WebTransferController::listOverstockItems);
        app.post("/api/transfers/overstock/{id}/items",      WebTransferController::addOverstockItem);
        app.delete("/api/transfers/overstock/items/{itemId}",WebTransferController::deleteOverstockItem);
    }

    // ═══════════════════════════════════════════════════════
    //  STORE TRANSFER REQUESTS
    // ═══════════════════════════════════════════════════════

    private static void listStoreTransfers(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT r.*, s.name as store_name, w.warehouse_name " +
                "FROM \"StoreTransferRequest\" r " +
                "LEFT JOIN \"Store\" s ON s.\"storeId\"=r.store_id " +
                "LEFT JOIN warehouse w ON w.warehouse_id=r.assigned_warehouse_id " +
                "ORDER BY r.created_at DESC")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) list.add(mapStoreTransferRow(rs));
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void getStoreTransfer(Context ctx) {
        String id = ctx.pathParam("id");
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT r.*, s.name as store_name, w.warehouse_name " +
                "FROM \"StoreTransferRequest\" r " +
                "LEFT JOIN \"Store\" s ON s.\"storeId\"=r.store_id " +
                "LEFT JOIN warehouse w ON w.warehouse_id=r.assigned_warehouse_id " +
                "WHERE r.request_id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) ctx.json(mapStoreTransferRow(rs));
                else ctx.status(404).json(Map.of("error", "Not found"));
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void createStoreTransfer(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String id = PgDatabaseManager.newId();
            int no = nextVal(pg, "StoreTransferRequest", "request_no");
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO \"StoreTransferRequest\"(request_id,request_no,store_id,created_by,status,priority,need_date,note,created_at,updated_at) " +
                    "VALUES(?,?,?,?,?,?,?,?,NOW(),NOW())")) {
                ps.setString(1, id);
                ps.setInt(2, no);
                ps.setString(3, (String) body.get("storeId"));
                ps.setString(4, (String) body.get("createdBy"));
                ps.setString(5, "DRAFT");
                ps.setString(6, (String) body.getOrDefault("priority", "NORMAL"));
                ps.setString(7, (String) body.get("needDate"));
                ps.setString(8, (String) body.get("note"));
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true, "requestId", id, "requestNo", no));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void updateStoreTransfer(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            Map body = ctx.bodyAsClass(Map.class);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE \"StoreTransferRequest\" SET priority=COALESCE(?,priority), need_date=COALESCE(?,need_date), note=COALESCE(?,note), updated_at=NOW() WHERE request_id=?")) {
                ps.setString(1, (String) body.get("priority"));
                ps.setString(2, (String) body.get("needDate"));
                ps.setString(3, (String) body.get("note"));
                ps.setString(4, id);
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void deleteStoreTransfer(Context ctx) {
        String id = ctx.pathParam("id");
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM \"StoreTransferRequest\" WHERE request_id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void submitStoreTransfer(Context ctx) {
        updateStatus(ctx, "StoreTransferRequest", "request_id", "SUBMITTED");
    }

    private static void assignWarehouse(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            Map body = ctx.bodyAsClass(Map.class);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE \"StoreTransferRequest\" SET status='ASSIGNED', assigned_warehouse_id=?, assigned_by=?, assigned_at=NOW(), updated_at=NOW() WHERE request_id=?")) {
                ps.setString(1, (String) body.get("warehouseId"));
                ps.setString(2, (String) body.get("assignedBy"));
                ps.setString(3, id);
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void markStoreShipped(Context ctx) {
        String id = ctx.pathParam("id");
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE \"StoreTransferRequest\" SET status='SHIPPING', shipped_at=NOW(), updated_at=NOW() WHERE request_id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void markStoreReceived(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            Map body = ctx.bodyAsClass(Map.class);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE \"StoreTransferRequest\" SET status='RECEIVED', received_by=?, received_at=NOW(), updated_at=NOW() WHERE request_id=?")) {
                ps.setString(1, (String) body.getOrDefault("receivedBy", ""));
                ps.setString(2, id);
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void rejectStoreTransfer(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            Map body = ctx.bodyAsClass(Map.class);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE \"StoreTransferRequest\" SET status='REJECTED', reject_reason=?, updated_at=NOW() WHERE request_id=?")) {
                ps.setString(1, (String) body.getOrDefault("reason", ""));
                ps.setString(2, id);
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    // ── STORE TRANSFER ITEMS ─────────────────────────────────

    private static void listStoreTransferItems(Context ctx) {
        String id = ctx.pathParam("id");
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT i.*, p.\"Name\" as product_name, p.product_code " +
                "FROM \"StoreTransferRequestItem\" i " +
                "LEFT JOIN \"Product\" p ON p.\"ProductID\"=i.product_id " +
                "WHERE i.request_id=? ORDER BY i.item_id")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> list = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("itemId", rs.getString("item_id"));
                    m.put("requestId", rs.getString("request_id"));
                    m.put("productId", rs.getString("product_id"));
                    m.put("productName", rs.getString("product_name"));
                    m.put("productCode", rs.getString("product_code"));
                    m.put("requestedQty", rs.getInt("requested_qty"));
                    m.put("approvedQty", rs.getObject("approved_qty"));
                    m.put("note", rs.getString("note"));
                    list.add(m);
                }
                ctx.json(list);
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void addStoreTransferItem(Context ctx) {
        String reqId = ctx.pathParam("id");
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String itemId = PgDatabaseManager.newId();
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO \"StoreTransferRequestItem\"(item_id,request_id,product_id,requested_qty,note) VALUES(?,?,?,?,?)")) {
                ps.setString(1, itemId);
                ps.setString(2, reqId);
                ps.setString(3, (String) body.get("productId"));
                ps.setInt(4, ((Number) body.getOrDefault("requestedQty", 1)).intValue());
                ps.setString(5, (String) body.get("note"));
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true, "itemId", itemId));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void deleteStoreTransferItem(Context ctx) {
        String itemId = ctx.pathParam("itemId");
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM \"StoreTransferRequestItem\" WHERE item_id=?")) {
            ps.setString(1, itemId);
            ps.executeUpdate();
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  WAREHOUSE OVERSTOCK REQUESTS
    // ═══════════════════════════════════════════════════════

    private static void listOverstockRequests(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT r.*, w.warehouse_name, s.name as target_store_name " +
                "FROM \"WarehouseOverstockRequest\" r " +
                "LEFT JOIN warehouse w ON w.warehouse_id=r.warehouse_id " +
                "LEFT JOIN \"Store\" s ON s.\"storeId\"=r.target_store_id " +
                "ORDER BY r.created_at DESC")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) list.add(mapOverstockRow(rs));
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void getOverstockRequest(Context ctx) {
        String id = ctx.pathParam("id");
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT r.*, w.warehouse_name, s.name as target_store_name " +
                "FROM \"WarehouseOverstockRequest\" r " +
                "LEFT JOIN warehouse w ON w.warehouse_id=r.warehouse_id " +
                "LEFT JOIN \"Store\" s ON s.\"storeId\"=r.target_store_id " +
                "WHERE r.request_id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) ctx.json(mapOverstockRow(rs));
                else ctx.status(404).json(Map.of("error", "Not found"));
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void createOverstockRequest(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String id = PgDatabaseManager.newId();
            int no = nextVal(pg, "WarehouseOverstockRequest", "request_no");
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO \"WarehouseOverstockRequest\"(request_id,request_no,warehouse_id,created_by,status,note,created_at,updated_at) " +
                    "VALUES(?,?,?,?,?,?,NOW(),NOW())")) {
                ps.setString(1, id);
                ps.setInt(2, no);
                ps.setString(3, (String) body.get("warehouseId"));
                ps.setString(4, (String) body.get("createdBy"));
                ps.setString(5, "DRAFT");
                ps.setString(6, (String) body.get("note"));
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true, "requestId", id, "requestNo", no));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void updateOverstockRequest(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            Map body = ctx.bodyAsClass(Map.class);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE \"WarehouseOverstockRequest\" SET note=COALESCE(?,note), updated_at=NOW() WHERE request_id=?")) {
                ps.setString(1, (String) body.get("note"));
                ps.setString(2, id);
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void deleteOverstockRequest(Context ctx) {
        String id = ctx.pathParam("id");
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM \"WarehouseOverstockRequest\" WHERE request_id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void submitOverstock(Context ctx) {
        updateStatus(ctx, "WarehouseOverstockRequest", "request_id", "SUBMITTED");
    }

    private static void assignTargetStore(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            Map body = ctx.bodyAsClass(Map.class);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE \"WarehouseOverstockRequest\" SET status='ASSIGNED', target_store_id=?, assigned_by=?, assigned_at=NOW(), updated_at=NOW() WHERE request_id=?")) {
                ps.setString(1, (String) body.get("targetStoreId"));
                ps.setString(2, (String) body.get("assignedBy"));
                ps.setString(3, id);
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void acceptOverstock(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            Map body = ctx.bodyAsClass(Map.class);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE \"WarehouseOverstockRequest\" SET status='ACCEPTED', accepted_by=?, accepted_at=NOW(), updated_at=NOW() WHERE request_id=?")) {
                ps.setString(1, (String) body.getOrDefault("acceptedBy", ""));
                ps.setString(2, id);
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void markOverstockShipped(Context ctx) {
        String id = ctx.pathParam("id");
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE \"WarehouseOverstockRequest\" SET status='SHIPPING', shipped_at=NOW(), updated_at=NOW() WHERE request_id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void markOverstockReceived(Context ctx) {
        String id = ctx.pathParam("id");
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE \"WarehouseOverstockRequest\" SET status='RECEIVED', received_at=NOW(), updated_at=NOW() WHERE request_id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void rejectOverstock(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            Map body = ctx.bodyAsClass(Map.class);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE \"WarehouseOverstockRequest\" SET status='REJECTED', reject_reason=?, updated_at=NOW() WHERE request_id=?")) {
                ps.setString(1, (String) body.getOrDefault("reason", ""));
                ps.setString(2, id);
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    // ── OVERSTOCK ITEMS ──────────────────────────────────────

    private static void listOverstockItems(Context ctx) {
        String id = ctx.pathParam("id");
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT i.*, p.\"Name\" as product_name, p.product_code " +
                "FROM \"WarehouseOverstockRequestItem\" i " +
                "LEFT JOIN \"Product\" p ON p.\"ProductID\"=i.product_id " +
                "WHERE i.request_id=? ORDER BY i.item_id")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> list = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("itemId", rs.getString("item_id"));
                    m.put("requestId", rs.getString("request_id"));
                    m.put("productId", rs.getString("product_id"));
                    m.put("productName", rs.getString("product_name"));
                    m.put("productCode", rs.getString("product_code"));
                    m.put("overstockQty", rs.getInt("overstock_qty"));
                    m.put("transferQty", rs.getObject("transfer_qty"));
                    m.put("note", rs.getString("note"));
                    list.add(m);
                }
                ctx.json(list);
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void addOverstockItem(Context ctx) {
        String reqId = ctx.pathParam("id");
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String itemId = PgDatabaseManager.newId();
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO \"WarehouseOverstockRequestItem\"(item_id,request_id,product_id,overstock_qty,transfer_qty,note) VALUES(?,?,?,?,?,?)")) {
                ps.setString(1, itemId);
                ps.setString(2, reqId);
                ps.setString(3, (String) body.get("productId"));
                ps.setInt(4, ((Number) body.getOrDefault("overstockQty", 0)).intValue());
                ps.setObject(5, body.get("transferQty") != null ? ((Number) body.get("transferQty")).intValue() : null);
                ps.setString(6, (String) body.get("note"));
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true, "itemId", itemId));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void deleteOverstockItem(Context ctx) {
        String itemId = ctx.pathParam("itemId");
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM \"WarehouseOverstockRequestItem\" WHERE item_id=?")) {
            ps.setString(1, itemId);
            ps.executeUpdate();
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════

    private static void updateStatus(Context ctx, String table, String pkCol, String newStatus) {
        String id = ctx.pathParam("id");
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE \"" + table + "\" SET status=?, updated_at=NOW() WHERE " + pkCol + "=?")) {
            ps.setString(1, newStatus);
            ps.setString(2, id);
            ps.executeUpdate();
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static int nextVal(PgDatabaseManager pg, String table, String col) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT COALESCE(MAX(" + col + "),0)+1 FROM \"" + table + "\"")) {
            return rs.next() ? rs.getInt(1) : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private static Map<String, Object> mapStoreTransferRow(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("requestId", rs.getString("request_id"));
        m.put("requestNo", rs.getInt("request_no"));
        m.put("storeId", rs.getString("store_id"));
        m.put("createdBy", rs.getString("created_by"));
        m.put("status", rs.getString("status"));
        m.put("priority", rs.getString("priority"));
        m.put("needDate", rs.getString("need_date"));
        m.put("note", rs.getString("note"));
        m.put("rejectReason", rs.getString("reject_reason"));
        m.put("assignedWarehouseId", rs.getString("assigned_warehouse_id"));
        m.put("assignedBy", rs.getString("assigned_by"));
        m.put("assignedAt", rs.getString("assigned_at"));
        m.put("shippedAt", rs.getString("shipped_at"));
        m.put("receivedAt", rs.getString("received_at"));
        m.put("receivedBy", rs.getString("received_by"));
        m.put("createdAt", rs.getString("created_at"));
        m.put("updatedAt", rs.getString("updated_at"));
        try { m.put("storeName", rs.getString("store_name")); } catch (SQLException ignored) {}
        try { m.put("warehouseName", rs.getString("warehouse_name")); } catch (SQLException ignored) {}
        return m;
    }

    private static Map<String, Object> mapOverstockRow(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("requestId", rs.getString("request_id"));
        m.put("requestNo", rs.getInt("request_no"));
        m.put("warehouseId", rs.getString("warehouse_id"));
        m.put("createdBy", rs.getString("created_by"));
        m.put("status", rs.getString("status"));
        m.put("targetStoreId", rs.getString("target_store_id"));
        m.put("assignedBy", rs.getString("assigned_by"));
        m.put("assignedAt", rs.getString("assigned_at"));
        m.put("acceptedBy", rs.getString("accepted_by"));
        m.put("acceptedAt", rs.getString("accepted_at"));
        m.put("shippedAt", rs.getString("shipped_at"));
        m.put("receivedAt", rs.getString("received_at"));
        m.put("note", rs.getString("note"));
        m.put("rejectReason", rs.getString("reject_reason"));
        m.put("createdAt", rs.getString("created_at"));
        m.put("updatedAt", rs.getString("updated_at"));
        try { m.put("warehouseName", rs.getString("warehouse_name")); } catch (SQLException ignored) {}
        try { m.put("targetStoreName", rs.getString("target_store_name")); } catch (SQLException ignored) {}
        return m;
    }
}

