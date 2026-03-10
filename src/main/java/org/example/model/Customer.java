package org.example.model;

public class Customer {
    private String customerId;
    private String phoneNum;
    private String fullName;
    private String email;
    private String registrationDate;
    // transient loyalty
    private int currentPoints;
    private String tierName;
    private String accountId;

    public Customer() {}

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String v) { this.customerId = v; }
    public String getPhoneNum() { return phoneNum; }
    public void setPhoneNum(String v) { this.phoneNum = v; }
    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(String v) { this.registrationDate = v; }
    public int getCurrentPoints() { return currentPoints; }
    public void setCurrentPoints(int v) { this.currentPoints = v; }
    public String getTierName() { return tierName; }
    public void setTierName(String v) { this.tierName = v; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String v) { this.accountId = v; }
    @Override public String toString() { return fullName + " (" + phoneNum + ")"; }
}

