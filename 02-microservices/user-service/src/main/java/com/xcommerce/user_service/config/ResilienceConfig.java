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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry(
            MeterRegistry meterRegistry,
            @Value("${resilience4j.circuitbreaker.instances.auth-sync.sliding-window-size:8}") int slidingWindowSize,
            @Value("${resilience4j.circuitbreaker.instances.auth-sync.failure-rate-threshold:50}") float failureRateThreshold,
            @Value("${resilience4j.circuitbreaker.instances.auth-sync.wait-duration-in-open-state:10s}") Duration waitDurationInOpenState,
            @Value("${resilience4j.circuitbreaker.instances.auth-sync.permitted-number-of-calls-in-half-open-state:2}") int permittedCallsInHalfOpenState) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRateThreshold)
            .minimumNumberOfCalls(4)
            .slidingWindowSize(slidingWindowSize)
            .waitDurationInOpenState(waitDurationInOpenState)
            .permittedNumberOfCallsInHalfOpenState(permittedCallsInHalfOpenState)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .slowCallRateThreshold(75)
            .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meterRegistry);
        return registry;
    }

    @Bean
    RetryRegistry retryRegistry(
            MeterRegistry meterRegistry,
            @Value("${resilience4j.retry.instances.auth-sync.max-attempts:3}") int maxAttempts) {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(maxAttempts)
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
