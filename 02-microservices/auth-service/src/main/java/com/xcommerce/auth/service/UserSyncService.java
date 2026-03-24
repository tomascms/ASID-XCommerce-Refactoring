package com.xcommerce.auth.service;

import com.xcommerce.auth.dto.UserSyncRequest;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserSyncService {

    private static final Logger log = LoggerFactory.getLogger(UserSyncService.class);
    private static final String RESILIENCE_NAME = "user-sync";

    private final RestClient restClient = RestClient.builder()
        .baseUrl("http://user-service:8086/users/internal")
        .build();
    private final BulkheadRegistry bulkheadRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public UserSyncService(BulkheadRegistry bulkheadRegistry, CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        this.bulkheadRegistry = bulkheadRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    public void sync(UserSyncRequest request) {
        executeRunnable(() -> restClient.post()
            .uri("/sync")
            .body(request)
            .retrieve()
            .toBodilessEntity(), throwable -> log.warn(
                "User service indisponivel; sync adiado para o utilizador {}: {}",
                request.username(),
                throwable.getMessage()
            )
        );
    }

    private void executeRunnable(Runnable action, java.util.function.Consumer<Throwable> fallback) {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(RESILIENCE_NAME);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE_NAME);
        Retry retry = retryRegistry.retry(RESILIENCE_NAME);
        Runnable decorated = Retry.decorateRunnable(retry, CircuitBreaker.decorateRunnable(circuitBreaker, Bulkhead.decorateRunnable(bulkhead, action)));

        try {
            decorated.run();
        } catch (Exception exception) {
            fallback.accept(exception);
        }
    }
}
