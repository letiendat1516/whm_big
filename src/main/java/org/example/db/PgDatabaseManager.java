package org.example.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
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
        "pg_seed_data.sql",
        "pg_sync_triggers.sql",
        "pg_migrations.sql"
    );

    private PgDatabaseManager() {}

    public static synchronized PgDatabaseManager getInstance() {
        if (instance == null) instance = new PgDatabaseManager();
        return instance;
    }

    /** Initialize connection pool from DATABASE_URL env var */
    public void init() {
        // Debug: print all DB-related env vars
        System.out.println("[PG] Checking environment variables...");
        System.out.println("[PG]   DATABASE_URL = " + (System.getenv("DATABASE_URL") != null ? "SET" : "NOT SET"));
        System.out.println("[PG]   DATABASE_PUBLIC_URL = " + (System.getenv("DATABASE_PUBLIC_URL") != null ? "SET" : "NOT SET"));
        System.out.println("[PG]   DATABASE_PRIVATE_URL = " + (System.getenv("DATABASE_PRIVATE_URL") != null ? "SET" : "NOT SET"));
        System.out.println("[PG]   PGHOST = " + System.getenv("PGHOST"));
        System.out.println("[PG]   PGPORT = " + System.getenv("PGPORT"));
        System.out.println("[PG]   PGDATABASE = " + System.getenv("PGDATABASE"));
        System.out.println("[PG]   PGUSER = " + System.getenv("PGUSER"));
        System.out.println("[PG]   PGPASSWORD = " + (System.getenv("PGPASSWORD") != null ? "SET" : "NOT SET"));

        String dbUrl = System.getenv("DATABASE_URL");

        // Railway sometimes uses DATABASE_PUBLIC_URL or DATABASE_PRIVATE_URL
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = System.getenv("DATABASE_PUBLIC_URL");
            if (dbUrl != null) System.out.println("[PG] Using DATABASE_PUBLIC_URL");
        }
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = System.getenv("DATABASE_PRIVATE_URL");
            if (dbUrl != null) System.out.println("[PG] Using DATABASE_PRIVATE_URL");
        }

        if (dbUrl == null || dbUrl.isBlank()) {
            // Build from individual PG* vars (Railway sets these automatically)
            String pgHost = System.getenv("PGHOST");
            String pgPort = System.getenv("PGPORT");
            String pgDb = System.getenv("PGDATABASE");
            String pgUser = System.getenv("PGUSER");
            String pgPass = System.getenv("PGPASSWORD");
            if (pgHost != null && pgDb != null && pgUser != null) {
                dbUrl = "postgresql://" + pgUser + ":" + (pgPass != null ? pgPass : "")
                        + "@" + pgHost + ":" + (pgPort != null ? pgPort : "5432") + "/" + pgDb;
                System.out.println("[PG] Built DATABASE_URL from individual PG* env vars.");
            } else {
                System.err.println("[PG] *** ALL DB env vars are missing! ***");
                System.err.println("[PG] You need to:");
                System.err.println("[PG]   1. Add a PostgreSQL database to your Railway project");
                System.err.println("[PG]   2. Go to your Java service → Variables tab");
                System.err.println("[PG]   3. Click 'Add Reference Variable' → select DATABASE_URL from Postgres");
                System.err.println("[PG]   4. Redeploy");
                throw new RuntimeException("DATABASE_URL environment variable is not set. "
                    + "Add a PostgreSQL service to your Railway project and reference DATABASE_URL.");
            }
        }

        // Parse DATABASE_URL and build JDBC connection
        // Railway format: postgresql://user:pass@host:port/dbname
        // or:             postgres://user:pass@host:port/dbname
        String cleanUrl = dbUrl;
        if (cleanUrl.startsWith("jdbc:")) {
            cleanUrl = cleanUrl.substring(5);
        }
        if (cleanUrl.startsWith("postgres://")) {
            cleanUrl = "postgresql://" + cleanUrl.substring("postgres://".length());
        }

        // Extract user:pass from URL
        String username = null;
        String password = null;
        String jdbcUrl;

        try {
            // postgresql://user:pass@host:port/db
            java.net.URI uri = new java.net.URI(cleanUrl);
            String userInfo = uri.getUserInfo(); // "user:pass"
            String host = uri.getHost();
            int uriPort = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath(); // "/dbname"

            if (userInfo != null && userInfo.contains(":")) {
                String[] parts = userInfo.split(":", 2);
                username = parts[0];
                password = parts[1];
            } else if (userInfo != null) {
                username = userInfo;
            }

            jdbcUrl = "jdbc:postgresql://" + host + ":" + uriPort + path;
            System.out.println("[PG] Parsed URL -> host=" + host + " port=" + uriPort
                + " db=" + path + " user=" + username);
        } catch (Exception e) {
            // Fallback: just prepend jdbc:
            System.out.println("[PG] Could not parse URL, using as-is: " + e.getMessage());
            jdbcUrl = "jdbc:" + cleanUrl;
        }

        // Explicitly load AND register PostgreSQL driver.
        // In fat JARs (maven-shade), META-INF/services may not merge correctly,
        // so we must force-register the driver with DriverManager.
        try {
            Class.forName("org.postgresql.Driver");
            // Double-check: register explicitly in case ServiceLoader missed it
            try {
                java.sql.DriverManager.registerDriver(new org.postgresql.Driver());
            } catch (Exception ignored) { /* already registered */ }
            System.out.println("[PG] PostgreSQL JDBC driver loaded and registered successfully.");
            System.out.println("[PG]   Available drivers:");
            java.util.Collections.list(java.sql.DriverManager.getDrivers())
                .forEach(d -> System.out.println("[PG]     " + d.getClass().getName()));
        } catch (ClassNotFoundException e) {
            System.err.println("[PG] *** FATAL: PostgreSQL JDBC driver NOT FOUND in classpath! ***");
            System.err.println("[PG] Check that 'org.postgresql:postgresql' is in pom.xml");
            System.err.println("[PG] Classpath entries:");
            String cp = System.getProperty("java.class.path");
            if (cp != null) {
                for (String entry : cp.split(java.io.File.pathSeparator)) {
                    System.err.println("[PG]   " + entry);
                }
            }
            throw new RuntimeException("PostgreSQL JDBC driver not found in classpath!", e);
        }

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(jdbcUrl);
        if (username != null) config.setUsername(username);
        if (password != null) config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // PostgreSQL specific
        config.addDataSourceProperty("reWriteBatchedInserts", "true");

        dataSource = new HikariDataSource(config);
        System.out.println("[PG] Connection pool initialized: " + jdbcUrl);
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

