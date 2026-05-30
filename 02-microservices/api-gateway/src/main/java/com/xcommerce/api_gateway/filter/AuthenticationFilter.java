package com.xcommerce.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AuthenticationFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath().toLowerCase();
        String method = exchange.getRequest().getMethod().name();

        if (isPublicPath(path, method)) {
            return chain.filter(exchange);
        }

        return exchange.getPrincipal()
            .flatMap(principal -> {
                if (!(principal instanceof AbstractAuthenticationToken authentication)) {
                    return onError(exchange, HttpStatus.UNAUTHORIZED);
                }

                if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
                    return onError(exchange, HttpStatus.UNAUTHORIZED);
                }

                String role = extractRole(jwtAuthenticationToken);

                if (isSuperAdminOnlyPath(path) && !"SUPERADMIN".equalsIgnoreCase(role)) {
                    return onError(exchange, HttpStatus.FORBIDDEN);
                }
                if (isAdminPath(path, method) && !isAdmin(role)) {
                    return onError(exchange, HttpStatus.FORBIDDEN);
                }

                ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                        .header("X-User-Name", jwtAuthenticationToken.getToken().getSubject())
                        .header("X-User-Role", role == null ? "" : role)
                        .build())
                    .build();

                return chain.filter(modifiedExchange);
            })
            .switchIfEmpty(chain.filter(exchange));
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    private boolean isPublicPath(String path, String method) {
        return path.equals("/rest/user/authenticate")
            || path.equals("/rest/user/login")
            || path.equals("/rest/user/register")
            || path.equals("/rest/user")
            || path.contains("/v3/api-docs")
            || path.contains("/swagger-ui")
            || path.startsWith("/docs/")
            || (path.startsWith("/categories") && method.equals("GET"))
            || (path.startsWith("/products") && method.equals("GET"));
    }

    private boolean isAdminPath(String path, String method) {
        return path.equals("/rest/user/admin")
            || path.equals("/rest/user/backoffice/list")
            || path.startsWith("/rest/order/backoffice")
            || (path.startsWith("/products") && !method.equals("GET"));
    }

    private boolean isSuperAdminOnlyPath(String path) {
        return path.equals("/rest/user/super-admin");
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role) || "SUPERADMIN".equalsIgnoreCase(role);
    }

    private String extractRole(JwtAuthenticationToken authentication) {
        Object directRole = authentication.getToken().getClaims().get("role");
        if (directRole instanceof String role && !role.isBlank()) {
            return role;
        }

        Object realmAccess = authentication.getToken().getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> realmAccessMap) {
            Object roles = realmAccessMap.get("roles");
            if (roles instanceof Collection<?> collection && !collection.isEmpty()) {
                return collection.stream().map(Object::toString).collect(Collectors.joining(","));
            }
        }

        return "";
    }
}
