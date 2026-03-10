package org.example.dao;

import org.example.db.DatabaseManager;
import org.example.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Product, Variant, Category, Price operations. */
public class ProductDAO {

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ── ProductCategory ────────────────────────────────────────────
    public List<ProductCategory> findAllCategories() throws SQLException {
        List<ProductCategory> list = new ArrayList<>();
        String sql = "SELECT * FROM ProductCategory WHERE status=1 ORDER BY category_name";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapCategory(rs));
        }
        return list;
    }

    public void saveCategory(ProductCategory c) throws SQLException {
        boolean isNew = c.getCategoryId() == null || c.getCategoryId().isEmpty();
        if (isNew) c.setCategoryId(DatabaseManager.newId());
        String sql = isNew
                ? "INSERT INTO ProductCategory(category_id,category_name,description,status) VALUES(?,?,?,?)"
                : "UPDATE ProductCategory SET category_name=?,description=?,status=? WHERE category_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            if (isNew) {
                ps.setString(1, c.getCategoryId()); ps.setString(2, c.getCategoryName());
                ps.setString(3, c.getDescription()); ps.setInt(4, c.getStatus());
            } else {
                ps.setString(1, c.getCategoryName()); ps.setString(2, c.getDescription());
                ps.setInt(3, c.getStatus()); ps.setString(4, c.getCategoryId());
            }
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("ProductCategory", c.getCategoryId(), isNew ? "INSERT" : "UPDATE", null);
    }

    // ── Product ────────────────────────────────────────────────────
    public List<Product> searchProducts(String query) throws SQLException {
        List<Product> list = new ArrayList<>();
        String like = "%" + (query == null ? "" : query) + "%";
        String sql = "SELECT p.*, c.category_name FROM Product p " +
                     "LEFT JOIN ProductCategory c ON c.category_id=p.category_id " +
                     "WHERE p.Status='ACTIVE' AND (p.Name LIKE ? OR p.product_code LIKE ? OR p.brand LIKE ?) " +
                     "ORDER BY p.Name LIMIT 100";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapProduct(rs));
            }
        }
        return list;
    }

    public List<Product> findAllActive() throws SQLException {
        return searchProducts("");
    }

    public Product findById(String productId) throws SQLException {
        String sql = "SELECT p.*, c.category_name FROM Product p " +
                     "LEFT JOIN ProductCategory c ON c.category_id=p.category_id WHERE p.ProductID=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapProduct(rs) : null;
            }
        }
    }

    public void saveProduct(Product p) throws SQLException {
        boolean isNew = p.getProductId() == null || p.getProductId().isEmpty();
        if (isNew) p.setProductId(DatabaseManager.newId());
        if (isNew) {
            String sql = "INSERT INTO Product(ProductID,product_code,Name,category_id,brand,unit,Description,Status,sales_rate) VALUES(?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, p.getProductId()); ps.setString(2, p.getProductCode());
                ps.setString(3, p.getName());       ps.setString(4, p.getCategoryId());
                ps.setString(5, p.getBrand());      ps.setString(6, p.getUnit());
                ps.setString(7, p.getDescription()); ps.setString(8, p.getStatus() != null ? p.getStatus() : "ACTIVE");
                ps.setInt(9, p.getSalesRate());
                ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE Product SET product_code=?,Name=?,category_id=?,brand=?,unit=?,Description=?,Status=? WHERE ProductID=?";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, p.getProductCode()); ps.setString(2, p.getName());
                ps.setString(3, p.getCategoryId()); ps.setString(4, p.getBrand());
                ps.setString(5, p.getUnit());        ps.setString(6, p.getDescription());
                ps.setString(7, p.getStatus());      ps.setString(8, p.getProductId());
                ps.executeUpdate();
            }
        }
        db.addSyncQueueEntry("Product", p.getProductId(), isNew ? "INSERT" : "UPDATE", null);
    }

    // ── ProductVariant ─────────────────────────────────────────────
    public List<ProductVariant> findVariantsByProduct(String productId) throws SQLException {
        List<ProductVariant> list = new ArrayList<>();
        String sql = "SELECT pv.*, p.Name as product_name, p.product_code FROM ProductVariant pv " +
                     "JOIN Product p ON p.ProductID=pv.product_id WHERE pv.product_id=? AND pv.status=1";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapVariant(rs)); }
        }
        return list;
    }

    /** Find variant by barcode — used by POS barcode scanner. */
    public ProductVariant findVariantByBarcode(String barcode) throws SQLException {
        String sql = "SELECT pv.*, p.Name as product_name, p.product_code FROM ProductVariant pv " +
                     "JOIN Product p ON p.ProductID=pv.product_id WHERE pv.barcode=? AND pv.status=1 AND p.Status='ACTIVE'";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, barcode);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapVariant(rs) : null; }
        }
    }

    public void saveVariant(ProductVariant v) throws SQLException {
        boolean isNew = v.getVariantId() == null || v.getVariantId().isEmpty();
        if (isNew) v.setVariantId(DatabaseManager.newId());
        if (isNew) {
            String sql = "INSERT INTO ProductVariant(variant_id,product_id,variant_name,barcode,status) VALUES(?,?,?,?,1)";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, v.getVariantId()); ps.setString(2, v.getProductId());
                ps.setString(3, v.getVariantName()); ps.setString(4, v.getBarcode());
                ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE ProductVariant SET variant_name=?,barcode=?,status=? WHERE variant_id=?";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, v.getVariantName()); ps.setString(2, v.getBarcode());
                ps.setInt(3, v.getStatus()); ps.setString(4, v.getVariantId());
                ps.executeUpdate();
            }
        }
        db.addSyncQueueEntry("ProductVariant", v.getVariantId(), isNew ? "INSERT" : "UPDATE", null);
    }

    // ── ProductPrice ───────────────────────────────────────────────
    /**
     * Get the current effective price for a variant in the default price list.
     * Price is frozen: start_time <= now AND (end_time IS NULL OR end_time > now).
     */
    public double getActivePrice(String variantId) throws SQLException {
        return getActivePrice(variantId, "PL-001"); // default standard retail price list
    }

    public double getActivePrice(String variantId, String priceListId) throws SQLException {
        String sql = "SELECT price FROM ProductPrice " +
                     "WHERE variant_id=? AND price_list_id=? " +
                     "  AND start_time <= datetime('now') " +
                     "  AND (end_time IS NULL OR end_time > datetime('now')) " +
                     "ORDER BY start_time DESC LIMIT 1";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, variantId); ps.setString(2, priceListId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("price") : 0.0;
            }
        }
    }

    public List<ProductPrice> findPriceHistory(String variantId) throws SQLException {
        List<ProductPrice> list = new ArrayList<>();
        String sql = "SELECT pp.*, pl.price_list_name FROM ProductPrice pp " +
                     "JOIN PriceList pl ON pl.price_list_id=pp.price_list_id " +
                     "WHERE pp.variant_id=? ORDER BY pp.start_time DESC";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, variantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductPrice p = new ProductPrice();
                    p.setPriceId(rs.getString("price_id"));
                    p.setVariantId(rs.getString("variant_id"));
                    p.setPriceListId(rs.getString("price_list_id"));
                    p.setPrice(rs.getDouble("price"));
                    p.setStartTime(rs.getString("start_time"));
                    p.setEndTime(rs.getString("end_time"));
                    list.add(p);
                }
            }
        }
        return list;
    }

    /** Set a new price for a variant (closes the current open-ended price). */
    public void setNewPrice(String variantId, String priceListId, double newPrice) throws SQLException {
        Connection conn = db.getConnection();
        conn.setAutoCommit(false);
        try {
            // Close the current open-ended price
            String closeOld = "UPDATE ProductPrice SET end_time=datetime('now') " +
                              "WHERE variant_id=? AND price_list_id=? AND (end_time IS NULL OR end_time > datetime('now'))";
            try (PreparedStatement ps = conn.prepareStatement(closeOld)) {
                ps.setString(1, variantId); ps.setString(2, priceListId); ps.executeUpdate();
            }
            // Insert new price record
            String insertNew = "INSERT INTO ProductPrice(price_id,variant_id,price_list_id,price,start_time,end_time) VALUES(?,?,?,?,datetime('now'),NULL)";
            String pid = DatabaseManager.newId();
            try (PreparedStatement ps = conn.prepareStatement(insertNew)) {
                ps.setString(1, pid); ps.setString(2, variantId);
                ps.setString(3, priceListId); ps.setDouble(4, newPrice);
                ps.executeUpdate();
            }
            conn.commit();
            db.addSyncQueueEntry("ProductPrice", pid, "INSERT", null);
        } catch (SQLException e) {
            conn.rollback(); throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── Mappers ────────────────────────────────────────────────────
    private ProductCategory mapCategory(ResultSet rs) throws SQLException {
        ProductCategory c = new ProductCategory();
        c.setCategoryId(rs.getString("category_id"));
        c.setCategoryName(rs.getString("category_name"));
        c.setDescription(rs.getString("description"));
        c.setStatus(rs.getInt("status"));
        return c;
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getString("ProductID"));
        p.setProductCode(rs.getString("product_code"));
        p.setName(rs.getString("Name"));
        p.setCategoryId(rs.getString("category_id"));
        p.setBrand(rs.getString("brand"));
        p.setUnit(rs.getString("unit"));
        p.setDescription(rs.getString("Description"));
        p.setStatus(rs.getString("Status"));
        p.setSalesRate(rs.getInt("sales_rate"));
        try { p.setCategoryName(rs.getString("category_name")); } catch (SQLException ignored) {}
        return p;
    }

    private ProductVariant mapVariant(ResultSet rs) throws SQLException {
        ProductVariant v = new ProductVariant();
        v.setVariantId(rs.getString("variant_id"));
        v.setProductId(rs.getString("product_id"));
        v.setVariantName(rs.getString("variant_name"));
        v.setBarcode(rs.getString("barcode"));
        v.setStatus(rs.getInt("status"));
        try { v.setProductName(rs.getString("product_name")); } catch (SQLException ignored) {}
        try { v.setProductCode(rs.getString("product_code")); } catch (SQLException ignored) {}
        return v;
    }
}

