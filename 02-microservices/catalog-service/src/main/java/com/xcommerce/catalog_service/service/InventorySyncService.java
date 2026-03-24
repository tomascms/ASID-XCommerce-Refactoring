package com.xcommerce.catalog_service.service;

import com.xcommerce.catalog_service.model.Product;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class InventorySyncService {

    private static final String RESILIENCE_NAME = "inventory-sync";

    private final RestClient restClient;
    private final BulkheadRegistry bulkheadRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public InventorySyncService(BulkheadRegistry bulkheadRegistry, CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        this.bulkheadRegistry = bulkheadRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.restClient = RestClient.builder()
            .baseUrl("http://inventory-service:8085")
            .build();
    }

    public void syncProduct(Product product) {
        int quantity = product.getQuantity() != null ? product.getQuantity() : 0;
        executeRunnable(() -> restClient.post()
            .uri("/inventory/sync?productId={productId}&quantity={quantity}", product.getId(), quantity)
            .retrieve()
            .toBodilessEntity(), product.getId()
        );
    }

    private void executeRunnable(Runnable action, Long productId) {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(RESILIENCE_NAME);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE_NAME);
        Retry retry = retryRegistry.retry(RESILIENCE_NAME);
        Runnable decorated = Retry.decorateRunnable(retry, CircuitBreaker.decorateRunnable(circuitBreaker, Bulkhead.decorateRunnable(bulkhead, action)));

        try {
            decorated.run();
        } catch (Exception exception) {
            throw new IllegalStateException("Nao foi possivel sincronizar o inventario para o produto " + productId, exception);
        }
    }
}
