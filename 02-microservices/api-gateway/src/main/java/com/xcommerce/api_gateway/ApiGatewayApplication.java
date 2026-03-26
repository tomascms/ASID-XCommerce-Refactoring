package com.xcommerce.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class ApiGatewayApplication {
    private static final Logger log = LoggerFactory.getLogger(ApiGatewayApplication.class);
    
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public CommandLineRunner printRoutes(RouteLocator routeLocator) {
        return args -> {
            log.info("✅ API Gateway initialized with routes:");
            routeLocator.getRoutes().doOnNext(route -> log.info("  📍 {} -> {}", route.getId(), route.getUri())).subscribe();
        };
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("auth-service", r -> r.path("/auth/**")
                .filters(f -> f.circuitBreaker(c -> c.setName("auth-gateway-cb").setFallbackUri("forward:/fallback/auth")))
                .uri("http://auth-service:8081"))
            .route("catalog-service", r -> r.path("/products/**")
                .filters(f -> f.circuitBreaker(c -> c.setName("catalog-gateway-cb").setFallbackUri("forward:/fallback/catalog")))
                .uri("http://catalog-service:8082"))
            .route("brand-service", r -> r.path("/brands/**")
                .filters(f -> f.circuitBreaker(c -> c.setName("catalog-brand-gateway-cb").setFallbackUri("forward:/fallback/catalog")))
                .uri("http://catalog-service:8082"))
            .route("category-service", r -> r.path("/categories/**")
                .filters(f -> f.circuitBreaker(c -> c.setName("catalog-category-gateway-cb").setFallbackUri("forward:/fallback/catalog")))
                .uri("http://catalog-service:8082"))
            .route("cart-service", r -> r.path("/cart/**")
                .filters(f -> f.circuitBreaker(c -> c.setName("cart-gateway-cb").setFallbackUri("forward:/fallback/cart")))
                .uri("http://cart-service:8083"))
            .route("order-service", r -> r.path("/order/**")
                .filters(f -> f.circuitBreaker(c -> c.setName("order-gateway-cb").setFallbackUri("forward:/fallback/order")))
                .uri("http://order-service:8084"))
            .route("inventory-service", r -> r.path("/inventory/**")
                .filters(f -> f.circuitBreaker(c -> c.setName("inventory-gateway-cb").setFallbackUri("forward:/fallback/inventory")))
                .uri("http://inventory-service:8085"))
            .route("user-service", r -> r.path("/users/**")
                .filters(f -> f.circuitBreaker(c -> c.setName("user-gateway-cb").setFallbackUri("forward:/fallback/users")))
                .uri("http://user-service:8086"))
            .route("payment-service", r -> r.path("/payments/**")
                .filters(f -> f.circuitBreaker(c -> c.setName("payment-gateway-cb").setFallbackUri("forward:/fallback/payments")))
                .uri("http://payment-service:8087"))
            .route("notification-service", r -> r.path("/notifications/**")
                .filters(f -> f.circuitBreaker(c -> c.setName("notification-gateway-cb").setFallbackUri("forward:/fallback/notifications")))
                .uri("http://notification-service:8088"))
            .route("catalog-docs", r -> r.path("/docs/catalog")
                .filters(f -> f.setPath("/v3/api-docs"))
                .uri("http://catalog-service:8082"))
            .route("auth-docs", r -> r.path("/docs/auth")
                .filters(f -> f.setPath("/v3/api-docs"))
                .uri("http://auth-service:8081"))
            .route("cart-docs", r -> r.path("/docs/cart")
                .filters(f -> f.setPath("/v3/api-docs"))
                .uri("http://cart-service:8083"))
            .route("order-docs", r -> r.path("/docs/order")
                .filters(f -> f.setPath("/v3/api-docs"))
                .uri("http://order-service:8084"))
            .route("inventory-docs", r -> r.path("/docs/inventory")
                .filters(f -> f.setPath("/v3/api-docs"))
                .uri("http://inventory-service:8085"))
            .route("user-docs", r -> r.path("/docs/users")
                .filters(f -> f.setPath("/v3/api-docs"))
                .uri("http://user-service:8086"))
            .route("payment-docs", r -> r.path("/docs/payments")
                .filters(f -> f.setPath("/v3/api-docs"))
                .uri("http://payment-service:8087"))
            .route("notification-docs", r -> r.path("/docs/notifications")
                .filters(f -> f.setPath("/v3/api-docs"))
                .uri("http://notification-service:8088"))
            .build();
    }
}
