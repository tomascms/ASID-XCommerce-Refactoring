package com.xcommerce.catalog_service.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ProductCreatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long productId;
    private String productCode;
    private String productName;
    private String description;
    private Double price;
    private Integer stockQuantity;
    private String category;
    private LocalDateTime createdAt;
    
    public ProductCreatedEvent() {}
    
    public ProductCreatedEvent(Long productId, String productCode, String productName, 
                              String description, Double price, Integer stockQuantity, String category) {
        this.productId = productId;
        this.productCode = productCode;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.createdAt = LocalDateTime.now();
    }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return "ProductCreatedEvent{" +
                "productId=" + productId +
                ", productCode='" + productCode + '\'' +
                ", productName='" + productName + '\'' +
                ", price=" + price +
                ", category='" + category + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
