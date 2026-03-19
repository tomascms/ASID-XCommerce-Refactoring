package com.xcommerce.catalog_service.controller;

import com.xcommerce.catalog_service.model.Product;
import com.xcommerce.catalog_service.repository.ProductRepository;
import com.xcommerce.catalog_service.service.CatalogProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private CatalogProducer catalogProducer;

    // GET: Listar todos
    @GetMapping
    public List<Product> getAll() {
        return repository.findAll();
    }

    // GET: Buscar por ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST: Criar e avisar o Kafka
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product) {
        Product savedProduct = repository.save(product);
        
        catalogProducer.sendProductCreatedEvent(savedProduct.getId().toString());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }

    // PUT: Atualizar
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product productDetails) {
        return repository.findById(id).map(product -> {
            product.setName(productDetails.getName());
            product.setDescription(productDetails.getDescription());
            product.setPrice(productDetails.getPrice());
            product.setCategory(productDetails.getCategory());
            
            Product updatedProduct = repository.save(product);
            
            catalogProducer.sendProductCreatedEvent(updatedProduct.getId().toString());
            
            return ResponseEntity.ok(updatedProduct);
        }).orElse(ResponseEntity.notFound().build());
    }

    // DELETE: Remover
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return repository.findById(id).map(product -> {
            repository.delete(product);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/price")
    public BigDecimal getProductPrice(@PathVariable Long id) {
        return repository.findById(id)
                .map(Product::getPrice)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
    }
}