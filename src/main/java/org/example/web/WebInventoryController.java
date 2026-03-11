package org.example.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.db.PgDatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * Inventory REST API: Warehouses, Balances, Inbound/Outbound, Suppliers
 */
public class WebInventoryController {

    private static final PgDatabaseManager pg = PgDatabaseManager.getInstance();

    public static void register(Javalin app) {
        // Warehouses CRUD
        app.get("/api/warehouses",          WebInventoryController::listWarehouses);
        app.post("/api/warehouses",         WebInventoryController::createWarehouse);
        app.put("/api/warehouses/{id}",     WebInventoryController::updateWarehouse);
        app.delete("/api/warehouses/{id}",  WebInventoryController::deleteWarehouse);
        app.get("/api/warehouses/{id}/balances", WebInventoryController::warehouseBalances);
        app.get("/api/inventory/balances",  WebInventoryController::allBalances);

        // Suppliers CRUD
        app.get("/api/suppliers",           WebInventoryController::listSuppliers);
        app.post("/api/suppliers",          WebInventoryController::createSupplier);
        app.put("/api/suppliers/{id}",      WebInventoryController::updateSupplier);
        app.delete("/api/suppliers/{id}",   WebInventoryController::deleteSupplier);

        // Inbound / Outbound
        app.get("/api/inbound",             WebInventoryController::listInbound);
        app.post("/api/inbound",            WebInventoryController::createInbound);
        app.get("/api/outbound",            WebInventoryController::listOutbound);
        app.post("/api/outbound",           WebInventoryController::createOutbound);
    }

    // ═══════════════════════════════════════
    // WAREHOUSES CRUD
    // ═══════════════════════════════════════

    private static void listWarehouses(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT * FROM warehouse WHERE is_active=1 ORDER BY warehouse_name")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("warehouseId", rs.getString("warehouse_id"));
                m.put("warehouseCode", rs.getString("warehouse_code"));
                m.put("warehouseName", rs.getString("warehouse_name"));
                m.put("address", rs.getString("address"));
                m.put("remainingCapacity", rs.getInt("remaining_capacity"));
                m.put("threshold", rs.getInt("threshold"));
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void createWarehouse(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String id = "WH-" + PgDatabaseManager.newId().substring(0, 8);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO warehouse(warehouse_id,warehouse_code,warehouse_name,address,is_active,remaining_capacity,threshold) " +
                    "VALUES(?,?,?,?,1,?,?)")) {
                ps.setString(1, id);
                ps.setString(2, (String) body.get("warehouseCode"));
                ps.setString(3, (String) body.get("warehouseName"));
                ps.setString(4, (String) body.get("address"));
                ps.setInt(5, ((Number) body.getOrDefault("remainingCapacity", 1000)).intValue());
                ps.setInt(6, ((Number) body.getOrDefault("threshold", 10)).intValue());
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true, "warehouseId", id));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void updateWarehouse(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE warehouse SET warehouse_code=?,warehouse_name=?,address=?,remaining_capacity=?,threshold=?,updated_at=NOW() " +
                    "WHERE warehouse_id=?")) {
                ps.setString(1, (String) body.get("warehouseCode"));
                ps.setString(2, (String) body.get("warehouseName"));
                ps.setString(3, (String) body.get("address"));
                ps.setInt(4, ((Number) body.getOrDefault("remainingCapacity", 1000)).intValue());
                ps.setInt(5, ((Number) body.getOrDefault("threshold", 10)).intValue());
                ps.setString(6, ctx.pathParam("id"));
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void deleteWarehouse(Context ctx) {
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE warehouse SET is_active=0 WHERE warehouse_id=?")) {
            ps.setString(1, ctx.pathParam("id"));
            ps.executeUpdate();
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════
    // BALANCES
    // ═══════════════════════════════════════

    private static void warehouseBalances(Context ctx) {
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT wb.*, p.\"Name\" as product_name, p.product_code, p.unit " +
                "FROM warehouse_balances wb " +
                "JOIN Product p ON p.\"ProductID\"=wb.product_id " +
                "WHERE wb.warehouse_id=? ORDER BY p.\"Name\"")) {
            ps.setString(1, ctx.pathParam("id"));
            List<Map<String, Object>> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("warehouseId", rs.getString("warehouse_id"));
                    m.put("productId", rs.getString("product_id"));
                    m.put("productName", rs.getString("product_name"));
                    m.put("productCode", rs.getString("product_code"));
                    m.put("unit", rs.getString("unit"));
                    m.put("onHandQty", rs.getInt("on_hand_qty"));
                    m.put("reservedQty", rs.getInt("reserved_qty"));
                    m.put("availableQty", rs.getInt("on_hand_qty") - rs.getInt("reserved_qty"));
                    list.add(m);
                }
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void allBalances(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT wb.*, w.warehouse_name, p.\"Name\" as product_name, p.product_code, p.unit " +
                "FROM warehouse_balances wb " +
                "JOIN warehouse w ON w.warehouse_id=wb.warehouse_id " +
                "JOIN Product p ON p.\"ProductID\"=wb.product_id " +
                "ORDER BY w.warehouse_name, p.\"Name\"")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("warehouseId", rs.getString("warehouse_id"));
                m.put("warehouseName", rs.getString("warehouse_name"));
                m.put("productId", rs.getString("product_id"));
                m.put("productName", rs.getString("product_name"));
                m.put("productCode", rs.getString("product_code"));
                m.put("unit", rs.getString("unit"));
                m.put("onHandQty", rs.getInt("on_hand_qty"));
                m.put("reservedQty", rs.getInt("reserved_qty"));
                m.put("availableQty", rs.getInt("on_hand_qty") - rs.getInt("reserved_qty"));
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════
    // SUPPLIERS CRUD
    // ═══════════════════════════════════════

    private static void listSuppliers(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT * FROM Supplier ORDER BY name_supplier")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("supplierId", rs.getString("supplierID"));
                m.put("name", rs.getString("name_supplier"));
                m.put("isCooperating", rs.getInt("isCooperating") == 1);
                m.put("phone", rs.getString("contact_phone"));
                m.put("address", rs.getString("address"));
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void createSupplier(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String id = "SUP-" + PgDatabaseManager.newId().substring(0, 6);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Supplier(\"supplierID\",name_supplier,\"isCooperating\",contact_phone,address) " +
                    "VALUES(?,?,1,?,?)")) {
                ps.setString(1, id);
                ps.setString(2, (String) body.get("name"));
                ps.setString(3, (String) body.get("phone"));
                ps.setString(4, (String) body.get("address"));
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true, "supplierId", id));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void updateSupplier(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Supplier SET name_supplier=?,contact_phone=?,address=?,\"isCooperating\"=? " +
                    "WHERE \"supplierID\"=?")) {
                ps.setString(1, (String) body.get("name"));
                ps.setString(2, (String) body.get("phone"));
                ps.setString(3, (String) body.get("address"));
                ps.setInt(4, Boolean.TRUE.equals(body.get("isCooperating")) ? 1 : 0);
                ps.setString(5, ctx.pathParam("id"));
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void deleteSupplier(Context ctx) {
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM Supplier WHERE \"supplierID\"=?")) {
            ps.setString(1, ctx.pathParam("id"));
            ps.executeUpdate();
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════
    // INBOUND / OUTBOUND
    // ═══════════════════════════════════════

    private static void listInbound(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT * FROM inbound_documents ORDER BY created_at DESC LIMIT 50")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("inboundId", rs.getString("inbound_id"));
                m.put("inboundNo", rs.getInt("inbound_no"));
                m.put("warehouseId", rs.getString("source_warehouse_id"));
                m.put("statusCode", rs.getString("status_code"));
                m.put("productId", rs.getString("product_id"));
                m.put("qty", rs.getInt("qty"));
                m.put("createdAt", rs.getString("created_at"));
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private static void createInbound(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String id = "INB-" + PgDatabaseManager.newId().substring(0, 8);
            String whId = (String) body.get("warehouseId");
            String productId = (String) body.get("productId");
            int qty = ((Number) body.getOrDefault("qty", 0)).intValue();

            try (Connection conn = pg.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Insert inbound doc
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO inbound_documents(inbound_id,source_warehouse_id,status_code,product_id,qty,created_by) " +
                            "VALUES(?,?,'POSTED',?,?,?)")) {
                        ps.setString(1, id);
                        ps.setString(2, whId);
                        ps.setString(3, productId);
                        ps.setInt(4, qty);
                        ps.setString(5, (String) body.getOrDefault("createdBy", "SYSTEM"));
                        ps.executeUpdate();
                    }
                    // Update warehouse balance
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO warehouse_balances(warehouse_id,product_id,on_hand_qty,reserved_qty) " +
                            "VALUES(?,?,?,0) ON CONFLICT(warehouse_id,product_id) DO UPDATE SET on_hand_qty=warehouse_balances.on_hand_qty+?")) {
                        ps.setString(1, whId);
                        ps.setString(2, productId);
                        ps.setInt(3, qty);
                        ps.setInt(4, qty);
                        ps.executeUpdate();
                    }
                    conn.commit();
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                }
            }
            ctx.json(Map.of("success", true, "inboundId", id));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void listOutbound(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT * FROM outbound_documents ORDER BY created_at DESC LIMIT 50")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("outboundId", rs.getString("outbound_id"));
                m.put("outboundNo", rs.getInt("outbound_no"));
                m.put("warehouseId", rs.getString("source_warehouse_id"));
                m.put("statusCode", rs.getString("status_code"));
                m.put("productId", rs.getString("product_id"));
                m.put("qty", rs.getInt("qty"));
                m.put("createdAt", rs.getString("created_at"));
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private static void createOutbound(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String id = "OUT-" + PgDatabaseManager.newId().substring(0, 8);
            String whId = (String) body.get("warehouseId");
            String productId = (String) body.get("productId");
            int qty = ((Number) body.getOrDefault("qty", 0)).intValue();

            try (Connection conn = pg.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO outbound_documents(outbound_id,source_warehouse_id,status_code,product_id,qty,created_by) " +
                            "VALUES(?,?,'POSTED',?,?,?)")) {
                        ps.setString(1, id);
                        ps.setString(2, whId);
                        ps.setString(3, productId);
                        ps.setInt(4, qty);
                        ps.setString(5, (String) body.getOrDefault("createdBy", "SYSTEM"));
                        ps.executeUpdate();
                    }
                    // Decrease balance
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE warehouse_balances SET on_hand_qty=GREATEST(on_hand_qty-?,0),updated_at=NOW() " +
                            "WHERE warehouse_id=? AND product_id=?")) {
                        ps.setInt(1, qty);
                        ps.setString(2, whId);
                        ps.setString(3, productId);
                        ps.executeUpdate();
                    }
                    conn.commit();
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                }
            }
            ctx.json(Map.of("success", true, "outboundId", id));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }
}

