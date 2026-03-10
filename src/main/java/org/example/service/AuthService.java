package org.example.service;

import org.example.dao.AuthDAO;
import org.example.db.DatabaseManager;
import org.example.model.Account;
import org.example.model.UserSession;

import java.sql.SQLException;
import java.util.List;

/**
 * Service xác thực & quản lý tài khoản.
 */
public class AuthService {

    private final AuthDAO authDAO = new AuthDAO();

    // ═══════════════════════════════════════════════════════════
    //  LOGIN / LOGOUT
    // ═══════════════════════════════════════════════════════════

    /**
     * Đăng nhập. Trả về Account nếu thành công, ném exception nếu lỗi.
     */
    public Account login(String username, String password) throws Exception {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Vui lòng nhập tên đăng nhập");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu");

        Account account = authDAO.findByUsername(username.trim());
        if (account == null) {
            throw new SecurityException("Tài khoản không tồn tại");
        }

        if (!account.isActive()) {
            throw new SecurityException("Tài khoản đã bị khóa. Liên hệ quản trị viên.");
        }

        // Verify password
        if (!PasswordUtil.verify(password, account.getPasswordHash())) {
            authDAO.logLogin(account.getAccountId(), false, "Wrong password");
            throw new SecurityException("Mật khẩu không đúng");
        }

        // Login success
        authDAO.updateLastLogin(account.getAccountId());
        authDAO.logLogin(account.getAccountId(), true, null);

        // Set session
        UserSession.current().login(account);

        return account;
    }

    /** Đăng xuất */
    public void logout() {
        UserSession.current().logout();
    }

    // ═══════════════════════════════════════════════════════════
    //  ACCOUNT MANAGEMENT (ADMIN only)
    // ═══════════════════════════════════════════════════════════

    /** Tạo tài khoản mới */
    public Account createAccount(String username, String password, String roleId,
                                  String fullName, String employeeId, String storeId) throws Exception {
        // Validate
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username không được trống");
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Mật khẩu tối thiểu 6 ký tự");

        // Check duplicate
        Account existing = authDAO.findByUsername(username.trim());
        if (existing != null)
            throw new IllegalArgumentException("Username '" + username + "' đã tồn tại");

        Account account = new Account();
        account.setAccountId(DatabaseManager.newId());
        account.setUsername(username.trim());
        account.setPasswordHash(PasswordUtil.hash(password));
        account.setRoleId(roleId);
        account.setFullName(fullName);
        account.setEmployeeId(employeeId);
        account.setStoreId(storeId);
        account.setActive(true);

        authDAO.insert(account);
        return account;
    }

    /** Cập nhật tài khoản */
    public void updateAccount(Account account) throws SQLException {
        authDAO.update(account);
    }

    /** Đổi mật khẩu */
    public void changePassword(String accountId, String oldPassword, String newPassword) throws Exception {
        Account account = authDAO.findById(accountId);
        if (account == null) throw new IllegalArgumentException("Tài khoản không tồn tại");

        if (!PasswordUtil.verify(oldPassword, account.getPasswordHash())) {
            throw new SecurityException("Mật khẩu cũ không đúng");
        }

        if (newPassword == null || newPassword.length() < 6)
            throw new IllegalArgumentException("Mật khẩu mới tối thiểu 6 ký tự");

        String hash = PasswordUtil.hash(newPassword);
        authDAO.updatePassword(accountId, hash);
    }

    /** Admin reset mật khẩu (không cần mật khẩu cũ) */
    public void resetPassword(String accountId, String newPassword) throws Exception {
        if (newPassword == null || newPassword.length() < 6)
            throw new IllegalArgumentException("Mật khẩu mới tối thiểu 6 ký tự");
        String hash = PasswordUtil.hash(newPassword);
        authDAO.updatePassword(accountId, hash);
    }

    /** Xóa tài khoản */
    public void deleteAccount(String accountId) throws SQLException {
        authDAO.delete(accountId);
    }

    /** Khóa/Mở khóa tài khoản */
    public void toggleActive(String accountId) throws SQLException {
        Account a = authDAO.findById(accountId);
        if (a != null) {
            a.setActive(!a.isActive());
            authDAO.update(a);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  QUERIES
    // ═══════════════════════════════════════════════════════════

    public List<Account> getAllAccounts() throws SQLException {
        return authDAO.findAll();
    }

    public List<String[]> getAllRoles() throws SQLException {
        return authDAO.findAllRoles();
    }
}

