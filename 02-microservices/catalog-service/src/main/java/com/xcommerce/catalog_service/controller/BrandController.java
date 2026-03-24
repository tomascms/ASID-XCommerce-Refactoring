package com.xcommerce.catalog_service.controller;

import com.xcommerce.catalog_service.model.Brand;
import com.xcommerce.catalog_service.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/brands")
public class BrandController {

    @Autowired
    private BrandRepository repository;

    @GetMapping
    public List<Brand> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<Brand> create(@RequestBody Brand brand) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(brand));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Brand> update(@PathVariable Long id, @RequestBody Brand data) {
        return repository.findById(id).map(brand -> {
            brand.setName(data.getName());
            if (data.getActive() != null) {
                brand.setActive(data.getActive());
            }
            return ResponseEntity.ok(repository.save(brand));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Brand> deactivate(@PathVariable Long id) {
        return repository.findById(id).map(brand -> {
            brand.setActive(false);
            return ResponseEntity.ok(repository.save(brand));
        }).orElse(ResponseEntity.notFound().build());
    }
}
