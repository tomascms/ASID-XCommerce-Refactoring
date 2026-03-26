package com.xcommerce.user_service.service;

import com.xcommerce.user_service.dto.AuthUserSyncRequest;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@SuppressWarnings("unchecked")
public class AuthSyncService extends AbstractSyncService {

    private static final Logger log = LoggerFactory.getLogger(AuthSyncService.class);

    private final RestClient restClient = RestClient.builder()
        .baseUrl("http://auth-service:8081/auth/internal")
        .build();

    public AuthSyncService(BulkheadRegistry bulkheadRegistry, CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        super("auth-sync", bulkheadRegistry, circuitBreakerRegistry, retryRegistry);
    }

    public void sync(AuthUserSyncRequest request) {
        executeRunnable(() -> restClient.post()
            .uri("/sync")
            .body(request)
            .retrieve()
            .toBodilessEntity(), throwable -> log.warn(
                "Auth service indisponivel; sync adiado para o utilizador {}: {}",
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
