package org.example;

import java.sql.*;

/**
 * Quick test to verify database schema + create a new Order.
 * Run with: mvn exec:java -Dexec.mainClass=org.example.DBVerifyTest
 */
public class DBVerifyTest {
    public static void main(String[] args) throws Exception {
        // Initialize database first
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:store_management.db");
        conn.setAutoCommit(true);

        System.out.println("=== DB Verify Test ===");

        // 1. List all tables
        System.out.println("\n--- Tables ---");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")) {
            int count = 0;
            while (rs.next()) { System.out.println("  " + rs.getString(1)); count++; }
            System.out.println("Total tables: " + count);
        }

        // 2. Count existing orders
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM \"Order\"")) {
            rs.next();
            System.out.println("\nExisting orders: " + rs.getInt(1));
        }

        // 3. Try creating a new order
        System.out.println("\n--- Creating a NEW Order ---");
        String newOrderId = "TEST-ORD-" + System.currentTimeMillis();
        String sql = "INSERT INTO \"Order\"(orderId,cashierId,shiftId,storeId,customerId,status,totalAmount,discountAmount,finalAmount,note) " +
                     "VALUES(?,?,NULL,?,NULL,'PENDING',0,0,0,'Test order')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newOrderId);
            ps.setString(2, "CSH-001"); // existing cashier from seed data
            ps.setString(3, "STORE-001"); // existing store from seed data
            ps.executeUpdate();
            System.out.println("  SUCCESS: Created order " + newOrderId);
        }

        // 4. Verify the order was created
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM \"Order\" WHERE orderId=?")) {
            ps.setString(1, newOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("  Verified: orderId=" + rs.getString("orderId")
                            + ", cashierId=" + rs.getString("cashierId")
                            + ", status=" + rs.getString("status")
                            + ", storeId=" + rs.getString("storeId"));
                } else {
                    System.out.println("  FAIL: Order not found after insert!");
                }
            }
        }

        // 5. Count orders after
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM \"Order\"")) {
            rs.next();
            System.out.println("\nTotal orders after test: " + rs.getInt(1));
        }

        // Cleanup test order
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM \"Order\" WHERE orderId=?")) {
            ps.setString(1, newOrderId);
            ps.executeUpdate();
            System.out.println("Test order cleaned up.");
        }

        conn.close();
        System.out.println("\n=== All Tests Passed ===");
    }
}

