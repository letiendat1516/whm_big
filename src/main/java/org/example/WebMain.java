package org.example;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.example.db.PgDatabaseManager;
import org.example.web.*;

/**
 * Web Application Entry Point for Railway deployment.
 *
 * Serves:
 *  - REST API endpoints (/api/...)
 *  - Static HTML/JS/CSS frontend (/public/...)
 *  - Session-based authentication with role-based access
 *
 * Environment variables (set by Railway):
 *  - DATABASE_URL: PostgreSQL connection string
 *  - PORT: HTTP port (default 8080)
 */
public class WebMain {

    public static void main(String[] args) {
        // ── 1. Initialize PostgreSQL ──
        PgDatabaseManager pg = PgDatabaseManager.getInstance();
        pg.init();
        pg.initializeSchema();

        // ── 2. Seed default admin password if needed ──
        WebAuthController.seedDefaultPasswords(pg);

        // ── 3. Start Javalin web server ──
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
            config.http.defaultContentType = "application/json";
        }).start(port);

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  Store Management Web Server             ║");
        System.out.println("║  http://localhost:" + port + "                  ║");
        System.out.println("╚══════════════════════════════════════════╝");

        // ── 4. Register API routes ──
        WebAuthController.register(app);
        WebProductController.register(app);
        WebOrderController.register(app);
        WebInventoryController.register(app);
        WebEmployeeController.register(app);
        WebCustomerController.register(app);

        // ── 5. Health check ──
        app.get("/api/health", ctx -> ctx.json(java.util.Map.of(
            "status", "UP",
            "time", java.time.Instant.now().toString()
        )));

        // ── 6. Redirect root to login page ──
        app.get("/", ctx -> ctx.redirect("/login.html"));

        // ── 7. Shutdown hook ──
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            pg.close();
            System.out.println("[WEB] Server shutdown complete.");
        }));
    }
}

