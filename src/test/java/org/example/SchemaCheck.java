package org.example;

import java.sql.*;

public class SchemaCheck {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:sqlite:store_management.db";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // 1. Check Order schema
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(\"Order\")");
            System.out.println("=== Order table columns ===");
            while (rs.next()) {
                System.out.println("  " + rs.getString("name") + " : " + rs.getString("type"));
            }
            rs.close();

            // 2. Test exact same SQL as OrderDAO.createOrder()
            System.out.println("\n=== Test DAO-style INSERT ===");
            String sql = "INSERT INTO \"Order\"(orderId,cashierId,shiftId,storeId,customerId,status," +
                         "totalAmount,discountAmount,finalAmount,taxRate,taxAmount," +
                         "paymentStatus,paidAmount,debtAmount,note) " +
                         "VALUES(?,?,?,?,?,'PENDING',0,0,0,?,0,'UNPAID',0,0,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "TEST-DAO-001");
                ps.setString(2, "CSH-001");
                ps.setString(3, null);
                ps.setString(4, "STORE-001");
                ps.setString(5, null);
                ps.setDouble(6, 0.1);
                ps.setString(7, "Test note");
                ps.executeUpdate();
                System.out.println("SUCCESS: DAO-style INSERT worked!");
            }

            // 3. Verify the inserted row
            rs = stmt.executeQuery("SELECT orderId, taxRate, paymentStatus, debtAmount FROM \"Order\" WHERE orderId='TEST-DAO-001'");
            if (rs.next()) {
                System.out.println("  orderId=" + rs.getString("orderId") +
                    " taxRate=" + rs.getDouble("taxRate") +
                    " paymentStatus=" + rs.getString("paymentStatus") +
                    " debtAmount=" + rs.getDouble("debtAmount"));
            }
            rs.close();

            // 4. Cleanup
            stmt.execute("DELETE FROM \"Order\" WHERE orderId='TEST-DAO-001'");
            System.out.println("\n=== ALL TESTS PASSED ===");
        }
    }
}

