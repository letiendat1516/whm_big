package org.example.dao;

import org.example.db.DatabaseManager;
import org.example.model.Promotion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Promotion and campaign operations. */
public class PromotionDAO {

    private final DatabaseManager db = DatabaseManager.getInstance();

    /** Get all active promotions for the current datetime. */
    public List<Promotion> findActivePromotions() throws SQLException {
        List<Promotion> list = new ArrayList<>();
        String sql = "SELECT p.*, c.Name as campaign_name FROM Promotion p " +
                     "JOIN Campaign c ON c.CampaignID=p.CampaignID " +
                     "WHERE p.IsActive=1 AND c.IsActive=1 " +
                     "  AND c.StartDate <= date('now') AND c.EndDate >= date('now') " +
                     "  AND (p.ExpiryDate IS NULL OR p.ExpiryDate >= date('now')) " +
                     "  AND (p.MaxUsageCount=0 OR p.CurrentUsageCount < p.MaxUsageCount) " +
                     "ORDER BY p.Priority DESC";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapPromotion(rs));
        }
        return list;
    }

    /** Find promotion by voucher code. */
    public Promotion findByVoucherCode(String code) throws SQLException {
        String sql = "SELECT p.*, c.Name as campaign_name FROM Promotion p " +
                     "JOIN Campaign c ON c.CampaignID=p.CampaignID " +
                     "WHERE p.VoucherCode=? AND p.IsActive=1 " +
                     "  AND c.IsActive=1 AND c.StartDate <= date('now') AND c.EndDate >= date('now') " +
                     "  AND (p.ExpiryDate IS NULL OR p.ExpiryDate >= date('now')) " +
                     "  AND (p.MaxUsageCount=0 OR p.CurrentUsageCount < p.MaxUsageCount)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapPromotion(rs) : null; }
        }
    }

    /** Calculate discount amount based on promo type and order total. */
    public double calculateDiscount(Promotion promo, double orderTotal, int itemCount) {
        if (promo == null) return 0;
        try {
            return switch (promo.getPromoType()) {
                case "PERCENT_DISCOUNT" -> {
                    double pct = Double.parseDouble(promo.getRuleDefinition());
                    yield orderTotal * (pct / 100.0);
                }
                case "FIXED_DISCOUNT" -> {
                    double fixed = Double.parseDouble(promo.getRuleDefinition());
                    yield Math.min(fixed, orderTotal);
                }
                case "BUY_X_GET_Y" -> 0; // handled at item level
                default -> 0;
            };
        } catch (Exception e) { return 0; }
    }

    /** Log promotion application and increment usage counter. */
    public void applyPromotion(Connection conn, String promotionId, String orderId, double discountAmount) throws SQLException {
        String logId = DatabaseManager.newId();
        String logSql = "INSERT INTO PromotionApplicationLog(LogID,PromotionID,ReferenceOrderID,AppliedDiscountAmount,SyncStatus) VALUES(?,?,?,?,'PENDING')";
        try (PreparedStatement ps = conn.prepareStatement(logSql)) {
            ps.setString(1, logId); ps.setString(2, promotionId);
            ps.setString(3, orderId); ps.setDouble(4, discountAmount);
            ps.executeUpdate();
        }
        String inc = "UPDATE Promotion SET CurrentUsageCount=CurrentUsageCount+1 WHERE PromotionID=?";
        try (PreparedStatement ps = conn.prepareStatement(inc)) {
            ps.setString(1, promotionId); ps.executeUpdate();
        }
        db.addSyncQueueEntry("PromotionApplicationLog", logId, "INSERT", null);
    }

    public List<Promotion> findAll() throws SQLException {
        List<Promotion> list = new ArrayList<>();
        String sql = "SELECT p.*, c.Name as campaign_name FROM Promotion p " +
                     "JOIN Campaign c ON c.CampaignID=p.CampaignID ORDER BY c.Name, p.Priority DESC";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapPromotion(rs));
        }
        return list;
    }

    private Promotion mapPromotion(ResultSet rs) throws SQLException {
        Promotion p = new Promotion();
        p.setPromotionId(rs.getString("PromotionID"));
        p.setCampaignId(rs.getString("CampaignID"));
        p.setPromoType(rs.getString("PromoType"));
        p.setName(rs.getString("Name"));
        try { p.setPriority(rs.getInt("Priority")); } catch (SQLException ignored){}
        p.setRuleDefinition(rs.getString("RuleDefinition"));
        p.setVoucherCode(rs.getString("VoucherCode"));
        p.setMaxUsageCount(rs.getInt("MaxUsageCount"));
        p.setCurrentUsageCount(rs.getInt("CurrentUsageCount"));
        p.setExpiryDate(rs.getString("ExpiryDate"));
        p.setIsActive(rs.getInt("IsActive"));
        try { p.setCampaignName(rs.getString("campaign_name")); } catch (SQLException ignored){}
        return p;
    }
}

