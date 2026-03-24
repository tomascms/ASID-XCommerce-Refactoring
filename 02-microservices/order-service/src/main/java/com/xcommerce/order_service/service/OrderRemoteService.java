package com.xcommerce.order_service.service;

import com.xcommerce.order_service.client.CatalogClient;
import com.xcommerce.order_service.client.CartClient;
import com.xcommerce.order_service.client.InventoryClient;
import com.xcommerce.order_service.dto.CartItemDTO;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class OrderRemoteService {

    private static final Logger log = LoggerFactory.getLogger(OrderRemoteService.class);

    private final CartClient cartClient;
    private final InventoryClient inventoryClient;
    private final CatalogClient catalogClient;
    private final BulkheadRegistry bulkheadRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public OrderRemoteService(
        CartClient cartClient,
        InventoryClient inventoryClient,
        CatalogClient catalogClient,
        BulkheadRegistry bulkheadRegistry,
        CircuitBreakerRegistry circuitBreakerRegistry,
        RetryRegistry retryRegistry
    ) {
        this.cartClient = cartClient;
        this.inventoryClient = inventoryClient;
        this.catalogClient = catalogClient;
        this.bulkheadRegistry = bulkheadRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    public List<CartItemDTO> getCartItems(String username) {
        return executeSupplier(
            "cart-fetch",
            () -> cartClient.getCartItems(username),
            throwable -> {
                throw new IllegalStateException("Carrinho indisponivel para checkout.", throwable);
            }
        );
    }

    public void clearCart(String username) {
        executeRunnable(
            "cart-clear",
            () -> cartClient.clearCart(username),
            throwable -> log.warn("Nao foi possivel limpar o carrinho de {} apos a encomenda: {}", username, throwable.getMessage())
        );
    }

    public boolean checkStock(Long productId, Integer quantity) {
        return executeSupplier(
            "inventory-check",
            () -> inventoryClient.checkStock(productId, quantity),
            throwable -> {
                log.warn("Fallback de stock para produto {}: {}", productId, throwable.getMessage());
                return false;
            }
        );
    }

    public void decreaseStock(Long productId, Integer quantity) {
        executeRunnable(
            "inventory-decrease",
            () -> inventoryClient.decreaseStock(productId, quantity),
            throwable -> {
                throw new IllegalStateException("Nao foi possivel reservar stock para o produto " + productId, throwable);
            }
        );
    }

    public BigDecimal getProductPrice(Long productId) {
        return executeSupplier(
            "catalog-price",
            () -> catalogClient.getProductPrice(productId),
            throwable -> {
                throw new IllegalStateException("Nao foi possivel obter o preco do produto " + productId, throwable);
            }
        );
    }

    private <T> T executeSupplier(String name, Supplier<T> action, Function<Throwable, T> fallback) {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(name);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
        Retry retry = retryRegistry.retry(name);
        Supplier<T> decorated = Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, Bulkhead.decorateSupplier(bulkhead, action)));

        try {
            return decorated.get();
        } catch (Exception exception) {
            return fallback.apply(exception);
        }
    }

    private void executeRunnable(String name, Runnable action, Consumer<Throwable> fallback) {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(name);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
        Retry retry = retryRegistry.retry(name);
        Runnable decorated = Retry.decorateRunnable(retry, CircuitBreaker.decorateRunnable(circuitBreaker, Bulkhead.decorateRunnable(bulkhead, action)));

        try {
            decorated.run();
        } catch (Exception exception) {
            fallback.accept(exception);
        }
    }
}
