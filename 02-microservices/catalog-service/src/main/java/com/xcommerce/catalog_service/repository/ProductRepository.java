package com.xcommerce.catalog_service.repository;

import com.xcommerce.catalog_service.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByCategoryEntityId(Long categoryId);
    List<Product> findByCategoryEntityIdAndPriceBetween(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice);
    List<Product> findByBrandEntityId(Long brandId);
    List<Product> findByBrandEntityIdAndPriceBetween(Long brandId, BigDecimal minPrice, BigDecimal maxPrice);
    List<Product> findByCategoryEntityIdAndBrandEntityId(Long categoryId, Long brandId);
    List<Product> findByCategoryEntityIdAndBrandEntityIdAndPriceBetween(Long categoryId, Long brandId, BigDecimal minPrice, BigDecimal maxPrice);
}
