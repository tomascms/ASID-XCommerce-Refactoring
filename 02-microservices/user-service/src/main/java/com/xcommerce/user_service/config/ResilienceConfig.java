package com.xcommerce.user_service.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meterRegistry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .minimumNumberOfCalls(4)
            .slidingWindowSize(8)
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .permittedNumberOfCallsInHalfOpenState(2)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .slowCallRateThreshold(75)
            .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meterRegistry);
        return registry;
    }

    @Bean
    RetryRegistry retryRegistry(MeterRegistry meterRegistry) {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .failAfterMaxAttempts(true)
            .retryExceptions(Exception.class)
            .build();

        RetryRegistry registry = RetryRegistry.of(config);
        TaggedRetryMetrics.ofRetryRegistry(registry).bindTo(meterRegistry);
        return registry;
    }

    @Bean
    BulkheadRegistry bulkheadRegistry(MeterRegistry meterRegistry) {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(10)
            .maxWaitDuration(Duration.ofMillis(250))
            .build();

        BulkheadRegistry registry = BulkheadRegistry.of(config);
        TaggedBulkheadMetrics.ofBulkheadRegistry(registry).bindTo(meterRegistry);
        return registry;
    }
}
