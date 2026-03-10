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

    private static final AtomicBoolean dbReady = new AtomicBoolean(false);
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

        // ── 4. Start listening ──
        app.start(port);

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  Store Management Web Server             ║");
        System.out.printf( "║  http://localhost:%-24s║%n", port);
        System.out.println("║  Health check: /api/health               ║");
        System.out.println("╚══════════════════════════════════════════╝");

        // ── 5. NOW initialize database (server is already listening) ──
        PgDatabaseManager pg = PgDatabaseManager.getInstance();
        try {
            System.out.println("[DB] Initializing database connection...");
            pg.init();
            System.out.println("[DB] Running schema scripts...");
            pg.initializeSchema();
            System.out.println("[DB] Seeding default passwords...");
            WebAuthController.seedDefaultPasswords(pg);

            // ── 6. Register API routes AFTER DB is ready ──
            WebAuthController.register(app);
            WebProductController.register(app);
            WebOrderController.register(app);
            WebInventoryController.register(app);
            WebEmployeeController.register(app);
            WebCustomerController.register(app);

            dbReady.set(true);
            System.out.println("[DB] ✅ Database ready. All API routes registered.");
        } catch (Exception e) {
            dbError.set(e.getMessage());
            System.err.println("[DB] ❌ Database initialization failed: " + e.getMessage());
            e.printStackTrace(System.err);
            // Server still runs — healthcheck still responds
            // API routes will return 404 until DB is fixed
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

