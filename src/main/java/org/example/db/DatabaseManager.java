package org.example.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Singleton database manager.
 * Manages the SQLite connection with Offline-First settings.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:store_management.db";
    private static DatabaseManager instance;
    private Connection connection;

    private static final List<String> SQL_SCRIPTS = Arrays.asList(
            "init_database.sql",
            "module3_product.sql",
            "module5_hr_shift.sql",
            "module4_crm_promotion.sql",
            "module2_inventory.sql",
            "module1_pos.sql",
            "seed_data.sql",
            "module6_pos_enhancement.sql"
    );

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    /** Returns a live connection, re-opening if closed. */
    private boolean fkEnabled = false;   // set to true by Main after all scripts load

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
                applyPragmas(connection);
                if (fkEnabled) {
                    try (Statement s = connection.createStatement()) {
                        s.execute("PRAGMA foreign_keys = ON");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot open DB connection", e);
        }
        return connection;
    }

    /** Called by Main.initDatabase() after all SQL scripts are loaded. */
    public void enableForeignKeys() {
        fkEnabled = true;
    }

    private void applyPragmas(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // NOTE: foreign_keys is intentionally OFF here.
            // Main.initDatabase() turns it ON after all scripts are loaded.
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA cache_size = -64000");
            stmt.execute("PRAGMA temp_store = MEMORY");
        }
    }

    /** Initialize DB: run all SQL scripts in order. */
    public void initialize() {
        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);
            for (String script : SQL_SCRIPTS) {
                runScript(conn, script);
            }
            conn.commit();
            conn.setAutoCommit(true);
            System.out.println("[DB] Initialized successfully.");
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("DB initialization failed", e);
        }
    }

    private void runScript(Connection conn, String resourceName) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
        if (is == null) { System.err.println("[DB] Script not found: " + resourceName); return; }

        List<String> statements = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        int beginEndDepth = 0;

        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;

                // Track BEGIN/END for TRIGGER bodies
                String upper = trimmed.toUpperCase();
                if (upper.startsWith("BEGIN")) beginEndDepth++;

                current.append(line).append("\n");

                if (upper.startsWith("END;") || upper.equals("END")) {
                    beginEndDepth = Math.max(0, beginEndDepth - 1);
                }

                // Statement complete when we hit ';' outside a BEGIN/END block
                if (trimmed.endsWith(";") && beginEndDepth == 0) {
                    String stmt = current.toString().trim();
                    if (!stmt.isEmpty()) statements.add(stmt);
                    current.setLength(0);
                }
            }
            // Leftover
            String leftover = current.toString().trim();
            if (!leftover.isEmpty()) statements.add(leftover);
        }

        try (Statement stmt = conn.createStatement()) {
            for (String s : statements) {
                String cleaned = s.endsWith(";") ? s.substring(0, s.length() - 1).trim() : s.trim();
                if (cleaned.isEmpty()) continue;
                try { stmt.execute(cleaned); } catch (SQLException e) {
                    String msg = e.getMessage();
                    if (!msg.contains("already exists") && !msg.contains("UNIQUE constraint")
                            && !msg.contains("duplicate column name"))
                        System.err.printf("[DB][WARN] %s : %s%n", resourceName, msg);
                }
            }
        }
    }

    /**
     * Insert a record into sync_queue so the cloud sync agent can pick it up.
     */
    public void addSyncQueueEntry(String tableName, String recordId, String operation, String payloadJson) {
        String sql = "INSERT INTO sync_queue(queue_id,table_name,record_id,operation,payload,status) VALUES(?,?,?,?,?,'PENDING')";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString().replace("-",""));
            ps.setString(2, tableName);
            ps.setString(3, recordId);
            ps.setString(4, operation);
            ps.setString(5, payloadJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB][SyncQueue] " + e.getMessage());
        }
    }

    /** Generate a new UUID string (no dashes). */
    public static String newId() {
        return UUID.randomUUID().toString().replace("-","");
    }

    public void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException e) { e.printStackTrace(); }
    }
}

