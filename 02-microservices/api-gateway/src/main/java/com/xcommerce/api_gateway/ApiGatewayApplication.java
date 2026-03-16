package com.xcommerce.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public CommandLineRunner printRoutes(RouteLocator routeLocator) {
        return args -> {
            System.out.println("Loaded gateway routes:");
            routeLocator.getRoutes().doOnNext(route -> System.out.println("- " + route.getId() + " -> " + route.getUri())).subscribe();
        };
    }
}