package com.xcommerce.catalog_service.dto;

import java.math.BigDecimal;

public class ProductResponse {
    private Long id;
    private String name;
    private String barcode;
    private String description;
    private String image;
    private BigDecimal price;
    private BigDecimal discount;
    private Integer quantity;
    private BigDecimal weight;
    private Boolean active;
    private String brand;
    private Long brandId;
    private String category;
    private Long categoryId;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public static ProductResponse from(com.xcommerce.catalog_service.model.Product product) {
        ProductResponse dto = new ProductResponse();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setBarcode(product.getBarcode());
        dto.setDescription(product.getDescription());
        dto.setImage(product.getImage());
        dto.setPrice(product.getPrice());
        dto.setDiscount(product.getDiscount());
        dto.setQuantity(product.getQuantity());
        dto.setWeight(product.getWeight());
        dto.setActive(product.getActive());
        if (product.getBrandEntity() != null) {
            dto.setBrand(product.getBrandEntity().getName());
            dto.setBrandId(product.getBrandEntity().getId());
        } else {
            dto.setBrand(product.getBrand());
        }
        if (product.getCategoryEntity() != null) {
            dto.setCategory(product.getCategoryEntity().getName());
            dto.setCategoryId(product.getCategoryEntity().getId());
        } else {
            dto.setCategory(product.getCategory());
        }
        return dto;
    }
}
