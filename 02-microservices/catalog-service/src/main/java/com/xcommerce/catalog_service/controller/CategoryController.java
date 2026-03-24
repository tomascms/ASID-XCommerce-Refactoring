package com.xcommerce.catalog_service.controller;

import com.xcommerce.catalog_service.dto.CategoryRequest;
import com.xcommerce.catalog_service.model.Category;
import com.xcommerce.catalog_service.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository repository;

    @GetMapping
    public List<Category> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<Category> create(@RequestBody CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setParentCategory(resolveParent(request.getParentCategoryId()));
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> update(@PathVariable Long id, @RequestBody CategoryRequest data) {
        return repository.findById(id).map(category -> {
            category.setName(data.getName());
            category.setParentCategory(resolveParent(data.getParentCategoryId()));
            if (data.getActive() != null) {
                category.setActive(data.getActive());
            }
            return ResponseEntity.ok(repository.save(category));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Category> deactivate(@PathVariable Long id) {
        return repository.findById(id).map(category -> {
            category.setActive(false);
            return ResponseEntity.ok(repository.save(category));
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
