package com.xcommerce.catalog_service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"reviews", "hibernateLazyInitializer"})
    private Product product;

    @Column(nullable = false, length = 2000)
    private String eval;

    private float grade;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getEval() { return eval; }
    public void setEval(String eval) { this.eval = eval; }
    public float getGrade() { return grade; }
    public void setGrade(float grade) { this.grade = grade; }
}
