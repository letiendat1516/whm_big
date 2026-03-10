package org.example.dao;

import org.example.db.DatabaseManager;
import org.example.model.Account;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DAO xử lý đăng nhập, tài khoản, phân quyền.
 */
public class AuthDAO {

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ═══════════════════════════════════════════════════════════
    //  FIND ACCOUNT
    // ═══════════════════════════════════════════════════════════

    /** Tìm account theo username (case-insensitive) */
    public Account findByUsername(String username) throws SQLException {
        String sql = "SELECT a.*, r.roleName, r.permissions " +
                     "FROM Account a JOIN Role r ON a.roleId = r.roleId " +
                     "WHERE a.username = ? COLLATE NOCASE AND a.isActive = 1";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapAccount(rs) : null;
            }
        }
    }

    /** Tìm account theo ID */
    public Account findById(String accountId) throws SQLException {
        String sql = "SELECT a.*, r.roleName, r.permissions " +
                     "FROM Account a JOIN Role r ON a.roleId = r.roleId " +
                     "WHERE a.accountId = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapAccount(rs) : null;
            }
        }
    }

    /** Lấy toàn bộ accounts */
    public List<Account> findAll() throws SQLException {
        String sql = "SELECT a.*, r.roleName, r.permissions " +
                     "FROM Account a JOIN Role r ON a.roleId = r.roleId " +
                     "ORDER BY a.createdAt DESC";
        List<Account> list = new ArrayList<>();
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapAccount(rs));
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════
    //  SAVE / UPDATE / DELETE
    // ═══════════════════════════════════════════════════════════

    /** Thêm mới account */
    public void insert(Account a) throws SQLException {
        String sql = "INSERT INTO Account(accountId,username,passwordHash,roleId,employeeId,storeId,fullName,isActive) " +
                     "VALUES(?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, a.getAccountId());
            ps.setString(2, a.getUsername());
            ps.setString(3, a.getPasswordHash());
            ps.setString(4, a.getRoleId());
            ps.setString(5, a.getEmployeeId());
            ps.setString(6, a.getStoreId());
            ps.setString(7, a.getFullName());
            ps.setInt(8, a.isActive() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    /** Cập nhật account (không đổi password) */
    public void update(Account a) throws SQLException {
        String sql = "UPDATE Account SET username=?, roleId=?, employeeId=?, storeId=?, " +
                     "fullName=?, isActive=?, updatedAt=datetime('now') WHERE accountId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, a.getUsername());
            ps.setString(2, a.getRoleId());
            ps.setString(3, a.getEmployeeId());
            ps.setString(4, a.getStoreId());
            ps.setString(5, a.getFullName());
            ps.setInt(6, a.isActive() ? 1 : 0);
            ps.setString(7, a.getAccountId());
            ps.executeUpdate();
        }
    }

    /** Đổi mật khẩu */
    public void updatePassword(String accountId, String newHash) throws SQLException {
        String sql = "UPDATE Account SET passwordHash=?, updatedAt=datetime('now') WHERE accountId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, accountId);
            ps.executeUpdate();
        }
    }

    /** Cập nhật thời gian đăng nhập cuối */
    public void updateLastLogin(String accountId) throws SQLException {
        String sql = "UPDATE Account SET lastLoginAt=datetime('now') WHERE accountId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, accountId);
            ps.executeUpdate();
        }
    }

    /** Xóa account */
    public void delete(String accountId) throws SQLException {
        String sql = "DELETE FROM Account WHERE accountId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, accountId);
            ps.executeUpdate();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  LOGIN LOG
    // ═══════════════════════════════════════════════════════════

    public void logLogin(String accountId, boolean success, String note) throws SQLException {
        String sql = "INSERT INTO LoginLog(logId,accountId,success,note) VALUES(?,?,?,?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, DatabaseManager.newId());
            ps.setString(2, accountId);
            ps.setInt(3, success ? 1 : 0);
            ps.setString(4, note);
            ps.executeUpdate();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  ROLES
    // ═══════════════════════════════════════════════════════════

    public List<String[]> findAllRoles() throws SQLException {
        String sql = "SELECT roleId, roleName, description FROM Role ORDER BY roleName";
        List<String[]> list = new ArrayList<>();
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("roleId"),
                    rs.getString("roleName"),
                    rs.getString("description")
                });
            }
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════
    //  MAPPER
    // ═══════════════════════════════════════════════════════════

    private Account mapAccount(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setAccountId(rs.getString("accountId"));
        a.setUsername(rs.getString("username"));
        a.setPasswordHash(rs.getString("passwordHash"));
        a.setRoleId(rs.getString("roleId"));
        a.setRoleName(rs.getString("roleName"));
        a.setEmployeeId(rs.getString("employeeId"));
        a.setStoreId(rs.getString("storeId"));
        a.setFullName(rs.getString("fullName"));
        a.setActive(rs.getInt("isActive") == 1);
        a.setLastLoginAt(rs.getString("lastLoginAt"));

        // Parse permissions JSON -> List<String>  e.g. ["POS","HR"]
        String permJson = rs.getString("permissions");
        if (permJson != null && permJson.length() > 2) {
            // Remove brackets and quotes, split by comma
            String inner = permJson.replace("[", "").replace("]", "")
                                   .replace("\"", "").replace("'", "").trim();
            if (!inner.isEmpty()) {
                a.setPermissions(Arrays.asList(inner.split("\\s*,\\s*")));
            } else {
                a.setPermissions(new ArrayList<>());
            }
        } else {
            a.setPermissions(new ArrayList<>());
        }
        return a;
    }
}

