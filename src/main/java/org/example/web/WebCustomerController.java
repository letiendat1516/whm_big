package org.example.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.db.PgDatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * Customer & CRM REST API controller.
 */
public class WebCustomerController {

    private static final PgDatabaseManager pg = PgDatabaseManager.getInstance();

    public static void register(Javalin app) {
        app.get("/api/customers",         WebCustomerController::listCustomers);
        app.get("/api/customers/{id}",    WebCustomerController::getCustomer);
        app.post("/api/customers",        WebCustomerController::createCustomer);
        app.put("/api/customers/{id}",    WebCustomerController::updateCustomer);
        app.delete("/api/customers/{id}", WebCustomerController::deleteCustomer);

        // Promotions
        app.get("/api/promotions",        WebCustomerController::listPromotions);
        app.get("/api/campaigns",         WebCustomerController::listCampaigns);

        // Loyalty
        app.get("/api/loyalty/accounts",  WebCustomerController::listLoyaltyAccounts);
        app.get("/api/loyalty/tiers",     WebCustomerController::listTiers);
    }

    private static void listCustomers(Context ctx) {
        try (Connection conn = pg.getConnection()) {
            String search = ctx.queryParam("search");
            String sql = "SELECT c.*, la.\"CurrentPoints\", la.\"TotalPointsEarned\", mr.\"TierName\" " +
                    "FROM Customer c " +
                    "LEFT JOIN LoyaltyAccount la ON la.\"CustomerID\"=c.\"CustomerID\" " +
                    "LEFT JOIN MembershipRank mr ON mr.\"TierID\"=la.\"TierID\" ";
            if (search != null && !search.isBlank()) {
                String s = search.toLowerCase().replace("'", "");
                sql += "WHERE LOWER(c.\"FullName\") LIKE '%" + s + "%' " +
                       "OR c.\"PhoneNum\" LIKE '%" + s + "%' ";
            }
            sql += "ORDER BY c.\"FullName\"";

            List<Map<String, Object>> list = new ArrayList<>();
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("customerId", rs.getString("CustomerID"));
                    m.put("phoneNum", rs.getString("PhoneNum"));
                    m.put("fullName", rs.getString("FullName"));
                    m.put("email", rs.getString("Email"));
                    m.put("registrationDate", rs.getString("RegistrationDate"));
                    m.put("currentPoints", rs.getObject("CurrentPoints"));
                    m.put("totalPointsEarned", rs.getObject("TotalPointsEarned"));
                    m.put("tierName", rs.getString("TierName"));
                    list.add(m);
                }
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void getCustomer(Context ctx) {
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM Customer WHERE \"CustomerID\"=?")) {
            ps.setString(1, ctx.pathParam("id"));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("customerId", rs.getString("CustomerID"));
                    m.put("phoneNum", rs.getString("PhoneNum"));
                    m.put("fullName", rs.getString("FullName"));
                    m.put("email", rs.getString("Email"));
                    ctx.json(m);
                } else {
                    ctx.status(404).json(Map.of("error", "Not found"));
                }
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void createCustomer(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String id = "CUST-" + PgDatabaseManager.newId().substring(0, 6);
            try (Connection conn = pg.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO Customer(\"CustomerID\",\"PhoneNum\",\"FullName\",\"Email\") VALUES(?,?,?,?)")) {
                        ps.setString(1, id);
                        ps.setString(2, (String) body.get("phoneNum"));
                        ps.setString(3, (String) body.get("fullName"));
                        ps.setString(4, (String) body.get("email"));
                        ps.executeUpdate();
                    }
                    // Auto-create loyalty account
                    String accId = "ACC-" + PgDatabaseManager.newId().substring(0, 6);
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO LoyaltyAccount(\"AccountID\",\"CustomerID\",\"TierID\") VALUES(?,?,?)")) {
                        ps.setString(1, accId);
                        ps.setString(2, id);
                        ps.setString(3, "TIER-001");
                        ps.executeUpdate();
                    }
                    conn.commit();
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                }
            }
            ctx.json(Map.of("success", true, "customerId", id));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void updateCustomer(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Customer SET \"PhoneNum\"=?, \"FullName\"=?, \"Email\"=?, " +
                    "last_modified=NOW() WHERE \"CustomerID\"=?")) {
                ps.setString(1, (String) body.get("phoneNum"));
                ps.setString(2, (String) body.get("fullName"));
                ps.setString(3, (String) body.get("email"));
                ps.setString(4, ctx.pathParam("id"));
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void deleteCustomer(Context ctx) {
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM Customer WHERE \"CustomerID\"=?")) {
            ps.setString(1, ctx.pathParam("id"));
            ps.executeUpdate();
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void listPromotions(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT p.*, c.\"Name\" as campaign_name FROM Promotion p " +
                "JOIN Campaign c ON c.\"CampaignID\"=p.\"CampaignID\" " +
                "ORDER BY p.\"Priority\"")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("promotionId", rs.getString("PromotionID"));
                m.put("campaignName", rs.getString("campaign_name"));
                m.put("promoType", rs.getString("PromoType"));
                m.put("name", rs.getString("Name"));
                m.put("priority", rs.getInt("Priority"));
                m.put("voucherCode", rs.getString("VoucherCode"));
                m.put("maxUsageCount", rs.getInt("MaxUsageCount"));
                m.put("currentUsageCount", rs.getInt("CurrentUsageCount"));
                m.put("expiryDate", rs.getString("ExpiryDate"));
                m.put("isActive", rs.getInt("IsActive") == 1);
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void listCampaigns(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT * FROM Campaign ORDER BY \"StartDate\" DESC")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("campaignId", rs.getString("CampaignID"));
                m.put("name", rs.getString("Name"));
                m.put("description", rs.getString("Description"));
                m.put("startDate", rs.getString("StartDate"));
                m.put("endDate", rs.getString("EndDate"));
                m.put("isActive", rs.getInt("IsActive") == 1);
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void listLoyaltyAccounts(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT la.*, c.\"FullName\", c.\"PhoneNum\", mr.\"TierName\" " +
                "FROM LoyaltyAccount la " +
                "JOIN Customer c ON c.\"CustomerID\"=la.\"CustomerID\" " +
                "JOIN MembershipRank mr ON mr.\"TierID\"=la.\"TierID\" " +
                "ORDER BY la.\"TotalPointsEarned\" DESC")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("accountId", rs.getString("AccountID"));
                m.put("customerId", rs.getString("CustomerID"));
                m.put("fullName", rs.getString("FullName"));
                m.put("phoneNum", rs.getString("PhoneNum"));
                m.put("tierName", rs.getString("TierName"));
                m.put("currentPoints", rs.getInt("CurrentPoints"));
                m.put("totalPointsEarned", rs.getInt("TotalPointsEarned"));
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void listTiers(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT * FROM MembershipRank ORDER BY \"MinPointsRequired\"")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("tierId", rs.getString("TierID"));
                m.put("tierName", rs.getString("TierName"));
                m.put("minPoints", rs.getInt("MinPointsRequired"));
                m.put("multiplier", rs.getBigDecimal("EarningMultipliers"));
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }
}

