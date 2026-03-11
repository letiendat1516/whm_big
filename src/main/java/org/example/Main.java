package org.example;

import org.example.db.DatabaseManager;
import org.example.model.Account;
import org.example.service.PasswordUtil;
import org.example.ui.LoginDialog;
import org.example.ui.MainWindow;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * WHM — Retail Store Management System
 *
 * Offline-First desktop app (SQLite + Swing).
 * Modules:
 *   0. Auth     (module0_auth.sql)  — Login + RBAC
 *   1. POS      (module1_pos.sql)
 *   2. Inventory(module2_inventory.sql)
 *   3. Product  (module3_product.sql)
 *   4. CRM      (module4_crm_promotion.sql)
 *   5. HR/Shift (module5_hr_shift.sql)
 */
public class Main {

    private static final List<String> SQL_SCRIPTS = Arrays.asList(
            "init_database.sql",          // 0. metadata, sync_queue, app_config
            "module0_auth.sql",           // 1. Account, Role, LoginLog (NO FK to other tables)
            "module3_product.sql",        // 2. Product, Category, Variant, Price
            "module5_hr_shift.sql",       // 3. Employee, Store, Shift, Payroll
            "module4_crm_promotion.sql",  // 4. Customer, Loyalty, Campaign, Promotion
            "module2_inventory.sql",      // 5. Warehouse, Stock, Documents
            "module1_pos.sql",            // 6. Order, Payment, Receipt (refs Customer, Employee)
            "seed_data.sql",              // 7. Cross-module seed data (refs Order + CRM)
            "sqlite_sync_triggers.sql"    // 8. Sync queue triggers (auto-enqueue changes for cloud sync)
    );

    public static void main(String[] args) {
        // 1. Initialize database
        try {
            initDatabase();
            seedDefaultPasswords();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Lỗi khởi tạo cơ sở dữ liệu:\n" + e.getMessage(),
                    "Lỗi nghiêm trọng", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // 2. Show Login Dialog
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            LoginDialog loginDialog = new LoginDialog(null);
            loginDialog.setVisible(true);  // blocks until login or close

            Account account = loginDialog.getLoggedInAccount();
            if (account == null) {
                System.exit(0);  // User cancelled
            }

            // 3. Launch MainWindow with RBAC
            MainWindow window = new MainWindow(account);
            window.setVisible(true);
        });
    }

    private static void initDatabase() throws Exception {
        System.out.println("=== WHM — Initializing Database ===");
        Connection conn = DatabaseManager.getInstance().getConnection();
        conn.setAutoCommit(true);

        // Performance settings (no FK enforcement yet — we load data first)
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys  = OFF");  // OFF during bulk load
            stmt.execute("PRAGMA journal_mode  = WAL");
            stmt.execute("PRAGMA synchronous   = NORMAL");
            stmt.execute("PRAGMA cache_size    = -32000");
        }

        // Run each SQL script
        for (String scriptName : SQL_SCRIPTS) {
            loadSqlScript(conn, scriptName);
        }

        // Re-enable FK enforcement for normal runtime
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        // Tell DatabaseManager FK is now on so future getConnection() calls keep it on
        DatabaseManager.getInstance().enableForeignKeys();
        System.out.println("=== Database ready ===");
    }

    private static void loadSqlScript(Connection conn, String scriptName) throws Exception {
        InputStream is = Main.class.getClassLoader().getResourceAsStream(scriptName);
        if (is == null) {
            System.err.println("[SKIP] " + scriptName + " not found.");
            return;
        }

        List<String> statements = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        int beginEndDepth = 0;  // tracks BEGIN...END nesting for TRIGGER bodies

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Strip inline line comment suffixes (but keep the line content before --)
                // We keep the line so that multi-line strings across comments still work
                String trimLine = line.trim();

                // Count BEGIN and END to track trigger body depth (case-insensitive)
                String upper = trimLine.toUpperCase();
                // BEGIN keyword increases depth (only when not in a string/comment)
                if (upper.equals("BEGIN") || upper.startsWith("BEGIN ") || upper.startsWith("BEGIN\t")) {
                    beginEndDepth++;
                }

                current.append(line).append('\n');

                // END; inside a trigger body closes it
                if (beginEndDepth > 0 && (upper.equals("END;") || upper.equals("END ;") || upper.startsWith("END;"))) {
                    beginEndDepth--;
                    if (beginEndDepth == 0) {
                        // The full trigger statement is complete at the END;
                        String stmt = current.toString().trim();
                        // Remove trailing semicolon from the collected block — JDBC executes without it
                        if (stmt.endsWith(";")) stmt = stmt.substring(0, stmt.length() - 1).trim();
                        if (!stmt.isEmpty()) statements.add(stmt);
                        current.setLength(0);
                    }
                } else if (beginEndDepth == 0 && trimLine.endsWith(";")) {
                    // Normal statement ends with semicolon on this line
                    String stmt = current.toString().trim();
                    if (stmt.endsWith(";")) stmt = stmt.substring(0, stmt.length() - 1).trim();
                    if (!stmt.isEmpty()) statements.add(stmt);
                    current.setLength(0);
                }
                // else: accumulate more lines
            }
        }
        // Flush any trailing statement without final semicolon
        String trailing = current.toString().trim();
        if (!trailing.isEmpty()) {
            if (trailing.endsWith(";")) trailing = trailing.substring(0, trailing.length() - 1).trim();
            if (!trailing.isEmpty()) statements.add(trailing);
        }

        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (trimmed.isEmpty()) continue;
                // Skip pure-comment statements
                boolean allComment = trimmed.lines()
                    .allMatch(l -> { String t = l.trim(); return t.isEmpty() || t.startsWith("--"); });
                if (allComment) continue;
                try {
                    // Skip PRAGMA — already handled by initDatabase
                    if (trimmed.toUpperCase().startsWith("PRAGMA")) continue;
                    stmt.execute(trimmed);
                } catch (SQLException e) {
                    String msg = e.getMessage();
                    if (msg != null && (
                            msg.contains("already exists") ||
                            msg.contains("UNIQUE constraint") ||
                            msg.contains("duplicate column") ||
                            msg.contains("no transaction is active"))) {
                        // Idempotent re-run — safe to skip
                    } else {
                        System.err.printf("[WARN] %s -> %s%n",
                                trimmed.substring(0, Math.min(80, trimmed.length())).replace('\n', ' '), msg);
                    }
                }
            }
        }
        System.out.println("[OK] " + scriptName);
    }

    /**
     * Hash mật khẩu mặc định cho các account có passwordHash='NEEDS_HASH'.
     * Chạy 1 lần khi lần đầu khởi tạo DB.
     */
    private static void seedDefaultPasswords() throws Exception {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String defaultPassword = "123456";
        String hash = PasswordUtil.hash(defaultPassword);

        String sql = "UPDATE Account SET passwordHash = ? WHERE passwordHash = 'NEEDS_HASH'";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                System.out.println("[AUTH] Seeded password for " + updated + " default accounts (pw: " + defaultPassword + ")");
            }
        }
    }
}