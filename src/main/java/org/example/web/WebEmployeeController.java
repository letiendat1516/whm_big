package org.example.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.db.PgDatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * Employee & HR REST API controller.
 */
public class WebEmployeeController {

    private static final PgDatabaseManager pg = PgDatabaseManager.getInstance();

    public static void register(Javalin app) {
        app.get("/api/employees",         WebEmployeeController::listEmployees);
        app.get("/api/employees/{id}",    WebEmployeeController::getEmployee);
        app.post("/api/employees",        WebEmployeeController::createEmployee);
        app.put("/api/employees/{id}",    WebEmployeeController::updateEmployee);
        app.delete("/api/employees/{id}", WebEmployeeController::deleteEmployee);

        app.get("/api/stores",            WebEmployeeController::listStores);
        app.get("/api/shifts",            WebEmployeeController::listShiftTemplates);
    }

    private static void listEmployees(Context ctx) {
        try (Connection conn = pg.getConnection()) {
            String search = ctx.queryParam("search");
            String sql = "SELECT e.*, ea.\"storeId\", s.name as store_name " +
                    "FROM Employee e " +
                    "LEFT JOIN EmployeeAssignment ea ON ea.\"employeeId\"=e.\"employeeId\" AND ea.status='ACTIVE' " +
                    "LEFT JOIN Store s ON s.\"storeId\"=ea.\"storeId\" ";
            if (search != null && !search.isBlank()) {
                sql += "WHERE LOWER(e.\"fullName\") LIKE '%" + search.toLowerCase().replace("'","") + "%' " +
                       "OR LOWER(e.\"employeeCode\") LIKE '%" + search.toLowerCase().replace("'","") + "%' ";
            }
            sql += "ORDER BY e.\"employeeCode\"";

            List<Map<String, Object>> list = new ArrayList<>();
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("employeeId", rs.getString("employeeId"));
                    m.put("employeeCode", rs.getString("employeeCode"));
                    m.put("fullName", rs.getString("fullName"));
                    m.put("hireDate", rs.getString("hireDate"));
                    m.put("baseSalary", rs.getBigDecimal("baseSalary"));
                    m.put("status", rs.getString("status"));
                    m.put("storeId", rs.getString("storeId"));
                    m.put("storeName", rs.getString("store_name"));
                    list.add(m);
                }
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void getEmployee(Context ctx) {
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM Employee WHERE \"employeeId\"=?")) {
            ps.setString(1, ctx.pathParam("id"));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("employeeId", rs.getString("employeeId"));
                    m.put("employeeCode", rs.getString("employeeCode"));
                    m.put("fullName", rs.getString("fullName"));
                    m.put("hireDate", rs.getString("hireDate"));
                    m.put("baseSalary", rs.getBigDecimal("baseSalary"));
                    m.put("status", rs.getString("status"));
                    ctx.json(m);
                } else {
                    ctx.status(404).json(Map.of("error", "Not found"));
                }
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void createEmployee(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            String id = "EMP-" + PgDatabaseManager.newId().substring(0, 6);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Employee(\"employeeId\",\"employeeCode\",\"fullName\",\"hireDate\",\"baseSalary\",status) " +
                    "VALUES(?,?,?,?,?,?)")) {
                ps.setString(1, id);
                ps.setString(2, (String) body.get("employeeCode"));
                ps.setString(3, (String) body.get("fullName"));
                ps.setString(4, (String) body.getOrDefault("hireDate", java.time.LocalDate.now().toString()));
                ps.setBigDecimal(5, new java.math.BigDecimal(body.getOrDefault("baseSalary", "0").toString()));
                ps.setString(6, (String) body.getOrDefault("status", "ACTIVE"));
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true, "employeeId", id));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void updateEmployee(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            try (Connection conn = pg.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Employee SET \"employeeCode\"=?, \"fullName\"=?, \"hireDate\"=?, " +
                    "\"baseSalary\"=?, status=? WHERE \"employeeId\"=?")) {
                ps.setString(1, (String) body.get("employeeCode"));
                ps.setString(2, (String) body.get("fullName"));
                ps.setString(3, (String) body.get("hireDate"));
                ps.setBigDecimal(4, new java.math.BigDecimal(body.getOrDefault("baseSalary", "0").toString()));
                ps.setString(5, (String) body.getOrDefault("status", "ACTIVE"));
                ps.setString(6, ctx.pathParam("id"));
                ps.executeUpdate();
            }
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void deleteEmployee(Context ctx) {
        try (Connection conn = pg.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM Employee WHERE \"employeeId\"=?")) {
            ps.setString(1, ctx.pathParam("id"));
            ps.executeUpdate();
            ctx.json(Map.of("success", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void listStores(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT * FROM Store ORDER BY name")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("storeId", rs.getString("storeId"));
                m.put("storeCode", rs.getString("storeCode"));
                m.put("name", rs.getString("name"));
                m.put("address", rs.getString("address"));
                m.put("status", rs.getString("status"));
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void listShiftTemplates(Context ctx) {
        try (Connection conn = pg.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT * FROM ShiftTemplate ORDER BY name")) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("shiftTemplateId", rs.getString("shiftTemplateId"));
                m.put("name", rs.getString("name"));
                m.put("startTime", rs.getString("startTime"));
                m.put("endTime", rs.getString("endTime"));
                m.put("breakMinutes", rs.getInt("breakMinutes"));
                m.put("status", rs.getString("status"));
                list.add(m);
            }
            ctx.json(list);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }
}

