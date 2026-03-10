package org.example.model;

public class Promotion {
    private String promotionId;
    private String campaignId;
    private String promoType; // PERCENT_DISCOUNT/FIXED_DISCOUNT/BUY_X_GET_Y/VOUCHER/POINTS_MULTIPLIER
    private String name;
    private int priority;
    private String ruleDefinition;
    private String voucherCode;
    private int maxUsageCount;
    private int currentUsageCount;
    private String expiryDate;
    private String triggerCondition;
    private int isActive;
    // transient
    private String campaignName;

    public Promotion() {}

    public String getPromotionId() { return promotionId; }
    public void setPromotionId(String v) { this.promotionId = v; }
    public String getCampaignId() { return campaignId; }
    public void setCampaignId(String v) { this.campaignId = v; }
    public String getPromoType() { return promoType; }
    public void setPromoType(String v) { this.promoType = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public int getPriority() { return priority; }
    public void setPriority(int v) { this.priority = v; }
    public String getRuleDefinition() { return ruleDefinition; }
    public void setRuleDefinition(String v) { this.ruleDefinition = v; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String v) { this.voucherCode = v; }
    public int getMaxUsageCount() { return maxUsageCount; }
    public void setMaxUsageCount(int v) { this.maxUsageCount = v; }
    public int getCurrentUsageCount() { return currentUsageCount; }
    public void setCurrentUsageCount(int v) { this.currentUsageCount = v; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String v) { this.expiryDate = v; }
    public String getTriggerCondition() { return triggerCondition; }
    public void setTriggerCondition(String v) { this.triggerCondition = v; }
    public int getIsActive() { return isActive; }
    public void setIsActive(int v) { this.isActive = v; }
    public String getCampaignName() { return campaignName; }
    public void setCampaignName(String v) { this.campaignName = v; }
    @Override public String toString() { return name; }
}

