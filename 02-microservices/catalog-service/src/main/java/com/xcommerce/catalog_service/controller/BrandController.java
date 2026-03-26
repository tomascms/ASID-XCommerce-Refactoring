package com.xcommerce.catalog_service.controller;

import com.xcommerce.catalog_service.model.Brand;
import com.xcommerce.catalog_service.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/brands")
@SuppressWarnings("unchecked")
public class BrandController {

    @Autowired
    private BrandRepository repository;

    @GetMapping
    public List<com.xcommerce.catalog_service.dto.BrandResponse> getAll() {
        return repository.findAll().stream().map(com.xcommerce.catalog_service.dto.BrandResponse::from).toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<com.xcommerce.catalog_service.dto.BrandResponse> create(@RequestBody Brand brand) {
        Brand saved = repository.save(brand);
        return ResponseEntity.status(HttpStatus.CREATED).body(com.xcommerce.catalog_service.dto.BrandResponse.from(saved));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<com.xcommerce.catalog_service.dto.BrandResponse> update(@PathVariable Long id, @RequestBody Brand data) {
        return repository.findById(id).map(brand -> {
            brand.setName(data.getName());
            if (data.getActive() != null) {
                brand.setActive(data.getActive());
            }
            Brand updated = repository.save(brand);
            return ResponseEntity.ok(com.xcommerce.catalog_service.dto.BrandResponse.from(updated));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/deactivate")
    @Transactional
    public ResponseEntity<com.xcommerce.catalog_service.dto.BrandResponse> deactivate(@PathVariable Long id) {
        return repository.findById(id).map(brand -> {
            brand.setActive(false);
            Brand updated = repository.save(brand);
            return ResponseEntity.ok(com.xcommerce.catalog_service.dto.BrandResponse.from(updated));
        }).orElse(ResponseEntity.notFound().build());
    }
}
