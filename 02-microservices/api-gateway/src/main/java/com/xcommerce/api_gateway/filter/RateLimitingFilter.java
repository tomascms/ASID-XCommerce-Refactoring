package com.xcommerce.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private static final int MAX_REQUESTS_PER_WINDOW = 120;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ConcurrentHashMap<String, WindowState> counters = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath().toLowerCase();
        if (path.startsWith("/actuator") || path.startsWith("/docs/") || path.contains("/swagger-ui") || path.contains("/v3/api-docs")) {
            return chain.filter(exchange);
        }

        String clientIp = extractClientIp(exchange);
        WindowState state = counters.compute(clientIp, (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new WindowState(Instant.now(), new AtomicInteger(1));
            }

            existing.counter.incrementAndGet();
            return existing;
        });

        if (state.counter.get() > MAX_REQUESTS_PER_WINDOW) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String extractClientIp(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress == null ? "unknown" : remoteAddress.getAddress().getHostAddress();
    }

    private static final class WindowState {
        private final Instant startedAt;
        private final AtomicInteger counter;

        private WindowState(Instant startedAt, AtomicInteger counter) {
            this.startedAt = startedAt;
            this.counter = counter;
        }

        private boolean isExpired() {
            return Instant.now().isAfter(startedAt.plus(WINDOW));
        }
    }
}