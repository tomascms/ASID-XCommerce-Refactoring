package com.xcommerce.catalog_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String barcode;

    @Column(length = 1000)
    private String description;

    private String image;

    @Column(nullable = false)
    private BigDecimal price;

    private BigDecimal discount;
    private Integer quantity;
    private BigDecimal weight;
    private Boolean active = Boolean.TRUE;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "brand_id")
    @JsonIgnore
    private Brand brandEntity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    @JsonIgnore
    private Category categoryEntity;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Review> reviews = new ArrayList<>();

    public Product() {}

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
    @Transient
    public String getCategory() { return categoryEntity != null ? categoryEntity.getName() : null; }
    public void setCategory(String category) {}
    @Transient
    public String getBrand() { return brandEntity != null ? brandEntity.getName() : null; }
    public void setBrand(String brand) {}
    @Transient
    public Long getBrandId() { return brandEntity != null ? brandEntity.getId() : null; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }
    public Brand getBrandEntity() { return brandEntity; }
    public void setBrandEntity(Brand brandEntity) { this.brandEntity = brandEntity; }
    @Transient
    public Long getCategoryId() { return categoryEntity != null ? categoryEntity.getId() : null; }
    public Category getCategoryEntity() { return categoryEntity; }
    public void setCategoryEntity(Category categoryEntity) { this.categoryEntity = categoryEntity; }
}
