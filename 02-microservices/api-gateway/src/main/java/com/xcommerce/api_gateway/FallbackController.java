package com.xcommerce.api_gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping(
        path = {
            "/fallback/auth",
            "/fallback/catalog",
            "/fallback/cart",
            "/fallback/order",
            "/fallback/inventory",
            "/fallback/users",
            "/fallback/payments",
            "/fallback/notifications"
        },
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> fallback(org.springframework.web.server.ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String service = path.substring(path.lastIndexOf('/') + 1);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", 503,
            "error", "Service Unavailable",
            "service", service,
            "message", "Servico temporariamente indisponivel. Tenta novamente em instantes."
        ));
    }
}
