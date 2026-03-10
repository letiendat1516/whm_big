package org.example.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PostgreSQL Database Manager for Railway cloud deployment.
 * Uses HikariCP connection pool for multi-threaded web server.
 *
 * Reads DATABASE_URL from environment (Railway sets this automatically).
 * Format: postgresql://user:pass@host:port/dbname
 */
public class PgDatabaseManager {

    private static PgDatabaseManager instance;
    private HikariDataSource dataSource;

    private static final List<String> PG_SCRIPTS = List.of(
        "pg_init.sql",
        "pg_module0_auth.sql",
        "pg_module3_product.sql",
        "pg_module5_hr_shift.sql",
        "pg_module4_crm_promotion.sql",
        "pg_module2_inventory.sql",
        "pg_module1_pos.sql",
        "pg_seed_data.sql"
    );

    private PgDatabaseManager() {}

    public static synchronized PgDatabaseManager getInstance() {
        if (instance == null) instance = new PgDatabaseManager();
        return instance;
    }

    /** Initialize connection pool from DATABASE_URL env var */
    public void init() {
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            throw new RuntimeException("DATABASE_URL environment variable is not set.\n"
                + "Railway sets this automatically when you add a PostgreSQL service.\n"
                + "For local dev, set it like: postgresql://user:pass@localhost:5432/store_db");
        }

        // Convert Railway format to JDBC format if needed
        String jdbcUrl = dbUrl;
        if (dbUrl.startsWith("postgresql://")) {
            jdbcUrl = "jdbc:" + dbUrl;
        } else if (dbUrl.startsWith("postgres://")) {
            jdbcUrl = "jdbc:postgresql://" + dbUrl.substring("postgres://".length());
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // PostgreSQL specific
        config.addDataSourceProperty("reWriteBatchedInserts", "true");

        dataSource = new HikariDataSource(config);
        System.out.println("[PG] Connection pool initialized: " + jdbcUrl.replaceAll("://.*@", "://***@"));
    }

    /** Get a connection from pool. CALLER MUST CLOSE IT (use try-with-resources). */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) throw new RuntimeException("Database not initialized. Call init() first.");
        return dataSource.getConnection();
    }

    /** Run all PostgreSQL init scripts in order */
    public void initializeSchema() {
        System.out.println("[PG] Initializing schema...");
        for (String script : PG_SCRIPTS) {
            try {
                runScript(script);
                System.out.println("[PG][OK] " + script);
            } catch (Exception e) {
                System.err.println("[PG][ERR] " + script + " -> " + e.getMessage());
            }
        }
        System.out.println("[PG] Schema initialization complete.");
    }

    private void runScript(String resourceName) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
        if (is == null) {
            System.err.println("[PG][SKIP] " + resourceName + " not found on classpath");
            return;
        }

        String fullSql;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--")) continue; // skip comments
                sb.append(line).append("\n");
            }
            fullSql = sb.toString().trim();
        }

        if (fullSql.isEmpty()) return;

        // Split by semicolons (respecting $$ blocks for functions/triggers)
        List<String> statements = splitStatements(fullSql);

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (trimmed.isEmpty()) continue;
                try {
                    stmt.execute(trimmed);
                } catch (SQLException e) {
                    String msg = e.getMessage();
                    if (msg != null && (msg.contains("already exists")
                            || msg.contains("duplicate key")
                            || msg.contains("relation") && msg.contains("already exists"))) {
                        // Idempotent — skip
                    } else {
                        System.err.printf("[PG][WARN] %s%n  SQL: %.100s%n",
                                msg, trimmed.replace('\n', ' '));
                    }
                }
            }
        }
    }

    /** Split SQL respecting $$ blocks for PostgreSQL functions/triggers */
    private List<String> splitStatements(String sql) {
        List<String> result = new ArrayList<>();
        boolean inDollarBlock = false;
        StringBuilder current = new StringBuilder();

        String[] lines = sql.split("\n");
        for (String line : lines) {
            current.append(line).append("\n");

            // Count $$ occurrences in line
            int idx = 0;
            while ((idx = line.indexOf("$$", idx)) != -1) {
                inDollarBlock = !inDollarBlock;
                idx += 2;
            }

            if (!inDollarBlock && line.trim().endsWith(";")) {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) result.add(stmt);
                current.setLength(0);
            }
        }

        String leftover = current.toString().trim();
        if (!leftover.isEmpty()) result.add(leftover);
        return result;
    }

    /** Generate a new UUID string (no dashes) */
    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[PG] Connection pool closed.");
        }
    }
}

