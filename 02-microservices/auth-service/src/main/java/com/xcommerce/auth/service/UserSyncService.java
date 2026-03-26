package com.xcommerce.auth.service;

import com.xcommerce.auth.dto.UserSyncRequest;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserSyncService extends AbstractSyncService {

    private static final Logger log = LoggerFactory.getLogger(UserSyncService.class);

    private final RestClient restClient = RestClient.builder()
        .baseUrl("http://user-service:8086/users/internal")
        .build();

    public UserSyncService(BulkheadRegistry bulkheadRegistry, CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        super("user-sync", bulkheadRegistry, circuitBreakerRegistry, retryRegistry);
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

    @Override
    protected Logger getLogger() {
        return log;
    }
}
