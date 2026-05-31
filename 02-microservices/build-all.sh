#!/bin/zsh

set -e

#sdk use java 21.0.11-tem

SERVICES=(
    "api-gateway"
    "cart-service"
    "catalog-service"
    "identity-service"
    "inventory-service"
    "notification-service"
    "order-service"
)

for service in "${SERVICES[@]}"; do
    clear
    echo "Building $service..."
    cd "$service"
    mvn clean package -DskipTests
    cd ..
done

echo "All services built successfully!"