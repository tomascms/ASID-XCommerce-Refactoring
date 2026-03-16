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
        String path = exchange.getRequest().getURI().getPath();
        System.out.println("AuthenticationFilter path=" + path + " method=" + exchange.getRequest().getMethod());

        // 1. Exceções (Acesso livre)
        if (path.contains("/v3/api-docs") || path.contains("/swagger-ui") || path.contains("/webjars") || path.contains("/auth") || (path.contains("/products") && exchange.getRequest().getMethod().name().equals("GET"))) {
            System.out.println("AuthenticationFilter allow path=" + path);
            return chain.filter(exchange);
        }

        // 2. Pegar o Token
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        try {
            // 3. Validar e extrair o username
            String username = validateTokenAndGetUsername(token);

            // 4. Injetar o header para o Cart Service
            // No WebFlux usamos o mutate() no request desta forma:
            ServerWebExchange modifiedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                    .header("X-User-Name", username)
                    .build())
                .build();

            return chain.filter(modifiedExchange);

        } catch (Exception e) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    // MÉTODO QUE FALTAVA: Validação real do JWT
    private String validateTokenAndGetUsername(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        DecodedJWT jwt = JWT.require(algorithm)
                .withIssuer("auth-api") // Certifica-te que o Auth Service usa o mesmo Issuer
                .build()
                .verify(token);
        return jwt.getSubject();
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}