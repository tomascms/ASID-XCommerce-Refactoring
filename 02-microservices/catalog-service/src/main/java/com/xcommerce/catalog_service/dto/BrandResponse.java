package com.xcommerce.catalog_service.dto;

public class BrandResponse {
    private Long id;
    private String name;
    private Boolean active;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public static BrandResponse from(com.xcommerce.catalog_service.model.Brand brand) {
        BrandResponse dto = new BrandResponse();
        dto.setId(brand.getId());
        dto.setName(brand.getName());
        dto.setActive(brand.getActive());
        return dto;
    }
}
