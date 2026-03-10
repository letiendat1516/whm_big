package org.example.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.db.PgDatabaseManager;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Sync API Controller — Handles bi-directional data synchronization
 * between Desktop (SQLite) clients and the Cloud (PostgreSQL) server.
 *
 * Endpoints:
 *   POST /api/sync/push   — Desktop pushes local changes to cloud
 *   POST /api/sync/pull   — Desktop pulls changes from cloud since a timestamp
 *   GET  /api/sync/status — Check sync status and table counts
 *   POST /api/sync/register — Register a store for sync (get API key)
 *
 * Auth: Uses X-Sync-Key header (store-level API key)
 *
 * Conflict resolution: Last-Write-Wins via version column.
 *   - If incoming version > server version → accept incoming
 *   - If incoming version <= server version → reject (server wins)
 */
public class WebSyncController {

    private static final PgDatabaseManager pg = PgDatabaseManager.getInstance();
    private static final ObjectMapper mapper = new ObjectMapper();

    // ── All syncable tables with their primary key column ──
    private static final Map<String, String> TABLE_PK = new LinkedHashMap<>();
    static {
        TABLE_PK.put("ProductCategory",         "category_id");
        TABLE_PK.put("Product",                 "\"ProductID\"");
        TABLE_PK.put("ProductVariant",          "variant_id");
        TABLE_PK.put("PriceList",               "price_list_id");
        TABLE_PK.put("ProductPrice",            "price_id");
        TABLE_PK.put("Store",                   "\"storeId\"");
        TABLE_PK.put("Employee",                "\"employeeId\"");
        TABLE_PK.put("ShiftTemplate",           "\"shiftTemplateId\"");
        TABLE_PK.put("ShiftAssignment",         "\"shiftAssignmentId\"");
        TABLE_PK.put("AttendanceRecord",        "\"attendanceId\"");
        TABLE_PK.put("PayrollPeriod",           "\"payrollPeriodId\"");
        TABLE_PK.put("PayrollSnapshot",         "\"snapshotId\"");
        TABLE_PK.put("Customer",                "\"CustomerID\"");
        TABLE_PK.put("MembershipRank",          "\"TierID\"");
        TABLE_PK.put("LoyaltyAccount",          "\"AccountID\"");
        TABLE_PK.put("LoyaltyPointRule",        "\"RuleID\"");
        TABLE_PK.put("PointTransaction",        "\"TransactionID\"");
        TABLE_PK.put("Campaign",                "\"CampaignID\"");
        TABLE_PK.put("Promotion",               "\"PromotionID\"");
        TABLE_PK.put("PromotionApplicationLog", "\"LogID\"");
        TABLE_PK.put("warehouse",               "warehouse_id");
        TABLE_PK.put("warehouse_balances",      "balance_id");
        TABLE_PK.put("Cashier",                 "\"cashierId\"");
        TABLE_PK.put("StoreManager",            "\"managerId\"");
        TABLE_PK.put("\"Order\"",               "\"orderId\"");
        TABLE_PK.put("OrderItem",               "\"orderItemId\"");
        TABLE_PK.put("Payment",                 "\"paymentId\"");
        TABLE_PK.put("CashPayment",             "\"paymentId\"");
        TABLE_PK.put("QRPayment",               "\"paymentId\"");
        TABLE_PK.put("Receipt",                 "\"receiptId\"");
        TABLE_PK.put("ReturnOrder",             "\"returnId\"");
        TABLE_PK.put("SalesOutbound",           "outbound_id");
    }

    public static void register(Javalin app) {
        // Sync endpoints — protected by API key
        app.post("/api/sync/push",     WebSyncController::handlePush);
        app.post("/api/sync/pull",     WebSyncController::handlePull);
        app.get("/api/sync/status",    WebSyncController::handleStatus);
        app.post("/api/sync/register", WebSyncController::handleRegister);
        app.get("/api/sync/tables",    WebSyncController::handleTableList);
    }

    // ═══════════════════════════════════════════════════════════════
    // PUSH — Desktop sends changes to cloud
    // ═══════════════════════════════════════════════════════════════
    /**
     * Request body:
     * {
     *   "storeId": "STORE-001",
     *   "apiKey": "xxx",
     *   "changes": [
     *     {
     *       "table": "Product",
     *       "operation": "INSERT|UPDATE|DELETE",
     *       "recordId": "xxx",
     *       "version": 5,
     *       "data": { "column1": "value1", ... }
     *     }
     *   ]
     * }
     */
    @SuppressWarnings("unchecked")
    private static void handlePush(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String storeId = (String) body.get("storeId");
            String apiKey = (String) body.get("apiKey");

            // Validate API key
            if (!validateApiKey(storeId, apiKey)) {
                ctx.status(401).json(Map.of("error", "Invalid API key"));
                return;
            }

            List<Map<String, Object>> changes = (List<Map<String, Object>>) body.get("changes");
            if (changes == null || changes.isEmpty()) {
                ctx.json(Map.of("success", true, "accepted", 0, "rejected", 0));
                return;
            }

            int accepted = 0, rejected = 0, errors = 0;
            List<Map<String, Object>> rejectedItems = new ArrayList<>();

            try (Connection conn = pg.getConnection()) {
                conn.setAutoCommit(false);

                for (Map<String, Object> change : changes) {
                    String table = (String) change.get("table");
                    String operation = (String) change.get("operation");
                    String recordId = (String) change.get("recordId");
                    int incomingVersion = toInt(change.get("version"));
                    Map<String, Object> data = (Map<String, Object>) change.get("data");

                    if (table == null || operation == null || recordId == null) {
                        errors++;
                        continue;
                    }

                    String pkCol = TABLE_PK.get(table);
                    if (pkCol == null) {
                        // Try with quotes
                        pkCol = TABLE_PK.get("\"" + table + "\"");
                        if (pkCol == null) {
                            errors++;
                            continue;
                        }
                        table = "\"" + table + "\"";
                    }

                    try {
                        if ("DELETE".equals(operation)) {
                            // Delete — only if server version <= incoming
                            String sql = "DELETE FROM " + table + " WHERE " + pkCol + "=? AND version<=?";
                            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                                ps.setString(1, recordId);
                                ps.setInt(2, incomingVersion);
                                int rows = ps.executeUpdate();
                                if (rows > 0) accepted++; else rejected++;
                            }
                        } else if (data != null && !data.isEmpty()) {
                            // Check server version
                            int serverVersion = getServerVersion(conn, table, pkCol, recordId);

                            if (serverVersion < 0) {
                                // Record doesn't exist on server → INSERT
                                accepted += doInsert(conn, table, pkCol, recordId, data, incomingVersion);
                            } else if (incomingVersion > serverVersion) {
                                // Incoming is newer → UPDATE
                                accepted += doUpdate(conn, table, pkCol, recordId, data, incomingVersion);
                            } else {
                                // Server is newer or same → REJECT (server wins)
                                rejected++;
                                rejectedItems.add(Map.of(
                                    "table", table, "recordId", recordId,
                                    "reason", "Server version " + serverVersion + " >= incoming " + incomingVersion
                                ));
                            }
                        }
                    } catch (SQLException e) {
                        errors++;
                        System.err.println("[SYNC] Push error on " + table + "/" + recordId + ": " + e.getMessage());
                    }
                }

                conn.commit();
            }

            // Log sync event
            logSyncEvent(storeId, "PUSH", accepted, rejected, errors);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("accepted", accepted);
            result.put("rejected", rejected);
            result.put("errors", errors);
            result.put("serverTime", Instant.now().toString());
            if (!rejectedItems.isEmpty()) result.put("rejectedItems", rejectedItems);
            ctx.json(result);

        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "Sync push failed: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PULL — Desktop requests changes from cloud since timestamp
    // ═══════════════════════════════════════════════════════════════
    /**
     * Request body:
     * {
     *   "storeId": "STORE-001",
     *   "apiKey": "xxx",
     *   "since": "2026-03-01T00:00:00Z",
     *   "tables": ["Product","Order"]   // optional, null = all
     * }
     */
    @SuppressWarnings("unchecked")
    private static void handlePull(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String storeId = (String) body.get("storeId");
            String apiKey = (String) body.get("apiKey");
            String since = (String) body.get("since");
            List<String> requestedTables = (List<String>) body.get("tables");

            if (!validateApiKey(storeId, apiKey)) {
                ctx.status(401).json(Map.of("error", "Invalid API key"));
                return;
            }

            if (since == null) since = "1970-01-01T00:00:00Z";

            // Parse since timestamp
            Timestamp sinceTs;
            try {
                sinceTs = Timestamp.from(Instant.parse(since));
            } catch (Exception e) {
                sinceTs = Timestamp.valueOf("1970-01-01 00:00:00");
            }

            Map<String, List<Map<String, Object>>> allChanges = new LinkedHashMap<>();
            int totalRecords = 0;

            try (Connection conn = pg.getConnection()) {
                for (Map.Entry<String, String> entry : TABLE_PK.entrySet()) {
                    String table = entry.getKey();
                    String cleanTable = table.replace("\"", "");

                    // Filter tables if specified
                    if (requestedTables != null && !requestedTables.isEmpty()) {
                        if (!requestedTables.contains(cleanTable)) continue;
                    }

                    try {
                        List<Map<String, Object>> rows = pullTable(conn, table, sinceTs);
                        if (!rows.isEmpty()) {
                            allChanges.put(cleanTable, rows);
                            totalRecords += rows.size();
                        }
                    } catch (SQLException e) {
                        System.err.println("[SYNC] Pull error on " + table + ": " + e.getMessage());
                    }
                }
            }

            logSyncEvent(storeId, "PULL", totalRecords, 0, 0);

            ctx.json(Map.of(
                "success", true,
                "serverTime", Instant.now().toString(),
                "totalRecords", totalRecords,
                "changes", allChanges
            ));

        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "Sync pull failed: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STATUS
    // ═══════════════════════════════════════════════════════════════
    private static void handleStatus(Context ctx) {
        try {
            Map<String, Object> status = new LinkedHashMap<>();
            status.put("serverTime", Instant.now().toString());
            status.put("status", "online");

            Map<String, Integer> tableCounts = new LinkedHashMap<>();
            try (Connection conn = pg.getConnection()) {
                for (String table : TABLE_PK.keySet()) {
                    try (Statement s = conn.createStatement();
                         ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
                        if (rs.next()) tableCounts.put(table.replace("\"", ""), rs.getInt(1));
                    } catch (SQLException ignored) {}
                }
            }
            status.put("tables", tableCounts);
            ctx.json(status);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // REGISTER — Create sync API key for a store
    // ═══════════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private static void handleRegister(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String storeId = (String) body.get("storeId");
            String storeName = (String) body.getOrDefault("storeName", storeId);

            if (storeId == null || storeId.isBlank()) {
                ctx.status(400).json(Map.of("error", "storeId is required"));
                return;
            }

            String apiKey = UUID.randomUUID().toString().replace("-", "");

            try (Connection conn = pg.getConnection()) {
                // Create sync_keys table if not exists
                try (Statement s = conn.createStatement()) {
                    s.execute("""
                        CREATE TABLE IF NOT EXISTS sync_keys (
                            store_id   TEXT PRIMARY KEY,
                            store_name TEXT,
                            api_key    TEXT NOT NULL UNIQUE,
                            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                            last_sync  TIMESTAMP,
                            is_active  BOOLEAN NOT NULL DEFAULT TRUE
                        )
                    """);
                }

                // Upsert
                String sql = """
                    INSERT INTO sync_keys(store_id, store_name, api_key)
                    VALUES (?, ?, ?)
                    ON CONFLICT(store_id) DO UPDATE SET api_key=EXCLUDED.api_key, store_name=EXCLUDED.store_name
                    RETURNING api_key
                """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, storeId);
                    ps.setString(2, storeName);
                    ps.setString(3, apiKey);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) apiKey = rs.getString(1);
                    }
                }
            }

            ctx.json(Map.of(
                "success", true,
                "storeId", storeId,
                "apiKey", apiKey,
                "serverUrl", ctx.scheme() + "://" + ctx.host()
            ));

        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TABLE LIST — For client to know which tables are syncable
    // ═══════════════════════════════════════════════════════════════
    private static void handleTableList(Context ctx) {
        List<Map<String, String>> tables = new ArrayList<>();
        for (Map.Entry<String, String> e : TABLE_PK.entrySet()) {
            tables.add(Map.of(
                "table", e.getKey().replace("\"", ""),
                "pkColumn", e.getValue().replace("\"", "")
            ));
        }
        ctx.json(Map.of("tables", tables));
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    private static boolean validateApiKey(String storeId, String apiKey) {
        if (storeId == null || apiKey == null) return false;
        try (Connection conn = pg.getConnection()) {
            // First check sync_keys table
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM sync_keys WHERE store_id=? AND api_key=? AND is_active=TRUE")) {
                ps.setString(1, storeId);
                ps.setString(2, apiKey);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return true;
                }
            } catch (SQLException e) {
                // Table may not exist yet — allow "master" key
            }
            // Fallback: check app_config for a master sync key
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT value FROM app_config WHERE key='sync_master_key'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && apiKey.equals(rs.getString(1))) return true;
                }
            }
        } catch (Exception e) {
            System.err.println("[SYNC] Auth error: " + e.getMessage());
        }
        return false;
    }

    private static int getServerVersion(Connection conn, String table, String pkCol, String recordId) throws SQLException {
        String sql = "SELECT version FROM " + table + " WHERE " + pkCol + "=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("version");
                return -1; // Not found
            }
        }
    }

    private static int doInsert(Connection conn, String table, String pkCol, String recordId,
                                 Map<String, Object> data, int version) throws SQLException {
        data.put(pkCol.replace("\"", ""), recordId);
        data.put("version", version);
        data.put("sync_status", "SYNCED");

        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        List<Object> params = new ArrayList<>();

        for (Map.Entry<String, Object> e : data.entrySet()) {
            if (cols.length() > 0) { cols.append(","); vals.append(","); }
            String colName = e.getKey();
            // Quote if contains uppercase
            if (colName.matches(".*[A-Z].*") && !colName.startsWith("\"")) {
                colName = "\"" + colName + "\"";
            }
            cols.append(colName);
            vals.append("?");
            params.add(e.getValue());
        }

        String sql = "INSERT INTO " + table + " (" + cols + ") VALUES (" + vals + ") ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate();
        }
    }

    private static int doUpdate(Connection conn, String table, String pkCol, String recordId,
                                 Map<String, Object> data, int version) throws SQLException {
        data.put("version", version);
        data.put("sync_status", "SYNCED");

        StringBuilder setClause = new StringBuilder();
        List<Object> params = new ArrayList<>();

        for (Map.Entry<String, Object> e : data.entrySet()) {
            String colName = e.getKey();
            String cleanPk = pkCol.replace("\"", "");
            if (colName.equals(cleanPk)) continue; // Don't update PK

            if (setClause.length() > 0) setClause.append(",");
            if (colName.matches(".*[A-Z].*") && !colName.startsWith("\"")) {
                colName = "\"" + colName + "\"";
            }
            setClause.append(colName).append("=?");
            params.add(e.getValue());
        }

        params.add(recordId);
        params.add(version);

        String sql = "UPDATE " + table + " SET " + setClause + " WHERE " + pkCol + "=? AND version<?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate();
        }
    }

    private static List<Map<String, Object>> pullTable(Connection conn, String table, Timestamp since) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT * FROM " + table + " WHERE last_modified > ? ORDER BY last_modified LIMIT 1000";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, since);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private static void logSyncEvent(String storeId, String direction, int accepted, int rejected, int errors) {
        try (Connection conn = pg.getConnection()) {
            try (Statement s = conn.createStatement()) {
                s.execute("""
                    CREATE TABLE IF NOT EXISTS sync_log (
                        log_id     TEXT PRIMARY KEY DEFAULT replace(gen_random_uuid()::text,'-',''),
                        store_id   TEXT NOT NULL,
                        direction  TEXT NOT NULL,
                        accepted   INTEGER DEFAULT 0,
                        rejected   INTEGER DEFAULT 0,
                        errors     INTEGER DEFAULT 0,
                        synced_at  TIMESTAMP NOT NULL DEFAULT NOW()
                    )
                """);
            }
            String sql = "INSERT INTO sync_log(store_id,direction,accepted,rejected,errors) VALUES(?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, storeId);
                ps.setString(2, direction);
                ps.setInt(3, accepted);
                ps.setInt(4, rejected);
                ps.setInt(5, errors);
                ps.executeUpdate();
            }
            // Update last_sync on sync_keys
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE sync_keys SET last_sync=NOW() WHERE store_id=?")) {
                ps.setString(1, storeId);
                ps.executeUpdate();
            } catch (SQLException ignored) {}
        } catch (Exception e) {
            System.err.println("[SYNC] Log error: " + e.getMessage());
        }
    }

    private static int toInt(Object obj) {
        if (obj == null) return 1;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return 1; }
    }
}

