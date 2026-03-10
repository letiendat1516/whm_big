package org.example.model;

/**
 * Singleton giữ thông tin user đang đăng nhập trong phiên hiện tại.
 * Dùng ở bất kỳ đâu trong app: UserSession.current().getAccount()
 */
public class UserSession {

    private static UserSession instance;

    private Account currentAccount;
    private String  loginAt;

    private UserSession() {}

    public static UserSession current() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public static void reset() {
        instance = new UserSession();
    }

    // ── Login / Logout ────────────────────────────────────────
    public void login(Account account) {
        this.currentAccount = account;
        this.loginAt = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    public void logout() {
        this.currentAccount = null;
        this.loginAt = null;
    }

    public boolean isLoggedIn() {
        return currentAccount != null;
    }

    // ── Getters ───────────────────────────────────────────────
    public Account getAccount()  { return currentAccount; }
    public String  getLoginAt()  { return loginAt; }

    /** Kiểm tra quyền truy cập module nhanh */
    public boolean canAccess(String module) {
        if (currentAccount == null) return false;
        if (currentAccount.isAdmin()) return true;
        return currentAccount.hasPermission(module);
    }

    public String getFullName() {
        return currentAccount != null ? currentAccount.getFullName() : "Khách";
    }

    public String getRoleName() {
        return currentAccount != null ? currentAccount.getRoleName() : "N/A";
    }
}

