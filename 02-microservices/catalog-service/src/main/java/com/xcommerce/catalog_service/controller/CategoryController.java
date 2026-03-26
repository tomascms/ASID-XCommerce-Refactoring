package com.xcommerce.catalog_service.controller;

import com.xcommerce.catalog_service.dto.CategoryRequest;
import com.xcommerce.catalog_service.model.Category;
import com.xcommerce.catalog_service.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository repository;

    @GetMapping
    public List<com.xcommerce.catalog_service.dto.CategoryResponse> getAll() {
        return repository.findAll().stream().map(com.xcommerce.catalog_service.dto.CategoryResponse::from).toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<com.xcommerce.catalog_service.dto.CategoryResponse> create(@RequestBody CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setParentCategory(resolveParent(request.getParentCategoryId()));
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }
        Category saved = repository.save(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(com.xcommerce.catalog_service.dto.CategoryResponse.from(saved));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<com.xcommerce.catalog_service.dto.CategoryResponse> update(@PathVariable Long id, @RequestBody CategoryRequest data) {
        return repository.findById(id).map(category -> {
            category.setName(data.getName());
            category.setParentCategory(resolveParent(data.getParentCategoryId()));
            if (data.getActive() != null) {
                category.setActive(data.getActive());
            }
            Category updated = repository.save(category);
            return ResponseEntity.ok(com.xcommerce.catalog_service.dto.CategoryResponse.from(updated));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/deactivate")
    @Transactional
    public ResponseEntity<com.xcommerce.catalog_service.dto.CategoryResponse> deactivate(@PathVariable Long id) {
        return repository.findById(id).map(category -> {
            category.setActive(false);
            Category updated = repository.save(category);
            return ResponseEntity.ok(com.xcommerce.catalog_service.dto.CategoryResponse.from(updated));
        }).orElse(ResponseEntity.notFound().build());
    }

    private Category resolveParent(Long parentCategoryId) {
        if (parentCategoryId == null) {
            return null;
        }
        return repository.findById(parentCategoryId)
            .orElseThrow(() -> new IllegalArgumentException("Categoria pai nao encontrada"));
    }
}
