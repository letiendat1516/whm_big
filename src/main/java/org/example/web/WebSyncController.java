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
        TABLE_PK.put("EmployeeAssignment",      "\"assignmentId\"");
        TABLE_PK.put("HR",                       "\"hrId\"");
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
        // warehouse_balances has composite PK — handled separately
        TABLE_PK.put("Cashier",                 "\"cashierId\"");
        TABLE_PK.put("StoreManager",            "\"managerId\"");
        TABLE_PK.put("\"Order\"",               "\"orderId\"");
        TABLE_PK.put("OrderItem",               "\"orderItemId\"");
        TABLE_PK.put("Payment",                 "\"paymentId\"");
        TABLE_PK.put("CashPayment",             "\"paymentId\"");
        TABLE_PK.put("QRPayment",               "\"paymentId\"");
        TABLE_PK.put("Receipt",                 "\"receiptId\"");
        TABLE_PK.put("ReturnOrder",             "\"returnId\"");
        TABLE_PK.put("ReturnOrderItem",         "\"returnItemId\"");
        TABLE_PK.put("SalesOutbound",           "\"outboundId\"");
        TABLE_PK.put("SalesOutboundItem",       "\"outboundItemId\"");
    }

    // Dependency order: parent tables first, child tables last
    private static final List<String> TABLE_ORDER = List.of(
        "ProductCategory", "Product", "ProductVariant", "PriceList", "ProductPrice",
        "Store", "Employee", "EmployeeAssignment", "StoreManager", "Cashier", "HR",
        "ShiftTemplate", "ShiftAssignment", "AttendanceRecord", "PayrollPeriod", "PayrollSnapshot",
        "MembershipRank", "Customer", "LoyaltyAccount", "LoyaltyPointRule", "PointTransaction",
        "Campaign", "Promotion", "PromotionApplicationLog",
        "warehouse",
        "Order", "OrderItem", "Payment", "CashPayment", "QRPayment", "Receipt",
        "ReturnOrder", "ReturnOrderItem", "SalesOutbound", "SalesOutboundItem"
    );

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

            // Sort changes by table dependency order (parents first)
            changes.sort((a, b) -> {
                String ta = (String) a.get("table");
                String tb = (String) b.get("table");
                int ia = TABLE_ORDER.indexOf(ta); if (ia < 0) ia = 999;
                int ib = TABLE_ORDER.indexOf(tb); if (ib < 0) ib = 999;
                return Integer.compare(ia, ib);
            });

            int accepted = 0, rejected = 0, errors = 0;
            List<Map<String, Object>> rejectedItems = new ArrayList<>();
            List<Map<String, Object>> errorItems = new ArrayList<>();

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

                    // Resolve table name → PG table name
                    String pgTable = table;
                    String pkCol = TABLE_PK.get(table);
                    if (pkCol == null) {
                        pkCol = TABLE_PK.get("\"" + table + "\"");
                        if (pkCol != null) {
                            pgTable = "\"" + table + "\"";
                        } else {
                            System.err.println("[SYNC] Unknown table: " + table);
                            errors++;
                            continue;
                        }
                    }

                    try {
                        if ("DELETE".equals(operation)) {
                            String sql = "DELETE FROM " + pgTable + " WHERE " + pkCol + "=?";
                            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                                ps.setString(1, recordId);
                                int rows = ps.executeUpdate();
                                if (rows > 0) accepted++; else rejected++;
                            }
                        } else if (data != null && !data.isEmpty()) {
                            int serverVersion = getServerVersion(conn, pgTable, pkCol, recordId);

                            if (serverVersion < 0) {
                                // Record doesn't exist → INSERT (upsert)
                                int r = doInsert(conn, pgTable, pkCol, recordId, data, incomingVersion);
                                if (r > 0) accepted++;
                                else { errors++; errorItems.add(Map.of("table", table, "recordId", recordId, "reason", "INSERT returned 0")); }
                            } else if (incomingVersion > serverVersion) {
                                // Incoming is newer → UPDATE
                                doUpdate(conn, pgTable, pkCol, recordId, data, incomingVersion);
                                accepted++;
                            } else {
                                rejected++;
                                rejectedItems.add(Map.of(
                                    "table", table, "recordId", recordId,
                                    "reason", "Server version " + serverVersion + " >= incoming " + incomingVersion
                                ));
                            }
                        }
                    } catch (SQLException e) {
                        errors++;
                        String msg = e.getMessage() != null ? e.getMessage() : "unknown";
                        System.err.println("[SYNC] Push error on " + table + "/" + recordId + ": " + msg);
                        errorItems.add(Map.of("table", table, "recordId", recordId, "error", msg));
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
            if (!errorItems.isEmpty()) result.put("errorItems", errorItems);
            ctx.json(result);

        } catch (Exception e) {
            e.printStackTrace();
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

                // Special: warehouse_balances (composite PK)
                if (requestedTables == null || requestedTables.isEmpty() || requestedTables.contains("warehouse_balances")) {
                    try {
                        List<Map<String, Object>> wbRows = pullTable(conn, "warehouse_balances", sinceTs);
                        if (!wbRows.isEmpty()) {
                            allChanges.put("warehouse_balances", wbRows);
                            totalRecords += wbRows.size();
                        }
                    } catch (SQLException e) {
                        System.err.println("[SYNC] Pull error on warehouse_balances: " + e.getMessage());
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
        String cleanPk = pkCol.replace("\"", "");
        data.put(cleanPk, recordId);
        data.put("version", version);
        data.put("sync_status", "SYNCED");
        data.remove("last_modified");

        // Get column types from DB metadata for proper casting
        Map<String, String> colTypes = getColumnTypes(conn, table);

        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        StringBuilder updateSet = new StringBuilder();
        List<Object> params = new ArrayList<>();

        for (Map.Entry<String, Object> e : data.entrySet()) {
            if (cols.length() > 0) { cols.append(","); vals.append(","); }
            String colName = quoteCol(e.getKey());
            cols.append(colName);
            vals.append(getCastExpression(e.getKey(), colTypes));
            params.add(e.getValue());
            if (!e.getKey().equals(cleanPk)) {
                if (updateSet.length() > 0) updateSet.append(",");
                updateSet.append(colName).append("=EXCLUDED.").append(colName);
            }
        }

        String sql;
        if (updateSet.length() > 0) {
            sql = "INSERT INTO " + table + " (" + cols + ") VALUES (" + vals + ") ON CONFLICT (" + pkCol + ") DO UPDATE SET " + updateSet;
        } else {
            sql = "INSERT INTO " + table + " (" + cols + ") VALUES (" + vals + ") ON CONFLICT DO NOTHING";
        }

        System.out.println("[SYNC-INSERT] " + table + " id=" + recordId);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SYNC-INSERT] FAIL " + table + ": " + e.getMessage());
            throw e;
        }
    }

    private static int doUpdate(Connection conn, String table, String pkCol, String recordId,
                                 Map<String, Object> data, int version) throws SQLException {
        data.put("version", version);
        data.put("sync_status", "SYNCED");
        data.remove("last_modified");

        Map<String, String> colTypes = getColumnTypes(conn, table);

        StringBuilder setClause = new StringBuilder();
        List<Object> params = new ArrayList<>();
        String cleanPk = pkCol.replace("\"", "");

        for (Map.Entry<String, Object> e : data.entrySet()) {
            String colName = e.getKey();
            if (colName.equals(cleanPk)) continue;
            if (setClause.length() > 0) setClause.append(",");
            setClause.append(quoteCol(colName)).append("=").append(getCastExpression(colName, colTypes));
            params.add(e.getValue());
        }

        params.add(recordId);
        String sql = "UPDATE " + table + " SET " + setClause + " WHERE " + pkCol + "=?";
        System.out.println("[SYNC-UPDATE] " + table + " id=" + recordId);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SYNC-UPDATE] FAIL " + table + ": " + e.getMessage());
            throw e;
        }
    }

    /** Quote a column name for PostgreSQL if it contains uppercase characters */
    private static String quoteCol(String colName) {
        if (colName == null) return colName;
        if (colName.startsWith("\"")) return colName;
        if (colName.matches(".*[A-Z].*")) return "\"" + colName + "\"";
        return colName;
    }

    /**
     * Get column type map for a table from PostgreSQL metadata.
     */
    private static Map<String, String> getColumnTypes(Connection conn, String table) {
        Map<String, String> types = new HashMap<>();
        String cleanTable = table.replace("\"", "");
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, cleanTable, null)) {
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                String type = rs.getString("TYPE_NAME").toLowerCase();
                types.put(col, type);
                types.put(col.toLowerCase(), type);
            }
        } catch (SQLException e) {
            // ignore
        }
        return types;
    }

    /**
     * Returns the proper placeholder with cast for a column.
     * Fixes: "column X is of type timestamp but expression is of type varchar"
     */
    private static String getCastExpression(String colName, Map<String, String> colTypes) {
        String type = colTypes.get(colName);
        if (type == null) type = colTypes.get(colName.toLowerCase());
        if (type == null) return "?";

        if (type.contains("timestamp")) {
            return "CAST(? AS TIMESTAMP)";
        } else if (type.equals("date")) {
            return "CAST(? AS DATE)";
        } else if (type.contains("bool")) {
            return "CAST(? AS BOOLEAN)";
        } else if (type.contains("int") || type.contains("serial")) {
            return "CAST(? AS INTEGER)";
        } else if (type.contains("numeric") || type.contains("decimal") || type.contains("float") || type.contains("double") || type.contains("real")) {
            return "CAST(? AS NUMERIC)";
        }
        return "?";
    }

    private static List<Map<String, Object>> pullTable(Connection conn, String table, Timestamp since) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        boolean hasLastModified = hasColumn(conn, table, "last_modified");

        String sql;
        if (hasLastModified) {
            sql = "SELECT * FROM " + table + " WHERE last_modified > ? ORDER BY last_modified LIMIT 2000";
        } else {
            sql = "SELECT * FROM " + table + " LIMIT 5000";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (hasLastModified) {
                ps.setTimestamp(1, since);
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        Object value = rs.getObject(i);
                        if (value instanceof Timestamp) value = value.toString();
                        row.put(meta.getColumnName(i), value);
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private static boolean hasColumn(Connection conn, String table, String column) {
        String cleanTable = table.replace("\"", "");
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, cleanTable, column)) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private static void logSyncEvent(String storeId, String direction, int accepted, int rejected, int errors) {
        try (Connection conn = pg.getConnection()) {
            try (Statement s = conn.createStatement()) {
                s.execute("CREATE TABLE IF NOT EXISTS sync_log (" +
                    "log_id TEXT PRIMARY KEY DEFAULT replace(gen_random_uuid()::text,'-','')," +
                    "store_id TEXT NOT NULL," +
                    "direction TEXT NOT NULL," +
                    "accepted INTEGER DEFAULT 0," +
                    "rejected INTEGER DEFAULT 0," +
                    "errors INTEGER DEFAULT 0," +
                    "synced_at TIMESTAMP NOT NULL DEFAULT NOW())");
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
