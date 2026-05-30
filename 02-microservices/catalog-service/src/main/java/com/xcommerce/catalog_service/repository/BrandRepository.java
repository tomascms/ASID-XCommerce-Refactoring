package com.xcommerce.catalog_service.repository;

import com.xcommerce.catalog_service.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    Brand findByNameIgnoreCase(String name);
}
