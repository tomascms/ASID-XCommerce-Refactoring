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

        // 1. LISTA BRANCA DEFINITIVA (Ignora Token para Auth e GET Products)
        if (path.contains("/auth") || 
            path.contains("/login") || 
            path.contains("/register") || 
            path.contains("/v3/api-docs") || 
            path.contains("/swagger-ui") || 
            (path.contains("/products") && method.equals("GET"))) {
            return chain.filter(exchange);
        }

        // 2. VERIFICAÇÃO DE HEADER
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        try {
            String token = authHeader.substring(7);
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT jwt = JWT.require(algorithm).withIssuer("auth-api").build().verify(token);
            
            // Injetar o username para que os outros serviços saibam quem é o user
            ServerWebExchange modifiedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                    .header("X-User-Name", jwt.getSubject())
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
}