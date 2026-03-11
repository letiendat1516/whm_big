package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.db.DatabaseManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * SyncService — Bi-directional sync engine for Desktop ↔ Cloud.
 *
 * Architecture:
 *   Desktop (SQLite) ──push──▶ Cloud (PostgreSQL)
 *   Desktop (SQLite) ◀──pull── Cloud (PostgreSQL)
 *
 * Sync flow:
 *   1. PUSH: Read sync_queue (status=PENDING) → send to POST /api/sync/push
 *   2. PULL: Call POST /api/sync/pull?since=last_sync → upsert into SQLite
 *   3. Update last_sync timestamp in app_config
 *
 * Conflict resolution: Last-Write-Wins (version column)
 * Offline handling: All HTTP failures are silently caught; queue stays PENDING.
 */
public class SyncService {

    public enum SyncState { IDLE, SYNCING, SYNCED, OFFLINE, ERROR }

    private static SyncService instance;

    private final DatabaseManager db = DatabaseManager.getInstance();
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    private String serverUrl;   // e.g. "https://xxx.railway.app"
    private String storeId;     // e.g. "STORE-001"
    private String apiKey;      // sync API key from /api/sync/register

    private ScheduledExecutorService scheduler;
    private volatile SyncState state = SyncState.IDLE;
    private volatile String lastError;
    private volatile Instant lastSyncTime;

    // UI callback
    private Consumer<SyncState> stateListener;

    // ── Table PK mapping (must match WebSyncController) ──
    private static final Map<String, String> TABLE_PK = new LinkedHashMap<>();
    static {
        TABLE_PK.put("ProductCategory",         "category_id");
        TABLE_PK.put("Product",                 "ProductID");
        TABLE_PK.put("ProductVariant",          "variant_id");
        TABLE_PK.put("PriceList",               "price_list_id");
        TABLE_PK.put("ProductPrice",            "price_id");
        TABLE_PK.put("Store",                   "storeId");
        TABLE_PK.put("Employee",                "employeeId");
        TABLE_PK.put("EmployeeAssignment",      "assignmentId");
        TABLE_PK.put("HR",                       "hrId");
        TABLE_PK.put("ShiftTemplate",           "shiftTemplateId");
        TABLE_PK.put("ShiftAssignment",         "shiftAssignmentId");
        TABLE_PK.put("AttendanceRecord",        "attendanceId");
        TABLE_PK.put("PayrollPeriod",           "payrollPeriodId");
        TABLE_PK.put("PayrollSnapshot",         "snapshotId");
        TABLE_PK.put("Customer",                "CustomerID");
        TABLE_PK.put("MembershipRank",          "TierID");
        TABLE_PK.put("LoyaltyAccount",          "AccountID");
        TABLE_PK.put("LoyaltyPointRule",        "RuleID");
        TABLE_PK.put("PointTransaction",        "TransactionID");
        TABLE_PK.put("Campaign",                "CampaignID");
        TABLE_PK.put("Promotion",               "PromotionID");
        TABLE_PK.put("PromotionApplicationLog", "LogID");
        TABLE_PK.put("warehouse",               "warehouse_id");
        // warehouse_balances has composite PK (warehouse_id, product_id) — handled separately
        TABLE_PK.put("Cashier",                 "cashierId");
        TABLE_PK.put("StoreManager",            "managerId");
        TABLE_PK.put("Order",                   "orderId");
        TABLE_PK.put("OrderItem",               "orderItemId");
        TABLE_PK.put("Payment",                 "paymentId");
        TABLE_PK.put("CashPayment",             "paymentId");
        TABLE_PK.put("QRPayment",               "paymentId");
        TABLE_PK.put("Receipt",                  "receiptId");
        TABLE_PK.put("ReturnOrder",             "returnId");
        TABLE_PK.put("ReturnOrderItem",         "returnItemId");
        TABLE_PK.put("SalesOutbound",           "outboundId");
        TABLE_PK.put("SalesOutboundItem",       "outboundItemId");
    }

    private SyncService() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        loadConfig();
    }

    public static synchronized SyncService getInstance() {
        if (instance == null) instance = new SyncService();
        return instance;
    }

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════

    private void loadConfig() {
        try {
            Connection conn = db.getConnection();
            storeId = getConfig(conn, "store_id");
            serverUrl = getConfig(conn, "sync_server_url");
            apiKey = getConfig(conn, "sync_api_key");
            String lastSync = getConfig(conn, "last_sync");
            if (lastSync != null && !lastSync.startsWith("1970")) {
                try { lastSyncTime = Instant.parse(lastSync); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("[SYNC] Config load error: " + e.getMessage());
        }
    }

    public void configure(String serverUrl, String storeId, String apiKey) {
        this.serverUrl = serverUrl;
        this.storeId = storeId;
        this.apiKey = apiKey;
        saveConfig("sync_server_url", serverUrl);
        saveConfig("sync_api_key", apiKey);
        System.out.println("[SYNC] Configured: server=" + serverUrl + " store=" + storeId);
    }

    public boolean isConfigured() {
        return serverUrl != null && !serverUrl.isBlank()
            && apiKey != null && !apiKey.isBlank()
            && storeId != null && !storeId.isBlank();
    }

    // ═══════════════════════════════════════════════════════════════
    // AUTO SYNC SCHEDULER
    // ═══════════════════════════════════════════════════════════════

    public void startAutoSync(int intervalSeconds) {
        if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdown();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SyncService-Worker");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::syncAll, 5, intervalSeconds, TimeUnit.SECONDS);
        System.out.println("[SYNC] Auto-sync started: every " + intervalSeconds + "s");
    }

    public void stopAutoSync() {
        if (scheduler != null) {
            scheduler.shutdown();
            System.out.println("[SYNC] Auto-sync stopped.");
        }
    }

    public void setStateListener(Consumer<SyncState> listener) {
        this.stateListener = listener;
    }

    // ═══════════════════════════════════════════════════════════════
    // MAIN SYNC FLOW
    // ═══════════════════════════════════════════════════════════════

    public synchronized void syncAll() {
        if (!isConfigured()) {
            setState(SyncState.IDLE);
            return;
        }
        if (state == SyncState.SYNCING) return; // Already running

        setState(SyncState.SYNCING);
        try {
            // 1. Push local changes to cloud
            int pushed = pushChanges();
            System.out.println("[SYNC] Pushed " + pushed + " changes to cloud.");

            // 2. Pull cloud changes to local
            int pulled = pullChanges();
            System.out.println("[SYNC] Pulled " + pulled + " changes from cloud.");

            // 3. Update last_sync
            lastSyncTime = Instant.now();
            saveConfig("last_sync", lastSyncTime.toString());

            setState(SyncState.SYNCED);
            lastError = null;

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("Connection refused") || msg.contains("timed out")
                    || msg.contains("UnknownHost") || msg.contains("No route"))) {
                setState(SyncState.OFFLINE);
            } else {
                setState(SyncState.ERROR);
            }
            lastError = msg;
            System.err.println("[SYNC] Error: " + msg);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PUSH — Send local sync_queue entries to cloud
    // ═══════════════════════════════════════════════════════════════

    private int pushChanges() throws Exception {
        Connection conn = db.getConnection();
        List<Map<String, Object>> pending = new ArrayList<>();

        // Read pending sync_queue entries
        String sql = "SELECT queue_id, table_name, record_id, operation FROM sync_queue WHERE status='PENDING' ORDER BY created_at LIMIT 200";
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                pending.add(Map.of(
                    "queue_id", rs.getString("queue_id"),
                    "table_name", rs.getString("table_name"),
                    "record_id", rs.getString("record_id"),
                    "operation", rs.getString("operation")
                ));
            }
        }

        if (pending.isEmpty()) return 0;

        // Build change list with full row data
        List<Map<String, Object>> changes = new ArrayList<>();
        for (Map<String, Object> entry : pending) {
            String tableName = (String) entry.get("table_name");
            String recordId = (String) entry.get("record_id");
            String operation = (String) entry.get("operation");

            Map<String, Object> change = new LinkedHashMap<>();
            change.put("table", tableName);
            change.put("operation", operation);
            change.put("recordId", recordId);

            if (!"DELETE".equals(operation)) {
                // Read full row from source table
                Map<String, Object> rowData = readRow(conn, tableName, recordId);
                if (rowData == null) {
                    // Row doesn't exist locally anymore — treat as DELETE
                    change.put("operation", "DELETE");
                    change.put("version", 1);
                } else {
                    change.put("version", rowData.getOrDefault("version", 1));
                    change.put("data", rowData);
                }
            } else {
                change.put("version", 1);
            }

            changes.add(change);
        }

        // Send to server
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("storeId", storeId);
        payload.put("apiKey", apiKey);
        payload.put("changes", changes);

        String json = mapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/api/sync/push"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Mark queue entries as DONE
            String updateSql = "UPDATE sync_queue SET status='DONE' WHERE queue_id=?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                for (Map<String, Object> entry : pending) {
                    ps.setString(1, (String) entry.get("queue_id"));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return pending.size();
        } else if (response.statusCode() == 401) {
            throw new RuntimeException("Sync authentication failed. Check API key.");
        } else {
            throw new RuntimeException("Push failed: HTTP " + response.statusCode() + " " + response.body());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PULL — Get cloud changes and apply to local SQLite
    // ═══════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private int pullChanges() throws Exception {
        String since = lastSyncTime != null ? lastSyncTime.toString() : "1970-01-01T00:00:00Z";

        Map<String, Object> payload = Map.of(
            "storeId", storeId,
            "apiKey", apiKey,
            "since", since
        );

        String json = mapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/api/sync/pull"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Pull failed: HTTP " + response.statusCode());
        }

        Map<String, Object> result = mapper.readValue(response.body(), new TypeReference<>() {});
        Map<String, List<Map<String, Object>>> changes = (Map<String, List<Map<String, Object>>>) result.get("changes");

        if (changes == null || changes.isEmpty()) return 0;

        Connection conn = db.getConnection();
        int totalApplied = 0;

        try {
            conn.setAutoCommit(false);

            for (Map.Entry<String, List<Map<String, Object>>> tableEntry : changes.entrySet()) {
                String tableName = tableEntry.getKey();
                List<Map<String, Object>> rows = tableEntry.getValue();
                String pkCol = TABLE_PK.get(tableName);

                if (pkCol == null) {
                    // Special handling for composite PK tables (e.g., warehouse_balances)
                    if ("warehouse_balances".equals(tableName)) {
                        for (Map<String, Object> row : rows) {
                            try {
                                int applied = upsertWarehouseBalance(conn, row);
                                totalApplied += applied;
                            } catch (SQLException e) {
                                System.err.println("[SYNC] Pull warehouse_balances error: " + e.getMessage());
                            }
                        }
                    }
                    continue;
                }

                for (Map<String, Object> row : rows) {
                    try {
                        int applied = upsertLocalRow(conn, tableName, pkCol, row);
                        totalApplied += applied;
                    } catch (SQLException e) {
                        System.err.println("[SYNC] Pull apply error (" + tableName + "): " + e.getMessage());
                    }
                }
            }

            conn.commit();
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }

        return totalApplied;
    }

    // ═══════════════════════════════════════════════════════════════
    // REGISTER — Get API key from server
    // ═══════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    public Map<String, String> registerWithServer(String serverUrl, String storeId, String storeName) throws Exception {
        Map<String, Object> payload = Map.of(
            "storeId", storeId,
            "storeName", storeName != null ? storeName : storeId
        );

        String json = mapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/api/sync/register"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Registration failed: HTTP " + response.statusCode());
        }

        Map<String, Object> result = mapper.readValue(response.body(), new TypeReference<>() {});
        String newApiKey = (String) result.get("apiKey");

        // Save config
        configure(serverUrl, storeId, newApiKey);

        return Map.of(
            "storeId", storeId,
            "apiKey", newApiKey,
            "serverUrl", serverUrl
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // CHECK SERVER STATUS
    // ═══════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    public Map<String, Object> checkServerStatus() throws Exception {
        if (serverUrl == null || serverUrl.isBlank()) throw new RuntimeException("Server URL not configured");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/api/sync/status"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(response.body(), new TypeReference<>() {});
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private Map<String, Object> readRow(Connection conn, String tableName, String recordId) {
        String pkCol = TABLE_PK.get(tableName);
        if (pkCol == null) return null;

        // Handle composite key (warehouse_balances uses "warehouseId:productId")
        if (recordId.contains(":") && tableName.equals("warehouse_balances")) {
            String[] parts = recordId.split(":", 2);
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM warehouse_balances WHERE warehouse_id=? AND product_id=?")) {
                ps.setString(1, parts[0]);
                ps.setString(2, parts[1]);
                return resultSetToMap(ps);
            } catch (SQLException e) { return null; }
        }

        // Quote table name if it's a SQL reserved word
        String qt = "Order".equals(tableName) ? "\"Order\"" : tableName;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM " + qt + " WHERE " + pkCol + "=?")) {
            ps.setString(1, recordId);
            return resultSetToMap(ps);
        } catch (SQLException e) {
            System.err.println("[SYNC] readRow error " + tableName + "/" + recordId + ": " + e.getMessage());
            return null;
        }
    }

    private Map<String, Object> resultSetToMap(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            ResultSetMetaData meta = rs.getMetaData();
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                row.put(meta.getColumnName(i), rs.getObject(i));
            }
            return row;
        }
    }

    private int upsertLocalRow(Connection conn, String tableName, String pkCol, Map<String, Object> row) throws SQLException {
        Object pk = row.get(pkCol);
        if (pk == null) return 0;

        // Quote table name if it's a SQL reserved word
        String qt = "Order".equals(tableName) ? "\"Order\"" : tableName;

        int incomingVersion = 1;
        Object vObj = row.get("version");
        if (vObj instanceof Number) incomingVersion = ((Number) vObj).intValue();

        // Check if row exists locally and get its version (if version column exists)
        boolean rowExists = false;
        boolean hasVersionCol = true;
        int localVersion = -1;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT version FROM " + qt + " WHERE " + pkCol + "=?")) {
            ps.setString(1, pk.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    rowExists = true;
                    localVersion = rs.getInt("version");
                }
            }
        } catch (SQLException e) {
            // Table might not have 'version' column (child tables like OrderItem, CashPayment)
            hasVersionCol = false;
            // Check existence without version
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT 1 FROM " + qt + " WHERE " + pkCol + "=?")) {
                ps2.setString(1, pk.toString());
                try (ResultSet rs2 = ps2.executeQuery()) {
                    rowExists = rs2.next();
                }
            } catch (SQLException e2) {
                return 0; // Table doesn't exist
            }
        }

        // For tables with version: skip if local is same or newer
        if (hasVersionCol && rowExists && localVersion >= incomingVersion) {
            return 0;
        }

        // Filter out columns that don't exist in local SQLite table
        Map<String, Object> filteredRow = filterExistingColumns(conn, tableName, row);

        // Mark as SYNCED if column exists
        if (filteredRow.containsKey("sync_status")) {
            filteredRow.put("sync_status", "SYNCED");
        }

        if (!rowExists) {
            // INSERT
            StringBuilder cols = new StringBuilder();
            StringBuilder vals = new StringBuilder();
            List<Object> params = new ArrayList<>();

            for (Map.Entry<String, Object> e : filteredRow.entrySet()) {
                if (cols.length() > 0) { cols.append(","); vals.append(","); }
                cols.append(e.getKey());
                vals.append("?");
                params.add(e.getValue());
            }

            String sql = "INSERT OR IGNORE INTO " + qt + " (" + cols + ") VALUES (" + vals + ")";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                setParams(ps, params);
                return ps.executeUpdate();
            }
        } else {
            // UPDATE
            StringBuilder setClause = new StringBuilder();
            List<Object> params = new ArrayList<>();

            for (Map.Entry<String, Object> e : filteredRow.entrySet()) {
                if (e.getKey().equals(pkCol)) continue;
                if (setClause.length() > 0) setClause.append(",");
                setClause.append(e.getKey()).append("=?");
                params.add(e.getValue());
            }

            if (setClause.length() == 0) return 0;

            params.add(pk.toString());
            String sql = "UPDATE " + qt + " SET " + setClause + " WHERE " + pkCol + "=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                setParams(ps, params);
                return ps.executeUpdate();
            }
        }
    }

    /** Filter row map to only include columns that exist in the local SQLite table */
    private Map<String, Object> filterExistingColumns(Connection conn, String tableName, Map<String, Object> row) {
        Set<String> existingCols = new HashSet<>();
        String pragmaTable = "Order".equals(tableName) ? "\"Order\"" : tableName;
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("PRAGMA table_info(" + pragmaTable + ")")) {
            while (rs.next()) {
                existingCols.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            return row; // If we can't check, pass all columns
        }

        if (existingCols.isEmpty()) return row;

        Map<String, Object> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (existingCols.contains(e.getKey())) {
                filtered.put(e.getKey(), e.getValue());
            }
        }
        return filtered;
    }

    /** Special upsert for warehouse_balances (composite PK: warehouse_id + product_id) */
    private int upsertWarehouseBalance(Connection conn, Map<String, Object> row) throws SQLException {
        Object whId = row.get("warehouse_id");
        Object prodId = row.get("product_id");
        if (whId == null || prodId == null) return 0;

        Map<String, Object> filtered = filterExistingColumns(conn, "warehouse_balances", row);
        filtered.put("sync_status", "SYNCED");

        String sql = "INSERT OR REPLACE INTO warehouse_balances (warehouse_id, product_id, on_hand_qty, reserved_qty, " +
                "updated_at, last_modified, sync_status) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, whId.toString());
            ps.setString(2, prodId.toString());
            ps.setObject(3, filtered.getOrDefault("on_hand_qty", 0));
            ps.setObject(4, filtered.getOrDefault("reserved_qty", 0));
            ps.setObject(5, filtered.getOrDefault("updated_at", ""));
            ps.setObject(6, filtered.getOrDefault("last_modified", ""));
            ps.setString(7, "SYNCED");
            return ps.executeUpdate();
        }
    }

    private void setParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object v = params.get(i);
            if (v == null) ps.setNull(i + 1, Types.VARCHAR);
            else ps.setObject(i + 1, v);
        }
    }

    private String getConfig(Connection conn, String key) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT value FROM app_config WHERE key=?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException ignored) {}
        return null;
    }

    private void saveConfig(String key, String value) {
        try {
            Connection conn = db.getConnection();
            String sql = "INSERT OR REPLACE INTO app_config(key,value,updated_at) VALUES(?,?,datetime('now'))";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, key);
                ps.setString(2, value);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("[SYNC] Config save error: " + e.getMessage());
        }
    }

    private void setState(SyncState newState) {
        state = newState;
        if (stateListener != null) {
            try { stateListener.accept(newState); }
            catch (Exception e) { System.err.println("[SYNC] Listener error: " + e.getMessage()); }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════

    public SyncState getState()       { return state; }
    public String getLastError()      { return lastError; }
    public Instant getLastSyncTime()  { return lastSyncTime; }
    public String getServerUrl()      { return serverUrl; }
    public String getStoreId()        { return storeId; }
    public String getApiKey()         { return apiKey; }

    public int getPendingCount() {
        try {
            Connection conn = db.getConnection();
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM sync_queue WHERE status='PENDING'")) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception ignored) {}
        return 0;
    }
}

