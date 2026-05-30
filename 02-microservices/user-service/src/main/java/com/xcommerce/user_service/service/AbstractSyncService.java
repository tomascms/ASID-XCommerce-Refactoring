package com.xcommerce.user_service.service;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;

/**
 * Base class for synchronization services across microservices.
 * Reduces code duplication by centralizing Resilience4j decoration logic.
 */
public abstract class AbstractSyncService {
    
    protected final BulkheadRegistry bulkheadRegistry;
    protected final CircuitBreakerRegistry circuitBreakerRegistry;
    protected final RetryRegistry retryRegistry;
    protected final String resilienceName;

    public AbstractSyncService(
            String resilienceName,
            BulkheadRegistry bulkheadRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this.resilienceName = resilienceName;
        this.bulkheadRegistry = bulkheadRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    /**
     * Execute an action with resilience decorators (Circuit Breaker, Retry, Bulkhead)
     */
    protected void executeRunnable(Runnable action, java.util.function.Consumer<Throwable> fallback) {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(resilienceName);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(resilienceName);
        Retry retry = retryRegistry.retry(resilienceName);
        
        Runnable decorated = Retry.decorateRunnable(retry,
            CircuitBreaker.decorateRunnable(circuitBreaker,
                Bulkhead.decorateRunnable(bulkhead, action)));

        try {
            decorated.run();
        } catch (Exception exception) {
            fallback.accept(exception);
        }
    }

    /**
     * Get logger for subclass - subclasses should override this
     */
    protected abstract Logger getLogger();
}
