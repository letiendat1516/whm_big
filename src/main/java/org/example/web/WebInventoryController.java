package org.example.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.db.PgDatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * Inventory REST API: Warehouses, Balances, Inbound/Outbound
 */
public class WebInventoryController {

    private static final PgDatabaseManager pg = PgDatabaseManager.getInstance();

    public static void register(Javalin app) {
        app.get("/api/warehouses",        WebInventoryController::listWarehouses);
        app.get("/api/warehouses/{id}/balances", WebInventoryController::warehouseBalances);
        app.get("/api/inventory/balances", WebInventoryController::allBalances);
        app.get("/api/suppliers",         WebInventoryController::listSuppliers);
        app.get("/api/inbound",           WebInventoryController::listInbound);
        app.get("/api/outbound",          WebInventoryController::listOutbound);
    }

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
}

