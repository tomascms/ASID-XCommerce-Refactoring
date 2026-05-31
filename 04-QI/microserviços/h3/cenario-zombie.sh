#!/usr/bin/env bash
# H3 — Cenário 1: Estado Zombie (microserviços)
# Objetivo: mostrar que sem Saga, a encomenda fica em HANDLING se inventory-service estiver parado
# Uso: ./cenario-zombie.sh
# PRÉ-REQUISITO: microserviços a correr + seed executado (04-QI/seed/seed-microservicos.sh)

set -euo pipefail

GATEWAY="http://localhost:9000"
DB_ORDERS_USER=postgres
DB_ORDERS_PASS=postgres

USER="user1"
PASS="password"
# IDs fixos gerados pelo seed-microservicos.sh (Produto Teste 1=101, Produto Teste 2=102)
PRODUCT_IDS=(101 102)

OUT_DIR="$(cd "$(dirname "$0")" && pwd)/resultados"
mkdir -p "$OUT_DIR"

echo "=== H3 Cenário Zombie — $(date) ===" | tee "$OUT_DIR/zombie-log.txt"

# 1. Verificar que inventory-service está a correr
echo ""
echo "1. Estado inicial do inventory-service:"
docker ps --filter name=inventory-service --format "{{.Names}} — {{.Status}}" | tee -a "$OUT_DIR/zombie-log.txt"

# 2. Login
echo ""
echo "2. Login como $USER..."
LOGIN_RESP=$(curl -sf -X POST "$GATEWAY/rest/user/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}")
TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null || \
        echo "$LOGIN_RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "Token obtido: ${TOKEN:0:30}..." | tee -a "$OUT_DIR/zombie-log.txt"

# 3. Repor carrinho com 2 produtos
echo ""
echo "3. A repor carrinho..."
curl -sf -X DELETE "$GATEWAY/rest/shoppingCart" \
  -H "Authorization: Bearer $TOKEN" > /dev/null

for pid in "${PRODUCT_IDS[@]}"; do
  curl -sf -X POST "$GATEWAY/rest/shoppingCart" \
    -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"productId\":$pid,\"quantity\":2}" > /dev/null
done
echo "Carrinho reposto com produtos: ${PRODUCT_IDS[*]}" | tee -a "$OUT_DIR/zombie-log.txt"

# 4. Capturar estado do inventário ANTES (via docker exec)
echo ""
echo "4. Stock ANTES do checkout:"
docker exec -e PGPASSWORD="$DB_ORDERS_PASS" xcommerce-db-inventory \
  psql -U "$DB_ORDERS_USER" -d xcommerce_inventory \
  -c "SELECT product_id, quantity FROM inventory WHERE product_id IN (${PRODUCT_IDS[*]// /,});" \
  2>/dev/null | tee "$OUT_DIR/zombie-inventory-antes.txt" || \
  echo "(não foi possível ligar à BD de inventário)" | tee -a "$OUT_DIR/zombie-log.txt"

# 5. PARAR inventory-service
echo ""
echo "5. A parar inventory-service..."
docker stop inventory-service 2>/dev/null || docker stop xcommerce-inventory-service 2>/dev/null
echo "inventory-service PARADO" | tee -a "$OUT_DIR/zombie-log.txt"
sleep 2

# 6. Fazer checkout
echo ""
echo "6. Checkout com inventory-service parado..."
CHECKOUT_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST "$GATEWAY/rest/order/checkout" \
  -H "Authorization: Bearer $TOKEN")
HTTP_STATUS=$(echo "$CHECKOUT_RESP" | grep -o 'HTTP_STATUS:[0-9]*' | cut -d: -f2)
BODY=$(echo "$CHECKOUT_RESP" | grep -v 'HTTP_STATUS:')
echo "HTTP: $HTTP_STATUS" | tee -a "$OUT_DIR/zombie-log.txt"
echo "Resposta: $BODY" | tee -a "$OUT_DIR/zombie-log.txt"

# 7. Aguardar propagação Kafka (30s)
echo ""
echo "7. A aguardar 30s para propagação Kafka..."
sleep 30

# 8. Capturar evidências
echo ""
echo "8. A capturar evidências..."

# Estado da encomenda
docker exec -e PGPASSWORD="$DB_ORDERS_PASS" xcommerce-db-orders \
  psql -U "$DB_ORDERS_USER" -d xcommerce_orders \
  -c "SELECT id, status, order_date FROM orders ORDER BY order_date DESC LIMIT 5;" \
  2>/dev/null | tee "$OUT_DIR/zombie-orders.txt" || \
  echo "(não foi possível ligar à BD de encomendas)" | tee -a "$OUT_DIR/zombie-log.txt"

# Estado do inventário DEPOIS
docker exec -e PGPASSWORD="$DB_ORDERS_PASS" xcommerce-db-inventory \
  psql -U "$DB_ORDERS_USER" -d xcommerce_inventory \
  -c "SELECT product_id, quantity FROM inventory WHERE product_id IN (${PRODUCT_IDS[*]// /,});" \
  2>/dev/null | tee "$OUT_DIR/zombie-inventory-depois.txt" || \
  echo "(não foi possível ligar à BD de inventário)" | tee -a "$OUT_DIR/zombie-log.txt"

# Estado do carrinho
docker exec -e PGPASSWORD="$DB_ORDERS_PASS" xcommerce-db-cart \
  psql -U "$DB_ORDERS_USER" -d xcommerce_cart \
  -c "SELECT * FROM cart_items WHERE username='$USER';" \
  2>/dev/null | tee "$OUT_DIR/zombie-cart.txt" || \
  echo "(não foi possível ligar à BD de carrinho)" | tee -a "$OUT_DIR/zombie-log.txt"

# Lag do tópico Kafka
echo ""
echo "Lag do consumer group inventory-group:"
docker exec xcommerce-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group inventory-group \
  2>/dev/null | tee "$OUT_DIR/zombie-kafka-lag.txt" || \
  echo "(não foi possível consultar Kafka consumer groups)" | tee -a "$OUT_DIR/zombie-log.txt"

# 9. Resumo
echo ""
echo "=== RESUMO ==="
echo "Encomenda esperada: CONFIRMED | Observada:"
grep -i "handling\|confirmed\|cancelled" "$OUT_DIR/zombie-orders.txt" 2>/dev/null || echo "(verificar zombie-orders.txt)"
echo ""
echo "Inventário alterado?"
diff "$OUT_DIR/zombie-inventory-antes.txt" "$OUT_DIR/zombie-inventory-depois.txt" 2>/dev/null && \
  echo "SEM alteração (confirma H3)" || echo "COM alteração (inesperado)"
echo ""
echo "Evidências em: $OUT_DIR"
echo ""
echo "NOTA: Reiniciar inventory-service manualmente: docker start inventory-service"
