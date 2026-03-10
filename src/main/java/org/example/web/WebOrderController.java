package org.example.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.db.PgDatabaseManager;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * Order & Payment REST API controller.
 * POS + Payment + Receipt + ReturnOrder
 */
public class WebOrderController {

    private static final PgDatabaseManager pg = PgDatabaseManager.getInstance();

    public static void register(Javalin app) {
        // Orders
        app.get("/api/orders",            WebOrderController::listOrders);
        app.get("/api/orders/{id}",       WebOrderController::getOrder);
        app.post("/api/orders",           WebOrderController::createOrder);
        app.put("/api/orders/{id}/status",WebOrderController::updateStatus);

        // Order items
        app.get("/api/orders/{id}/items", WebOrderController::getOrderItems);

        // Payments
        app.get("/api/payments",          WebOrderController::listPayments);
        app.post("/api/payments",         WebOrderController::createPayment);

        // Returns
        app.get("/api/returns",           WebOrderController::listReturns);
        app.post("/api/returns",          WebOrderController::createReturn);
    }

    // ═══════════════════════════════════════
    // ORDERS
    // ═══════════════════════════════════════

    private static void listOrders(Context ctx) {
        try (Connection conn = pg.getConnection()) {
            String sql = "SELECT o.\"orderId\", o.\"orderDate\", o.status, o.\"totalAmount\", " +
                    "o.\"discountAmount\", o.\"finalAmount\", o.\"taxRate\", o.\"taxAmount\", " +
                    "o.\"paymentStatus\", o.\"paidAmount\", o.\"debtAmount\", o.note, " +
                    "o.\"cashierId\", o.\"storeId\", o.\"customerId\" " +
                    "FROM \"Order\" o ORDER BY o.\"orderDate\" DESC LIMIT 100";
            List<Map<String, Object>> list = new ArrayList<>();
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
                while (rs.next()) list.add(orderMap(rs));
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void getOrder(Context ctx) {
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM \"Order\" WHERE \"orderId\"=?")) {
            ps.setString(1, ctx.pathParam("id"));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) ctx.json(orderMap(rs));
                else ctx.status(404).json(Map.of("error", "Not found"));
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private static void createOrder(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String orderId = "ORD-" + PgDatabaseManager.newId().substring(0, 8);
            String cashierId = (String) body.get("cashierId");
            String storeId = (String) body.get("storeId");
            String customerId = (String) body.get("customerId");
            double taxRate = body.containsKey("taxRate") ? ((Number) body.get("taxRate")).doubleValue() : 0.1;
            String note = (String) body.get("note");

            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items == null || items.isEmpty()) {
                ctx.status(400).json(Map.of("error", "Đơn hàng phải có ít nhất 1 sản phẩm"));
                return;
            }

            try (Connection conn = pg.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Calculate totals
                    double totalAmount = 0;
                    for (Map<String, Object> item : items) {
                        double qty = ((Number) item.get("quantity")).doubleValue();
                        double price = ((Number) item.get("unitPrice")).doubleValue();
                        totalAmount += qty * price;
                    }
                    double discountAmount = body.containsKey("discountAmount") ?
                            ((Number) body.get("discountAmount")).doubleValue() : 0;
                    double taxAmount = (totalAmount - discountAmount) * taxRate;
                    double finalAmount = totalAmount - discountAmount + taxAmount;

                    // Insert order
                    String orderSql = "INSERT INTO \"Order\"(\"orderId\",\"cashierId\",\"storeId\",\"customerId\"," +
                            "\"orderDate\",status,\"totalAmount\",\"discountAmount\",\"finalAmount\"," +
                            "\"taxRate\",\"taxAmount\",\"paymentStatus\",\"paidAmount\",\"debtAmount\",note) " +
                            "VALUES(?,?,?,?,NOW(),?,?,?,?,?,?,?,?,?,?)";
                    try (PreparedStatement ps = conn.prepareStatement(orderSql)) {
                        ps.setString(1, orderId);
                        ps.setString(2, cashierId);
                        ps.setString(3, storeId);
                        ps.setString(4, customerId);
                        ps.setString(5, "PENDING");
                        ps.setDouble(6, totalAmount);
                        ps.setDouble(7, discountAmount);
                        ps.setDouble(8, finalAmount);
                        ps.setDouble(9, taxRate);
                        ps.setDouble(10, taxAmount);
                        ps.setString(11, "UNPAID");
                        ps.setDouble(12, 0);
                        ps.setDouble(13, finalAmount);
                        ps.setString(14, note);
                        ps.executeUpdate();
                    }

                    // Insert items
                    String itemSql = "INSERT INTO OrderItem(\"orderItemId\",\"orderId\",\"productId\",\"variantId\"," +
                            "quantity,\"unitPrice\",subtotal) VALUES(?,?,?,?,?,?,?)";
                    try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                        for (Map<String, Object> item : items) {
                            String itemId = "OI-" + PgDatabaseManager.newId().substring(0, 8);
                            double qty = ((Number) item.get("quantity")).doubleValue();
                            double price = ((Number) item.get("unitPrice")).doubleValue();
                            ps.setString(1, itemId);
                            ps.setString(2, orderId);
                            ps.setString(3, (String) item.get("productId"));
                            ps.setString(4, (String) item.get("variantId"));
                            ps.setInt(5, (int) qty);
                            ps.setDouble(6, price);
                            ps.setDouble(7, qty * price);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }

                    conn.commit();
                    ctx.json(Map.of("success", true, "orderId", orderId,
                            "totalAmount", totalAmount, "finalAmount", finalAmount));
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                }
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void updateStatus(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String id = ctx.pathParam("id");
            String status = (String) body.get("status");

            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE \"Order\" SET status=?, last_modified=NOW() WHERE \"orderId\"=?")) {
                ps.setString(1, status);
                ps.setString(2, id);
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void getOrderItems(Context ctx) {
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT oi.*, p.\"Name\" as product_name, v.variant_name " +
                "FROM OrderItem oi " +
                "JOIN Product p ON p.\"ProductID\"=oi.\"productId\" " +
                "LEFT JOIN ProductVariant v ON v.variant_id=oi.\"variantId\" " +
                "WHERE oi.\"orderId\"=?")) {
            ps.setString(1, ctx.pathParam("id"));
            List<Map<String, Object>> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("orderItemId", rs.getString("orderItemId"));
                    m.put("productId", rs.getString("productId"));
                    m.put("productName", rs.getString("product_name"));
                    m.put("variantId", rs.getString("variantId"));
                    m.put("variantName", rs.getString("variant_name"));
                    m.put("quantity", rs.getInt("quantity"));
                    m.put("unitPrice", rs.getBigDecimal("unitPrice"));
                    m.put("subtotal", rs.getBigDecimal("subtotal"));
                    list.add(m);
                }
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════
    // PAYMENTS
    // ═══════════════════════════════════════

    private static void listPayments(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT p.*, o.\"finalAmount\" as order_total FROM Payment p " +
                "JOIN \"Order\" o ON o.\"orderId\"=p.\"orderId\" " +
                "ORDER BY p.\"paymentDate\" DESC LIMIT 100")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("paymentId", rs.getString("paymentId"));
                m.put("orderId", rs.getString("orderId"));
                m.put("paymentDate", rs.getString("paymentDate"));
                m.put("method", rs.getString("method"));
                m.put("amountPaid", rs.getBigDecimal("amountPaid"));
                m.put("status", rs.getString("status"));
                m.put("orderTotal", rs.getBigDecimal("order_total"));
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void createPayment(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String paymentId = "PAY-" + PgDatabaseManager.newId().substring(0, 8);
            String orderId = (String) body.get("orderId");
            String method = (String) body.getOrDefault("method", "CASH");
            double amountPaid = ((Number) body.get("amountPaid")).doubleValue();

            try (Connection conn = pg.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Insert payment
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO Payment(\"paymentId\",\"orderId\",method,\"amountPaid\",status) " +
                            "VALUES(?,?,?,?,'COMPLETED')")) {
                        ps.setString(1, paymentId);
                        ps.setString(2, orderId);
                        ps.setString(3, method);
                        ps.setDouble(4, amountPaid);
                        ps.executeUpdate();
                    }

                    // Cash payment details
                    if ("CASH".equals(method)) {
                        double cashReceived = body.containsKey("cashReceived") ?
                                ((Number) body.get("cashReceived")).doubleValue() : amountPaid;
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO CashPayment(\"paymentId\",\"cashReceived\",\"changeAmount\") VALUES(?,?,?)")) {
                            ps.setString(1, paymentId);
                            ps.setDouble(2, cashReceived);
                            ps.setDouble(3, cashReceived - amountPaid);
                            ps.executeUpdate();
                        }
                    }

                    // Update order payment status
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE \"Order\" SET \"paidAmount\" = \"paidAmount\" + ?, " +
                            "\"debtAmount\" = \"finalAmount\" - (\"paidAmount\" + ?), " +
                            "\"paymentStatus\" = CASE " +
                            "  WHEN \"paidAmount\" + ? >= \"finalAmount\" THEN 'PAID' " +
                            "  ELSE 'PARTIAL' END, " +
                            "status = CASE WHEN \"paidAmount\" + ? >= \"finalAmount\" THEN 'COMPLETED' ELSE status END " +
                            "WHERE \"orderId\"=?")) {
                        ps.setDouble(1, amountPaid);
                        ps.setDouble(2, amountPaid);
                        ps.setDouble(3, amountPaid);
                        ps.setDouble(4, amountPaid);
                        ps.setString(5, orderId);
                        ps.executeUpdate();
                    }

                    conn.commit();
                    ctx.json(Map.of("success", true, "paymentId", paymentId));
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                }
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════
    // RETURNS
    // ═══════════════════════════════════════

    private static void listReturns(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT * FROM ReturnOrder ORDER BY \"returnDate\" DESC LIMIT 100")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("returnId", rs.getString("returnId"));
                m.put("orderId", rs.getString("orderId"));
                m.put("returnDate", rs.getString("returnDate"));
                m.put("status", rs.getString("status"));
                m.put("quantity", rs.getInt("quantity"));
                m.put("reason", rs.getString("reason"));
                m.put("refundAmount", rs.getBigDecimal("refundAmount"));
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void createReturn(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String returnId = "RET-" + PgDatabaseManager.newId().substring(0, 8);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ReturnOrder(\"returnId\",\"orderId\",status,quantity,reason,\"refundAmount\") " +
                    "VALUES(?,?,'PENDING',?,?,?)")) {
                ps.setString(1, returnId);
                ps.setString(2, (String) body.get("orderId"));
                ps.setInt(3, ((Number) body.getOrDefault("quantity", 1)).intValue());
                ps.setString(4, (String) body.get("reason"));
                ps.setDouble(5, ((Number) body.getOrDefault("refundAmount", 0)).doubleValue());
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true, "returnId", returnId));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════

    private static Map<String, Object> orderMap(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", rs.getString("orderId"));
        m.put("orderDate", rs.getString("orderDate"));
        m.put("status", rs.getString("status"));
        m.put("totalAmount", rs.getBigDecimal("totalAmount"));
        m.put("discountAmount", rs.getBigDecimal("discountAmount"));
        m.put("finalAmount", rs.getBigDecimal("finalAmount"));
        m.put("taxRate", rs.getBigDecimal("taxRate"));
        m.put("taxAmount", rs.getBigDecimal("taxAmount"));
        m.put("paymentStatus", rs.getString("paymentStatus"));
        m.put("paidAmount", rs.getBigDecimal("paidAmount"));
        m.put("debtAmount", rs.getBigDecimal("debtAmount"));
        m.put("note", rs.getString("note"));
        m.put("cashierId", rs.getString("cashierId"));
        m.put("storeId", rs.getString("storeId"));
        m.put("customerId", rs.getString("customerId"));
        return m;
    }
}

