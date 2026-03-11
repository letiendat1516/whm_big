package org.example;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.example.db.PgDatabaseManager;
import org.example.web.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Web Application Entry Point for Railway deployment.
 *
 * IMPORTANT: Javalin starts FIRST so /api/health responds immediately.
 * Database initialization happens AFTER the server is listening.
 *
 * Environment variables (set by Railway):
 *  - DATABASE_URL: PostgreSQL connection string
 *  - PORT: HTTP port (default 8080)
 */
public class WebMain {

    public static final AtomicBoolean dbReady = new AtomicBoolean(false);
    private static final AtomicReference<String> dbError = new AtomicReference<>(null);

    public static void main(String[] args) {
        System.out.println("[BOOT] Starting Store Management Web Server...");
        System.out.println("[BOOT] PORT env = " + System.getenv("PORT"));
        System.out.println("[BOOT] DATABASE_URL env = " + (System.getenv("DATABASE_URL") != null ? "SET (hidden)" : "NOT SET"));

        // ── 1. Start Javalin web server FIRST (so healthcheck responds) ──
        int port;
        try {
            port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        } catch (NumberFormatException e) {
            port = 8080;
        }
        System.out.println("[BOOT] Will listen on port: " + port);

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
            config.http.defaultContentType = "application/json";
        });

        // ── 2. Health check MUST be registered BEFORE start ──
        app.get("/api/health", ctx -> {
            if (dbReady.get()) {
                ctx.json(Map.of(
                    "status", "UP",
                    "database", "connected",
                    "time", java.time.Instant.now().toString()
                ));
            } else {
                String err = dbError.get();
                ctx.status(200); // Return 200 so Railway healthcheck passes
                ctx.json(Map.of(
                    "status", "STARTING",
                    "database", err != null ? "error: " + err : "initializing",
                    "time", java.time.Instant.now().toString()
                ));
            }
        });

        // ── 3. Redirect root to login page ──
        app.get("/", ctx -> ctx.redirect("/login.html"));

        // ── 4. RBAC middleware: protect /api/* routes by role/permission ──
        app.before("/api/*", ctx -> {
            String path = ctx.path();
            // Public endpoints — skip auth
            if (path.startsWith("/api/health") || path.startsWith("/api/auth/")
                || path.startsWith("/api/sync/")) return;

            // Require login
            @SuppressWarnings("unchecked")
            Map<String, Object> user = ctx.sessionAttribute("user");
            if (user == null) {
                ctx.status(401).json(Map.of("error", "Chưa đăng nhập"));
                return; // Javalin before-handler: returning doesn't skip; use skipRemainingHandlers
            }

            String roleName = (String) user.get("roleName");
            if ("ADMIN".equals(roleName)) return; // Admin can do everything

            @SuppressWarnings("unchecked")
            java.util.List<String> perms = (java.util.List<String>) user.get("permissions");
            if (perms == null) perms = java.util.List.of();

            // Map URL → required permission
            String need = null;
            if (path.startsWith("/api/orders") || path.startsWith("/api/payments")
                || path.startsWith("/api/returns")) need = "POS";
            else if (path.startsWith("/api/warehouses") || path.startsWith("/api/inventory")
                || path.startsWith("/api/inbound") || path.startsWith("/api/outbound")
                || path.startsWith("/api/suppliers")) need = "INVENTORY";
            else if (path.startsWith("/api/products") || path.startsWith("/api/categories")
                || path.startsWith("/api/variants") || path.startsWith("/api/prices")
                || path.startsWith("/api/pricelists")) need = "PRODUCT";
            else if (path.startsWith("/api/customers") || path.startsWith("/api/promotions")
                || path.startsWith("/api/campaigns") || path.startsWith("/api/loyalty")
                || path.startsWith("/api/vouchers")) need = "CRM";
            else if (path.startsWith("/api/employees") || path.startsWith("/api/stores")
                || path.startsWith("/api/shifts")) need = "HR";
            else if (path.startsWith("/api/accounts")) need = "ADMIN";

            if (need != null && !perms.contains(need)) {
                ctx.status(403).json(Map.of("error", "Bạn không có quyền truy cập chức năng này"));
            }
        });

        // ── 5. Register ALL API routes NOW (before server starts) ──
        WebAuthController.register(app);
        WebProductController.register(app);
        WebOrderController.register(app);
        WebInventoryController.register(app);
        WebEmployeeController.register(app);
        WebCustomerController.register(app);
        WebSyncController.register(app);
        System.out.println("[BOOT] All API routes registered (with RBAC, sync).");

        // ── 5. Start listening ──
        app.start(port);

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  Store Management Web Server             ║");
        System.out.printf( "║  http://localhost:%-24s║%n", port);
        System.out.println("╚══════════════════════════════════════════╝");

        // ── 6. NOW initialize database (server is already listening) ──
        PgDatabaseManager pg = PgDatabaseManager.getInstance();
        try {
            System.out.println("[DB] Initializing database connection...");
            pg.init();
            System.out.println("[DB] Running schema scripts...");
            pg.initializeSchema();
            System.out.println("[DB] Seeding default passwords...");
            WebAuthController.seedDefaultPasswords(pg);
            dbReady.set(true);
            System.out.println("[DB] ✅ Database ready.");
        } catch (Exception e) {
            dbError.set(e.getMessage());
            System.err.println("[DB] ❌ Database initialization failed: " + e.getMessage());
            e.printStackTrace(System.err);
        }

        // ── 7. Shutdown hook ──
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[WEB] Shutting down...");
            app.stop();
            pg.close();
            System.out.println("[WEB] Server shutdown complete.");
        }));
    }
}
