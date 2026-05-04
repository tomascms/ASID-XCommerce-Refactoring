package com.xcommerce.catalog_service.controller;

import com.xcommerce.catalog_service.dto.ProductRequest;
import com.xcommerce.catalog_service.model.Brand;
import com.xcommerce.catalog_service.model.Category;
import com.xcommerce.catalog_service.model.Product;
import com.xcommerce.catalog_service.model.Review;
import com.xcommerce.catalog_service.dto.ProductResponse;
import com.xcommerce.catalog_service.dto.ReviewResponse;
import com.xcommerce.catalog_service.repository.BrandRepository;
import com.xcommerce.catalog_service.repository.CategoryRepository;
import com.xcommerce.catalog_service.repository.ProductRepository;
import com.xcommerce.catalog_service.repository.ReviewRepository;
import com.xcommerce.catalog_service.service.CatalogProducer;
import com.xcommerce.catalog_service.service.InventorySyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.List;
import java.util.Optional;
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
    public List<ProductResponse> getAll() {
        return repository.findAll().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/search")
    public List<ProductResponse> search(
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
        return stream.map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable long id) {
        return repository.findById(id)
            .map(product -> ResponseEntity.ok(ProductResponse.from(product)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ProductResponse> create(@RequestBody ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        Product savedProduct = repository.save(product);
        try {
            inventorySyncService.syncProduct(savedProduct);
        } catch (Exception e) {
            // Inventory sync failures should not block product creation
        }
        catalogProducer.sendProductCreatedEvent(savedProduct);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(savedProduct));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ProductResponse> update(@PathVariable long id, @RequestBody ProductRequest productDetails) {
        Optional<Product> maybeProduct = repository.findById(id);
        if (maybeProduct.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = maybeProduct.get();
        applyRequest(product, productDetails);
        Product updatedProduct = Objects.requireNonNull(repository.save(Objects.requireNonNull(product)));
        try {
            inventorySyncService.syncProduct(updatedProduct);
        } catch (Exception e) {
            // Inventory sync failures should not block product updates
        }
        catalogProducer.sendProductCreatedEvent(updatedProduct);
        return ResponseEntity.ok(ProductResponse.from(updatedProduct));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable long id) {
        Optional<Product> maybeProduct = repository.findById(id);
        if (maybeProduct.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product productToDelete = maybeProduct.get();
        repository.delete(Objects.requireNonNull(productToDelete));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/price")
    public BigDecimal getProductPrice(@PathVariable long id) {
        return repository.findById(id)
            .map(Product::getPrice)
            .orElseThrow(() -> new RuntimeException("Produto nao encontrado"));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        List<ReviewResponse> reviews = reviewRepository.findByProductId(id).stream().map(ReviewResponse::from).toList();
        return ResponseEntity.ok(reviews);
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewResponse> createReview(@PathVariable long id, @RequestBody Review review) {
        Optional<Product> maybeProduct = repository.findById(id);
        if (maybeProduct.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = maybeProduct.get();
        review.setId(null);
        review.setProduct(Objects.requireNonNull(product));
        Review saved = reviewRepository.save(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReviewResponse.from(saved));
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setSku(request.getSku());
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
