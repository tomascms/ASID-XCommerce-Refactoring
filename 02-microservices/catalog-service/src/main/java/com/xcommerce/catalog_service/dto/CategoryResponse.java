package com.xcommerce.catalog_service.dto;

public class CategoryResponse {
    private Long id;
    private String name;
    private Long parentCategoryId;
    private Boolean active;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getParentCategoryId() { return parentCategoryId; }
    public void setParentCategoryId(Long parentCategoryId) { this.parentCategoryId = parentCategoryId; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public static CategoryResponse from(com.xcommerce.catalog_service.model.Category category) {
        CategoryResponse dto = new CategoryResponse();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setActive(category.getActive());
        dto.setParentCategoryId(category.getParentCategory() != null ? category.getParentCategory().getId() : null);
        return dto;
    }
}
