package com.xcommerce.api_gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter implements GlobalFilter {

    private final String secret = "my-secret-key";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath().toLowerCase();
        String method = exchange.getRequest().getMethod().name();

        if (isPublicPath(path, method)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        try {
            String token = authHeader.substring(7);
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT jwt = JWT.require(algorithm).withIssuer("auth-api").build().verify(token);
            String role = jwt.getClaim("role").asString();

            if (isSuperAdminOnlyPath(path) && !"SUPERADMIN".equalsIgnoreCase(role)) {
                return onError(exchange, HttpStatus.FORBIDDEN);
            }
            if (isAdminPath(path, method) && !isAdmin(role)) {
                return onError(exchange, HttpStatus.FORBIDDEN);
            }

            ServerWebExchange modifiedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                    .header("X-User-Name", jwt.getSubject())
                    .header("X-User-Role", role == null ? "" : role)
                    .build())
                .build();

            return chain.filter(modifiedExchange);
        } catch (Exception e) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    private boolean isPublicPath(String path, String method) {
        return path.equals("/auth/login")
            || path.equals("/auth/register")
            || path.equals("/auth/authenticate")
            || path.contains("/v3/api-docs")
            || path.contains("/swagger-ui")
            || path.startsWith("/docs/")
            || (path.startsWith("/brands") && method.equals("GET"))
            || (path.startsWith("/categories") && method.equals("GET"))
            || (path.startsWith("/products") && method.equals("GET"));
    }

    private boolean isAdminPath(String path, String method) {
        return path.equals("/users")
            || path.equals("/users/admin")
            || path.equals("/users/backoffice/list")
            || path.startsWith("/order/backoffice")
            || ((path.startsWith("/products") || path.startsWith("/brands") || path.startsWith("/categories")) && !method.equals("GET"));
    }

    private boolean isSuperAdminOnlyPath(String path) {
        return path.equals("/users/super-admin");
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role) || "SUPERADMIN".equalsIgnoreCase(role);
    }
}
