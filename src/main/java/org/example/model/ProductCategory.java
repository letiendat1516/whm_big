package org.example.model;

public class ProductCategory {
    private String categoryId;
    private String categoryName;
    private String description;
    private int status; // 1=active
    private String createdAt;

    public ProductCategory() {}
    public ProductCategory(String categoryId, String categoryName) {
        this.categoryId = categoryId; this.categoryName = categoryName;
    }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String v) { this.categoryId = v; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String v) { this.categoryName = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public int getStatus() { return status; }
    public void setStatus(int v) { this.status = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
    @Override public String toString() { return categoryName; }
}

