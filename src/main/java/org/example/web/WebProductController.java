package org.example.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.db.PgDatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * Product REST API controller.
 * Supports: Products, Variants, PriceList, Prices, Categories
 */
public class WebProductController {

    private static final PgDatabaseManager pg = PgDatabaseManager.getInstance();

    public static void register(Javalin app) {
        // Products
        app.get("/api/products",          WebProductController::listProducts);
        app.get("/api/products/{id}",     WebProductController::getProduct);
        app.post("/api/products",         WebProductController::createProduct);
        app.put("/api/products/{id}",     WebProductController::updateProduct);
        app.delete("/api/products/{id}",  WebProductController::deleteProduct);

        // Categories CRUD
        app.get("/api/categories",           WebProductController::listCategories);
        app.post("/api/categories",          WebProductController::createCategory);
        app.put("/api/categories/{id}",      WebProductController::updateCategory);
        app.delete("/api/categories/{id}",   WebProductController::deleteCategory);

        // Variants
        app.get("/api/variants",          WebProductController::listVariants);
        app.get("/api/variants/product/{productId}", WebProductController::variantsByProduct);

        // Prices
        app.get("/api/prices",            WebProductController::listPrices);
        app.get("/api/pricelists",        WebProductController::listPriceLists);
    }

    private static void listProducts(Context ctx) {
        try (Connection conn = pg.getConnection();
             Statement s = conn.createStatement()) {
            String search = ctx.queryParam("search");
            String sql = "SELECT p.\"ProductID\", p.product_code, p.\"Name\", p.brand, p.unit, " +
                    "p.\"Status\", p.sales_rate, c.category_name, p.category_id " +
                    "FROM Product p LEFT JOIN ProductCategory c ON p.category_id=c.category_id ";
            if (search != null && !search.isBlank()) {
                sql += "WHERE LOWER(p.\"Name\") LIKE '%" + search.toLowerCase().replace("'", "") + "%' " +
                       "OR LOWER(p.product_code) LIKE '%" + search.toLowerCase().replace("'", "") + "%' ";
            }
            sql += "ORDER BY p.product_code";

            List<Map<String, Object>> list = new ArrayList<>();
            try (ResultSet rs = s.executeQuery(sql)) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("productId", rs.getString("ProductID"));
                    m.put("productCode", rs.getString("product_code"));
                    m.put("name", rs.getString("Name"));
                    m.put("brand", rs.getString("brand"));
                    m.put("unit", rs.getString("unit"));
                    m.put("status", rs.getString("Status"));
                    m.put("salesRate", rs.getInt("sales_rate"));
                    m.put("categoryName", rs.getString("category_name"));
                    m.put("categoryId", rs.getString("category_id"));
                    list.add(m);
                }
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void getProduct(Context ctx) {
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM Product WHERE \"ProductID\"=?")) {
            ps.setString(1, ctx.pathParam("id"));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ctx.json(productMap(rs));
                } else {
                    ctx.status(404).json(Map.of("error", "Product not found"));
                }
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void createProduct(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String id = "PROD-" + PgDatabaseManager.newId().substring(0, 6);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Product(\"ProductID\",product_code,\"Name\",category_id,brand,unit,\"Description\",\"Status\",sales_rate) " +
                    "VALUES(?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, id);
                ps.setString(2, (String) body.get("productCode"));
                ps.setString(3, (String) body.get("name"));
                ps.setString(4, (String) body.get("categoryId"));
                ps.setString(5, (String) body.get("brand"));
                ps.setString(6, (String) body.get("unit"));
                ps.setString(7, (String) body.get("description"));
                ps.setString(8, (String) body.getOrDefault("status", "ACTIVE"));
                ps.setInt(9, ((Number) body.getOrDefault("salesRate", 0)).intValue());
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true, "productId", id));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void updateProduct(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Product SET product_code=?, \"Name\"=?, category_id=?, brand=?, unit=?, " +
                    "\"Description\"=?, \"Status\"=?, sales_rate=?, \"UpdatedAt\"=NOW() WHERE \"ProductID\"=?")) {
                ps.setString(1, (String) body.get("productCode"));
                ps.setString(2, (String) body.get("name"));
                ps.setString(3, (String) body.get("categoryId"));
                ps.setString(4, (String) body.get("brand"));
                ps.setString(5, (String) body.get("unit"));
                ps.setString(6, (String) body.get("description"));
                ps.setString(7, (String) body.getOrDefault("status", "ACTIVE"));
                ps.setInt(8, ((Number) body.getOrDefault("salesRate", 0)).intValue());
                ps.setString(9, ctx.pathParam("id"));
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void deleteProduct(Context ctx) {
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM Product WHERE \"ProductID\"=?")) {
            ps.setString(1, ctx.pathParam("id"));
            ps.executeUpdate();
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void listCategories(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT category_id, category_name, description FROM ProductCategory ORDER BY category_name")) {
            List<Map<String, String>> list = new ArrayList<>();
            while (rs.next()) {
                list.add(Map.of(
                    "categoryId", rs.getString("category_id"),
                    "categoryName", rs.getString("category_name"),
                    "description", Optional.ofNullable(rs.getString("description")).orElse("")
                ));
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void createCategory(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String id = "CAT-" + PgDatabaseManager.newId().substring(0, 6);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ProductCategory(category_id,category_name,description) VALUES(?,?,?)")) {
                ps.setString(1, id);
                ps.setString(2, (String) body.get("categoryName"));
                ps.setString(3, (String) body.get("description"));
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true, "categoryId", id));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void updateCategory(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE ProductCategory SET category_name=?,description=? WHERE category_id=?")) {
                ps.setString(1, (String) body.get("categoryName"));
                ps.setString(2, (String) body.get("description"));
                ps.setString(3, ctx.pathParam("id"));
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void deleteCategory(Context ctx) {
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM ProductCategory WHERE category_id=?")) {
            ps.setString(1, ctx.pathParam("id"));
            ps.executeUpdate();
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void listVariants(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT v.variant_id, v.product_id, v.variant_name, v.barcode, v.status, " +
                "p.\"Name\" as product_name FROM ProductVariant v " +
                "JOIN Product p ON p.\"ProductID\"=v.product_id ORDER BY v.variant_name")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("variantId", rs.getString("variant_id"));
                m.put("productId", rs.getString("product_id"));
                m.put("variantName", rs.getString("variant_name"));
                m.put("barcode", rs.getString("barcode"));
                m.put("status", rs.getInt("status"));
                m.put("productName", rs.getString("product_name"));
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void variantsByProduct(Context ctx) {
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT v.*, pp.price FROM ProductVariant v " +
                "LEFT JOIN ProductPrice pp ON pp.variant_id=v.variant_id AND pp.end_time IS NULL " +
                "WHERE v.product_id=? AND v.status=1")) {
            ps.setString(1, ctx.pathParam("productId"));
            List<Map<String, Object>> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("variantId", rs.getString("variant_id"));
                    m.put("variantName", rs.getString("variant_name"));
                    m.put("barcode", rs.getString("barcode"));
                    m.put("price", rs.getBigDecimal("price"));
                    list.add(m);
                }
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void listPrices(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT pp.price_id, pp.variant_id, v.variant_name, p.\"Name\" as product_name, " +
                "pp.price, pp.start_time, pp.end_time, pl.price_list_name " +
                "FROM ProductPrice pp " +
                "JOIN ProductVariant v ON v.variant_id=pp.variant_id " +
                "JOIN Product p ON p.\"ProductID\"=v.product_id " +
                "JOIN PriceList pl ON pl.price_list_id=pp.price_list_id " +
                "ORDER BY p.\"Name\", v.variant_name")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("priceId", rs.getString("price_id"));
                m.put("variantId", rs.getString("variant_id"));
                m.put("variantName", rs.getString("variant_name"));
                m.put("productName", rs.getString("product_name"));
                m.put("price", rs.getBigDecimal("price"));
                m.put("startTime", rs.getString("start_time"));
                m.put("endTime", rs.getString("end_time"));
                m.put("priceListName", rs.getString("price_list_name"));
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void listPriceLists(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT * FROM PriceList ORDER BY price_list_name")) {
            List<Map<String, String>> list = new ArrayList<>();
            while (rs.next()) {
                list.add(Map.of(
                    "priceListId", rs.getString("price_list_id"),
                    "priceListName", rs.getString("price_list_name"),
                    "status", rs.getString("status")
                ));
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static Map<String, Object> productMap(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("productId", rs.getString("ProductID"));
        m.put("productCode", rs.getString("product_code"));
        m.put("name", rs.getString("Name"));
        m.put("brand", rs.getString("brand"));
        m.put("unit", rs.getString("unit"));
        m.put("status", rs.getString("Status"));
        return m;
    }
}

