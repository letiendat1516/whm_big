package org.example.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.db.PgDatabaseManager;
import org.example.service.PasswordUtil;

import java.sql.*;
import java.util.*;

/**
 * Authentication & Authorization REST API.
 * Uses session cookie for state management.
 */
public class WebAuthController {

    private static final PgDatabaseManager pg = PgDatabaseManager.getInstance();

    public static void register(Javalin app) {
        app.post("/api/auth/login",  WebAuthController::login);
        app.post("/api/auth/logout", WebAuthController::logout);
        app.get("/api/auth/me",      WebAuthController::me);
        app.get("/api/auth/roles",   WebAuthController::listRoles);

        // CRUD accounts
        app.get("/api/accounts",      WebAuthController::listAccounts);
        app.post("/api/accounts",     WebAuthController::createAccount);
        app.put("/api/accounts/{id}", WebAuthController::updateAccount);
        app.delete("/api/accounts/{id}", WebAuthController::deleteAccount);
    }

    /** Seed default passwords for demo accounts */
    public static void seedDefaultPasswords(PgDatabaseManager db) {
        try (Connection conn = db.getConnection()) {
            String check = "SELECT \"accountId\", \"passwordHash\" FROM Account WHERE \"passwordHash\"='NEEDS_HASH'";
            List<String> ids = new ArrayList<>();
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(check)) {
                while (rs.next()) ids.add(rs.getString("accountId"));
            }
            if (!ids.isEmpty()) {
                String hash = PasswordUtil.hash("123456"); // default password
                String upd = "UPDATE Account SET \"passwordHash\"=? WHERE \"accountId\"=?";
                try (PreparedStatement ps = conn.prepareStatement(upd)) {
                    for (String id : ids) {
                        ps.setString(1, hash);
                        ps.setString(2, id);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                System.out.println("[AUTH] Seeded passwords for " + ids.size() + " accounts (default: 123456)");
            }
        } catch (Exception e) {
            System.err.println("[AUTH] Seed error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════
    // LOGIN / LOGOUT / ME
    // ═══════════════════════════════════════

    private static void login(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String username = (String) body.get("username");
            String password = (String) body.get("password");

            if (username == null || password == null) {
                ctx.status(400).json(Map.of("error", "Username và password là bắt buộc"));
                return;
            }

            try (Connection conn = pg.getConnection()) {
                String sql = "SELECT a.\"accountId\", a.username, a.\"passwordHash\", a.\"roleId\", " +
                        "a.\"employeeId\", a.\"storeId\", a.\"fullName\", a.\"isActive\", " +
                        "r.\"roleName\", r.permissions " +
                        "FROM Account a JOIN Role r ON a.\"roleId\" = r.\"roleId\" " +
                        "WHERE LOWER(a.username) = LOWER(?) AND a.\"isActive\" = 1";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            ctx.status(401).json(Map.of("error", "Tài khoản không tồn tại hoặc đã bị khóa"));
                            return;
                        }

                        String storedHash = rs.getString("passwordHash");
                        if (!PasswordUtil.verify(password, storedHash)) {
                            ctx.status(401).json(Map.of("error", "Sai mật khẩu"));
                            return;
                        }

                        // Build session
                        Map<String, Object> user = new HashMap<>();
                        user.put("accountId", rs.getString("accountId"));
                        user.put("username", rs.getString("username"));
                        user.put("roleId", rs.getString("roleId"));
                        user.put("roleName", rs.getString("roleName"));
                        user.put("employeeId", rs.getString("employeeId"));
                        user.put("storeId", rs.getString("storeId"));
                        user.put("fullName", rs.getString("fullName"));

                        String permJson = rs.getString("permissions");
                        List<String> perms = parsePermissions(permJson);
                        user.put("permissions", perms);

                        ctx.sessionAttribute("user", user);

                        // Update last login
                        try (PreparedStatement up = conn.prepareStatement(
                                "UPDATE Account SET \"lastLoginAt\"=NOW() WHERE \"accountId\"=?")) {
                            up.setString(1, (String) user.get("accountId"));
                            up.executeUpdate();
                        }

                        ctx.json(Map.of("success", true, "user", user));
                    }
                }
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void logout(Context ctx) {
        ctx.req().getSession().invalidate();
        ctx.json(Map.of("success", true));
    }

    @SuppressWarnings("unchecked")
    private static void me(Context ctx) {
        Map<String, Object> user = ctx.sessionAttribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Chưa đăng nhập"));
        } else {
            ctx.json(Map.of("user", user));
        }
    }

    // ═══════════════════════════════════════
    // ROLES
    // ═══════════════════════════════════════

    private static void listRoles(Context ctx) {
        try (Connection conn = pg.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT \"roleId\",\"roleName\",description FROM Role ORDER BY \"roleName\"")) {
            List<Map<String, String>> roles = new ArrayList<>();
            while (rs.next()) {
                roles.add(Map.of(
                    "roleId", rs.getString("roleId"),
                    "roleName", rs.getString("roleName"),
                    "description", Optional.ofNullable(rs.getString("description")).orElse("")
                ));
            }
            ctx.json(roles);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════
    // CRUD ACCOUNTS
    // ═══════════════════════════════════════

    private static void listAccounts(Context ctx) {
        try (Connection conn = pg.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(
                "SELECT a.\"accountId\", a.username, a.\"roleId\", r.\"roleName\", " +
                "a.\"fullName\", a.\"isActive\", a.\"employeeId\", a.\"storeId\" " +
                "FROM Account a JOIN Role r ON a.\"roleId\"=r.\"roleId\" ORDER BY a.\"createdAt\" DESC")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("accountId", rs.getString("accountId"));
                m.put("username", rs.getString("username"));
                m.put("roleId", rs.getString("roleId"));
                m.put("roleName", rs.getString("roleName"));
                m.put("fullName", rs.getString("fullName"));
                m.put("isActive", rs.getInt("isActive") == 1);
                m.put("employeeId", rs.getString("employeeId"));
                m.put("storeId", rs.getString("storeId"));
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void createAccount(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String id = PgDatabaseManager.newId();
            String hash = PasswordUtil.hash((String) body.getOrDefault("password", "123456"));

            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Account(\"accountId\",username,\"passwordHash\",\"roleId\",\"employeeId\",\"storeId\",\"fullName\",\"isActive\") " +
                    "VALUES(?,?,?,?,?,?,?,?)")) {
                ps.setString(1, id);
                ps.setString(2, (String) body.get("username"));
                ps.setString(3, hash);
                ps.setString(4, (String) body.get("roleId"));
                ps.setString(5, (String) body.get("employeeId"));
                ps.setString(6, (String) body.get("storeId"));
                ps.setString(7, (String) body.get("fullName"));
                ps.setInt(8, 1);
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true, "accountId", id));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void updateAccount(Context ctx) {
        try {
            String id = ctx.pathParam("id");
            Map body = ctx.bodyAsClass(Map.class);

            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Account SET username=?, \"roleId\"=?, \"employeeId\"=?, " +
                    "\"storeId\"=?, \"fullName\"=?, \"isActive\"=?, \"updatedAt\"=NOW() " +
                    "WHERE \"accountId\"=?")) {
                ps.setString(1, (String) body.get("username"));
                ps.setString(2, (String) body.get("roleId"));
                ps.setString(3, (String) body.get("employeeId"));
                ps.setString(4, (String) body.get("storeId"));
                ps.setString(5, (String) body.get("fullName"));
                ps.setInt(6, Boolean.TRUE.equals(body.get("isActive")) ? 1 : 0);
                ps.setString(7, id);
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void deleteAccount(Context ctx) {
        try {
            String id = ctx.pathParam("id");
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM Account WHERE \"accountId\"=?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════

    private static List<String> parsePermissions(String json) {
        List<String> list = new ArrayList<>();
        if (json != null && json.length() > 2) {
            String inner = json.replace("[", "").replace("]", "")
                    .replace("\"", "").replace("'", "").trim();
            if (!inner.isEmpty()) {
                list.addAll(Arrays.asList(inner.split("\\s*,\\s*")));
            }
        }
        return list;
    }

    /** Utility: check if current session has a specific permission */
    @SuppressWarnings("unchecked")
    public static boolean hasPermission(Context ctx, String module) {
        Map<String, Object> user = ctx.sessionAttribute("user");
        if (user == null) return false;
        String roleName = (String) user.get("roleName");
        if ("ADMIN".equals(roleName)) return true;
        List<String> perms = (List<String>) user.get("permissions");
        return perms != null && perms.contains(module);
    }

    /** Utility: require login, return 401 if not */
    public static boolean requireAuth(Context ctx) {
        if (ctx.sessionAttribute("user") == null) {
            ctx.status(401).json(Map.of("error", "Chưa đăng nhập"));
            return false;
        }
        return true;
    }
}

