package org.example.dao;

import org.example.db.DatabaseManager;
import org.example.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Customer and loyalty account operations. */
public class CustomerDAO {

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ── Customer ───────────────────────────────────────────────────
    public Customer findByPhone(String phone) throws SQLException {
        String sql = "SELECT c.*, la.AccountID, la.CurrentPoints, mr.TierName " +
                     "FROM Customer c " +
                     "LEFT JOIN LoyaltyAccount la ON la.CustomerID=c.CustomerID " +
                     "LEFT JOIN MembershipRank mr ON mr.TierID=la.TierID " +
                     "WHERE c.PhoneNum=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapCustomer(rs) : null; }
        }
    }

    public Customer findById(String customerId) throws SQLException {
        String sql = "SELECT c.*, la.AccountID, la.CurrentPoints, mr.TierName " +
                     "FROM Customer c " +
                     "LEFT JOIN LoyaltyAccount la ON la.CustomerID=c.CustomerID " +
                     "LEFT JOIN MembershipRank mr ON mr.TierID=la.TierID " +
                     "WHERE c.CustomerID=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapCustomer(rs) : null; }
        }
    }

    public List<Customer> searchCustomers(String query) throws SQLException {
        List<Customer> list = new ArrayList<>();
        String like = "%" + (query == null ? "" : query) + "%";
        String sql = "SELECT c.*, la.AccountID, la.CurrentPoints, mr.TierName " +
                     "FROM Customer c " +
                     "LEFT JOIN LoyaltyAccount la ON la.CustomerID=c.CustomerID " +
                     "LEFT JOIN MembershipRank mr ON mr.TierID=la.TierID " +
                     "WHERE c.FullName LIKE ? OR c.PhoneNum LIKE ? ORDER BY c.FullName LIMIT 50";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, like); ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapCustomer(rs)); }
        }
        return list;
    }

    public void saveCustomer(Customer c) throws SQLException {
        boolean isNew = c.getCustomerId() == null || c.getCustomerId().isEmpty();
        if (isNew) c.setCustomerId(DatabaseManager.newId());
        if (isNew) {
            String sql = "INSERT INTO Customer(CustomerID,PhoneNum,FullName,Email) VALUES(?,?,?,?)";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, c.getCustomerId()); ps.setString(2, c.getPhoneNum());
                ps.setString(3, c.getFullName()); ps.setString(4, c.getEmail());
                ps.executeUpdate();
            }
            // Create a loyalty account for the new customer (Bronze tier)
            createLoyaltyAccount(c.getCustomerId());
        } else {
            String sql = "UPDATE Customer SET PhoneNum=?,FullName=?,Email=? WHERE CustomerID=?";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, c.getPhoneNum()); ps.setString(2, c.getFullName());
                ps.setString(3, c.getEmail()); ps.setString(4, c.getCustomerId());
                ps.executeUpdate();
            }
        }
        db.addSyncQueueEntry("Customer", c.getCustomerId(), isNew ? "INSERT" : "UPDATE", null);
    }

    private void createLoyaltyAccount(String customerId) throws SQLException {
        String accountId = DatabaseManager.newId();
        String sql = "INSERT OR IGNORE INTO LoyaltyAccount(AccountID,CustomerID,TierID,CurrentPoints,TotalPointsEarned) " +
                     "VALUES(?,'TIER-001',0,0)";
        // Use parameterized properly
        sql = "INSERT OR IGNORE INTO LoyaltyAccount(AccountID,CustomerID,TierID,CurrentPoints,TotalPointsEarned) VALUES(?,?,'TIER-001',0,0)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, accountId); ps.setString(2, customerId);
            ps.executeUpdate();
        }
    }

    // ── Loyalty Points ─────────────────────────────────────────────
    /**
     * Earn points after a completed order.
     * Formula: floor(finalAmount / 10000) * earnRate * earningMultiplier
     */
    public int earnPoints(Connection conn, String customerId, String orderId, double finalAmount) throws SQLException {
        // Get account + tier multiplier
        String getSql = "SELECT la.AccountID, la.CurrentPoints, mr.EarningMultipliers, " +
                        "(SELECT EarnRate FROM LoyaltyPointRule WHERE is_active=1 ORDER BY rowid LIMIT 1) as earnRate " +
                        "FROM LoyaltyAccount la JOIN MembershipRank mr ON mr.TierID=la.TierID " +
                        "WHERE la.CustomerID=?";
        String accountId; double multiplier; double earnRate; int currentPts;
        try (PreparedStatement ps = conn.prepareStatement(getSql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return 0;
                accountId = rs.getString("AccountID");
                currentPts = rs.getInt("CurrentPoints");
                multiplier = rs.getDouble("EarningMultipliers");
                earnRate = rs.getDouble("earnRate");
                if (earnRate == 0) earnRate = 1.0;
            }
        }
        int pts = (int) Math.floor(finalAmount / 10000.0 * earnRate * multiplier);
        if (pts <= 0) return 0;

        String upd = "UPDATE LoyaltyAccount SET CurrentPoints=CurrentPoints+?,TotalPointsEarned=TotalPointsEarned+? WHERE AccountID=?";
        try (PreparedStatement ps = conn.prepareStatement(upd)) {
            ps.setInt(1, pts); ps.setInt(2, pts); ps.setString(3, accountId); ps.executeUpdate();
        }
        // Log the transaction
        String txnId = DatabaseManager.newId();
        String txnSql = "INSERT INTO PointTransaction(TransactionID,AccountID,ReferenceOrderID,PointsAmount,TransactionType,note) VALUES(?,?,?,?,'EARN',?)";
        try (PreparedStatement ps = conn.prepareStatement(txnSql)) {
            ps.setString(1, txnId); ps.setString(2, accountId); ps.setString(3, orderId);
            ps.setInt(4, pts); ps.setString(5, "+" + pts + " pts from order");
            ps.executeUpdate();
        }
        // Auto-upgrade tier
        upgradeTierIfNeeded(conn, accountId, currentPts + pts);
        db.addSyncQueueEntry("LoyaltyAccount", accountId, "UPDATE", null);
        return pts;
    }

    /**
     * Redeem points. 1 point = redeemRate VND discount.
     */
    public double redeemPoints(Connection conn, String customerId, int pointsToRedeem, String orderId) throws SQLException {
        String getSql = "SELECT la.AccountID, la.CurrentPoints, r.RedeemRate " +
                        "FROM LoyaltyAccount la " +
                        "LEFT JOIN LoyaltyPointRule r ON r.is_active=1 " +
                        "WHERE la.CustomerID=? ORDER BY r.rowid LIMIT 1";
        String accountId; int currentPts; double redeemRate;
        try (PreparedStatement ps = conn.prepareStatement(getSql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return 0;
                accountId = rs.getString("AccountID");
                currentPts = rs.getInt("CurrentPoints");
                redeemRate = rs.getDouble("RedeemRate");
                if (redeemRate == 0) redeemRate = 100.0;
            }
        }
        int actualRedeem = Math.min(pointsToRedeem, currentPts);
        if (actualRedeem <= 0) return 0;
        double discount = actualRedeem * redeemRate;

        String upd = "UPDATE LoyaltyAccount SET CurrentPoints=CurrentPoints-? WHERE AccountID=?";
        try (PreparedStatement ps = conn.prepareStatement(upd)) {
            ps.setInt(1, actualRedeem); ps.setString(2, accountId); ps.executeUpdate();
        }
        String txnId = DatabaseManager.newId();
        String txnSql = "INSERT INTO PointTransaction(TransactionID,AccountID,ReferenceOrderID,PointsAmount,TransactionType,note) VALUES(?,?,?,?,'REDEEM',?)";
        try (PreparedStatement ps = conn.prepareStatement(txnSql)) {
            ps.setString(1, txnId); ps.setString(2, accountId); ps.setString(3, orderId);
            ps.setInt(4, -actualRedeem); ps.setString(5, "Redeemed " + actualRedeem + " pts");
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("LoyaltyAccount", accountId, "UPDATE", null);
        return discount;
    }

    private void upgradeTierIfNeeded(Connection conn, String accountId, int totalPoints) throws SQLException {
        String sql = "SELECT TierID FROM MembershipRank WHERE MinPointsRequired<=? ORDER BY MinPointsRequired DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, totalPoints);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String upd = "UPDATE LoyaltyAccount SET TierID=? WHERE AccountID=?";
                    try (PreparedStatement pu = conn.prepareStatement(upd)) {
                        pu.setString(1, rs.getString("TierID")); pu.setString(2, accountId);
                        pu.executeUpdate();
                    }
                }
            }
        }
    }

    public List<Object[]> getPointHistory(String customerId) throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT pt.TransactionType, pt.PointsAmount, pt.TimeStamp, pt.note, pt.ReferenceOrderID " +
                     "FROM PointTransaction pt JOIN LoyaltyAccount la ON la.AccountID=pt.AccountID " +
                     "WHERE la.CustomerID=? ORDER BY pt.TimeStamp DESC LIMIT 50";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                        rs.getString("TransactionType"), rs.getInt("PointsAmount"),
                        rs.getString("TimeStamp"), rs.getString("note"), rs.getString("ReferenceOrderID")
                    });
                }
            }
        }
        return list;
    }

    private Customer mapCustomer(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setCustomerId(rs.getString("CustomerID"));
        c.setPhoneNum(rs.getString("PhoneNum"));
        c.setFullName(rs.getString("FullName"));
        c.setEmail(rs.getString("Email"));
        c.setRegistrationDate(rs.getString("RegistrationDate"));
        try { c.setAccountId(rs.getString("AccountID")); } catch (SQLException ignored){}
        try { c.setCurrentPoints(rs.getInt("CurrentPoints")); } catch (SQLException ignored){}
        try { c.setTierName(rs.getString("TierName")); } catch (SQLException ignored){}
        return c;
    }
}

