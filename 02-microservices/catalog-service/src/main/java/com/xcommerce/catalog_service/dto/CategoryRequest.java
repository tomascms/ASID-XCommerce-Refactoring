package com.xcommerce.catalog_service.dto;

public class CategoryRequest {

    private String name;
    private Long parentCategoryId;
    private Boolean active;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getParentCategoryId() { return parentCategoryId; }
    public void setParentCategoryId(Long parentCategoryId) { this.parentCategoryId = parentCategoryId; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
