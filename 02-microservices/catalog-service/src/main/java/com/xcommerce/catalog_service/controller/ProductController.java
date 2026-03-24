package com.xcommerce.catalog_service.controller;

import com.xcommerce.catalog_service.dto.ProductRequest;
import com.xcommerce.catalog_service.model.Brand;
import com.xcommerce.catalog_service.model.Category;
import com.xcommerce.catalog_service.model.Product;
import com.xcommerce.catalog_service.model.Review;
import com.xcommerce.catalog_service.repository.BrandRepository;
import com.xcommerce.catalog_service.repository.CategoryRepository;
import com.xcommerce.catalog_service.repository.ProductRepository;
import com.xcommerce.catalog_service.repository.ReviewRepository;
import com.xcommerce.catalog_service.service.CatalogProducer;
import com.xcommerce.catalog_service.service.InventorySyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CatalogProducer catalogProducer;

    @Autowired
    private InventorySyncService inventorySyncService;

    @GetMapping
    public List<Product> getAll() {
        return repository.findAll();
    }

    @GetMapping("/search")
    public List<Product> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String brand,
        @RequestParam(required = false) Long brandId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice
    ) {
        Stream<Product> stream = repository.findAll().stream();

        if (name != null && !name.isBlank()) {
            String normalized = name.toLowerCase();
            stream = stream.filter(product -> product.getName() != null && product.getName().toLowerCase().contains(normalized));
        }
        if (category != null && !category.isBlank()) {
            stream = stream.filter(product -> product.getCategory() != null && product.getCategory().equalsIgnoreCase(category));
        }
        if (categoryId != null) {
            stream = stream.filter(product -> product.getCategoryEntity() != null && categoryId.equals(product.getCategoryEntity().getId()));
        }
        if (brand != null && !brand.isBlank()) {
            stream = stream.filter(product -> product.getBrand() != null && product.getBrand().equalsIgnoreCase(brand));
        }
        if (brandId != null) {
            stream = stream.filter(product -> product.getBrandEntity() != null && brandId.equals(product.getBrandEntity().getId()));
        }
        if (minPrice != null) {
            stream = stream.filter(product -> product.getPrice() != null && product.getPrice().compareTo(minPrice) >= 0);
        }
        if (maxPrice != null) {
            stream = stream.filter(product -> product.getPrice() != null && product.getPrice().compareTo(maxPrice) <= 0);
        }

        return stream.toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return repository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        Product savedProduct = repository.save(product);
        inventorySyncService.syncProduct(savedProduct);
        catalogProducer.sendProductCreatedEvent(savedProduct);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody ProductRequest productDetails) {
        return repository.findById(id).map(product -> {
            applyRequest(product, productDetails);

            Product updatedProduct = repository.save(product);
            inventorySyncService.syncProduct(updatedProduct);
            catalogProducer.sendProductCreatedEvent(updatedProduct);
            return ResponseEntity.ok(updatedProduct);
        }).orElse(ResponseEntity.notFound().build());
    }

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
            .orElseThrow(() -> new RuntimeException("Produto nao encontrado"));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<Review>> getReviews(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reviewRepository.findByProductId(id));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<Review> createReview(@PathVariable Long id, @RequestBody Review review) {
        return repository.findById(id).map(product -> {
            review.setId(null);
            review.setProduct(product);
            return ResponseEntity.status(HttpStatus.CREATED).body(reviewRepository.save(review));
        }).orElse(ResponseEntity.notFound().build());
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setBarcode(request.getBarcode());
        product.setDescription(request.getDescription());
        product.setImage(request.getImage());
        product.setPrice(request.getPrice());
        product.setDiscount(request.getDiscount());
        if (request.getQuantity() != null) {
            product.setQuantity(request.getQuantity());
        } else if (product.getQuantity() == null) {
            product.setQuantity(100);
        }
        product.setWeight(request.getWeight());
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        product.setBrandEntity(resolveBrand(request.getBrandId(), request.getBrand()));
        product.setCategoryEntity(resolveCategory(request.getCategoryId(), request.getCategory()));
    }

    private Brand resolveBrand(Long brandId, String brandName) {
        if (brandId != null) {
            return brandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Marca nao encontrada"));
        }
        if (brandName == null || brandName.isBlank()) {
            return null;
        }
        Brand existing = brandRepository.findByNameIgnoreCase(brandName);
        if (existing != null) {
            return existing;
        }
        Brand brand = new Brand();
        brand.setName(brandName);
        return brandRepository.save(brand);
    }

    private Category resolveCategory(Long categoryId, String categoryName) {
        if (categoryId != null) {
            return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria nao encontrada"));
        }
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        Category existing = categoryRepository.findByNameIgnoreCase(categoryName);
        if (existing != null) {
            return existing;
        }
        Category category = new Category();
        category.setName(categoryName);
        return categoryRepository.save(category);
    }
}
