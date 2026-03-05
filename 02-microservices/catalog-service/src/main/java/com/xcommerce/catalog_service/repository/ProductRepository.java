package com.xcommerce.catalog_service.repository;

import com.xcommerce.catalog_service.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // Aqui o Spring já te dá todos os métodos de save e find por defeito
}