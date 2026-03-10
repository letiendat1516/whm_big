package org.example.model;

public class Store {
    private String storeId;
    private String storeCode;
    private String name;
    private String address;
    private String status;

    public Store() {}
    public Store(String storeId, String name) { this.storeId = storeId; this.name = name; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String v) { this.storeId = v; }
    public String getStoreCode() { return storeCode; }
    public void setStoreCode(String v) { this.storeCode = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    @Override public String toString() { return "[" + storeCode + "] " + name; }
}

