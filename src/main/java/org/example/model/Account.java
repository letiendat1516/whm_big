package org.example.model;

import java.util.List;

/** Tài khoản đăng nhập hệ thống */
public class Account {
    private String accountId;
    private String username;
    private String passwordHash;
    private String roleId;
    private String roleName;
    private String employeeId;
    private String storeId;
    private String fullName;
    private boolean isActive;
    private String lastLoginAt;
    private List<String> permissions; // ["POS","PAYMENT","INVENTORY",...]

    public Account() {}

    // Getters & Setters
    public String getAccountId()    { return accountId; }
    public void setAccountId(String v) { this.accountId = v; }

    public String getUsername()     { return username; }
    public void setUsername(String v) { this.username = v; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }

    public String getRoleId()       { return roleId; }
    public void setRoleId(String v) { this.roleId = v; }

    public String getRoleName()     { return roleName; }
    public void setRoleName(String v) { this.roleName = v; }

    public String getEmployeeId()   { return employeeId; }
    public void setEmployeeId(String v) { this.employeeId = v; }

    public String getStoreId()      { return storeId; }
    public void setStoreId(String v) { this.storeId = v; }

    public String getFullName()     { return fullName; }
    public void setFullName(String v) { this.fullName = v; }

    public boolean isActive()       { return isActive; }
    public void setActive(boolean v) { this.isActive = v; }

    public String getLastLoginAt()  { return lastLoginAt; }
    public void setLastLoginAt(String v) { this.lastLoginAt = v; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> v) { this.permissions = v; }

    /** Kiểm tra quyền truy cập module */
    public boolean hasPermission(String module) {
        if (permissions == null) return false;
        return permissions.contains(module);
    }

    /** ADMIN có toàn quyền */
    public boolean isAdmin() {
        return "ADMIN".equals(roleName);
    }

    @Override
    public String toString() {
        return fullName + " [" + roleName + "]";
    }
}

