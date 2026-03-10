package org.example.dao;

import org.example.db.DatabaseManager;
import org.example.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Warehouse stock and movement operations. */
public class InventoryDAO {

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ── Warehouse ──────────────────────────────────────────────────
    public List<Warehouse> findAllWarehouses() throws SQLException {
        List<Warehouse> list = new ArrayList<>();
        String sql = "SELECT * FROM warehouse WHERE is_active=1 ORDER BY warehouse_name";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapWarehouse(rs));
        }
        return list;
    }

    public Warehouse findWarehouseById(String id) throws SQLException {
        String sql = "SELECT * FROM warehouse WHERE warehouse_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapWarehouse(rs) : null; }
        }
    }

    // ── Balances ───────────────────────────────────────────────────
    public List<WarehouseBalance> findAllBalances() throws SQLException {
        List<WarehouseBalance> list = new ArrayList<>();
        String sql = "SELECT wb.*, w.warehouse_name, p.Name as product_name, p.product_code, w.threshold " +
                     "FROM warehouse_balances wb " +
                     "JOIN warehouse w ON w.warehouse_id=wb.warehouse_id " +
                     "JOIN Product   p ON p.ProductID=wb.product_id " +
                     "ORDER BY w.warehouse_name, p.Name";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapBalance(rs));
        }
        return list;
    }

    public List<WarehouseBalance> findLowStockBalances() throws SQLException {
        List<WarehouseBalance> list = new ArrayList<>();
        String sql = "SELECT wb.*, w.warehouse_name, p.Name as product_name, p.product_code, w.threshold " +
                     "FROM warehouse_balances wb " +
                     "JOIN warehouse w ON w.warehouse_id=wb.warehouse_id " +
                     "JOIN Product   p ON p.ProductID=wb.product_id " +
                     "WHERE (wb.on_hand_qty - wb.reserved_qty) <= w.threshold " +
                     "ORDER BY (wb.on_hand_qty - wb.reserved_qty) ASC";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapBalance(rs));
        }
        return list;
    }

    public WarehouseBalance getBalance(String warehouseId, String productId) throws SQLException {
        String sql = "SELECT wb.*, w.warehouse_name, p.Name as product_name, p.product_code, w.threshold " +
                     "FROM warehouse_balances wb " +
                     "JOIN warehouse w ON w.warehouse_id=wb.warehouse_id " +
                     "JOIN Product   p ON p.ProductID=wb.product_id " +
                     "WHERE wb.warehouse_id=? AND wb.product_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, warehouseId); ps.setString(2, productId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapBalance(rs) : null; }
        }
    }

    /**
     * Decrement stock after sale. Throws if insufficient stock.
     * Used by POSService.checkout() inside a transaction.
     */
    public void decrementStock(Connection conn, String warehouseId, String productId, int qty) throws SQLException {
        // Check availability
        String check = "SELECT (on_hand_qty - reserved_qty) AS avail FROM warehouse_balances WHERE warehouse_id=? AND product_id=?";
        try (PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setString(1, warehouseId); ps.setString(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt("avail") < qty)
                    throw new SQLException("Insufficient stock for product " + productId + " in warehouse " + warehouseId);
            }
        }
        String sql = "UPDATE warehouse_balances SET on_hand_qty = on_hand_qty - ?, updated_at=datetime('now') " +
                     "WHERE warehouse_id=? AND product_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty); ps.setString(2, warehouseId); ps.setString(3, productId);
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("warehouse_balances", warehouseId + ":" + productId, "UPDATE", null);
    }

    /** Increment stock (inbound or return). */
    public void incrementStock(String warehouseId, String productId, int qty) throws SQLException {
        String upsert = "INSERT INTO warehouse_balances(warehouse_id,product_id,on_hand_qty,reserved_qty,updated_at) " +
                        "VALUES(?,?,?,0,datetime('now')) " +
                        "ON CONFLICT(warehouse_id,product_id) DO UPDATE SET on_hand_qty=on_hand_qty+?,updated_at=datetime('now')";
        try (PreparedStatement ps = db.getConnection().prepareStatement(upsert)) {
            ps.setString(1, warehouseId); ps.setString(2, productId);
            ps.setInt(3, qty); ps.setInt(4, qty);
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("warehouse_balances", warehouseId + ":" + productId, "UPDATE", null);
    }

    /** Shortcut: find first active warehouse for a store. */
    public String getDefaultWarehouseId(String storeId) throws SQLException {
        String sql = "SELECT warehouse_id FROM warehouse WHERE is_active=1 LIMIT 1";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString("warehouse_id") : null;
        }
    }

    // ── Mappers ────────────────────────────────────────────────────
    private Warehouse mapWarehouse(ResultSet rs) throws SQLException {
        Warehouse w = new Warehouse();
        w.setWarehouseId(rs.getString("warehouse_id"));
        w.setWarehouseCode(rs.getString("warehouse_code"));
        w.setWarehouseName(rs.getString("warehouse_name"));
        w.setAddress(rs.getString("address"));
        w.setIsActive(rs.getInt("is_active"));
        try { w.setRemainingCapacity(rs.getInt("remaining_capacity")); } catch (SQLException ignored){}
        try { w.setIsLowStock(rs.getInt("is_low_stock")); } catch (SQLException ignored){}
        try { w.setThreshold(rs.getInt("threshold")); } catch (SQLException ignored){}
        return w;
    }

    private WarehouseBalance mapBalance(ResultSet rs) throws SQLException {
        WarehouseBalance b = new WarehouseBalance();
        b.setWarehouseId(rs.getString("warehouse_id"));
        b.setProductId(rs.getString("product_id"));
        b.setOnHandQty(rs.getInt("on_hand_qty"));
        b.setReservedQty(rs.getInt("reserved_qty"));
        b.setUpdatedAt(rs.getString("updated_at"));
        try { b.setWarehouseName(rs.getString("warehouse_name")); } catch (SQLException ignored){}
        try { b.setProductName(rs.getString("product_name")); } catch (SQLException ignored){}
        try { b.setProductCode(rs.getString("product_code")); } catch (SQLException ignored){}
        try { b.setThreshold(rs.getInt("threshold")); } catch (SQLException ignored){}
        return b;
    }
}

