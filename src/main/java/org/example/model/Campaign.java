package org.example.model;

public class Campaign {
    private String campaignId;
    private String name;
    private String description;
    private String startDate;
    private String endDate;
    private int isActive;
    private String storeId;

    public Campaign() { this.isActive = 1; }

    public String getCampaignId() { return campaignId; }
    public void setCampaignId(String v) { this.campaignId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String v) { this.startDate = v; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String v) { this.endDate = v; }
    public int getIsActive() { return isActive; }
    public void setIsActive(int v) { this.isActive = v; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String v) { this.storeId = v; }
    @Override public String toString() { return name; }
}

