#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# T-MICRO-FALHA — Estado zombie nos microserviços
# ─────────────────────────────────────────────────────────────────────────────
# Sustenta a hipótese H3 (consistência sob falha parcial).
# Para o inventory-service durante o checkout para mostrar que a encomenda
# fica em estado intermédio (HANDLING) sem mecanismo automático de recuperação.
#
# Pré-requisitos: microserviços a correr.
# Saída: testes/k6/output/zombie-{order,stock,kafka}.txt
#
# Uso: ./testes-falha.sh
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$SCRIPT_DIR/k6/output"
BASE_URL="http://localhost:9000"

mkdir -p "$OUT_DIR"

echo "═══════════════════════════════════════════════════════════════"
echo "  TESTE DE FALHA — MICROSERVIÇOS (estado zombie)"
echo "═══════════════════════════════════════════════════════════════"
echo

if ! curl -fsS -o /dev/null "$BASE_URL/actuator/health"; then
  echo "✗ Gateway não responde. Sobe primeiro: cd 02-microservices && docker compose up -d"
  exit 1
fi

# ─── Setup ────────────────────────────────────────────────────────────────────
TOKEN_MICRO=$(curl -fsS -X POST -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' \
  "$BASE_URL/rest/user/login" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

docker exec xcommerce-db-inventory psql -U postgres -d xcommerce_inventory \
  -c "UPDATE inventory SET quantity=10000000, quantity_on_hand=10000000;" >/dev/null

STOCK_ANTES=$(docker exec xcommerce-db-inventory psql -U postgres -d xcommerce_inventory \
  -t -c "SELECT quantity FROM inventory WHERE product_id=1;" | tr -d ' ')

echo "▸ Estado ANTES da falha:"
echo "  Stock produto 1: $STOCK_ANTES"
echo

# ─── Parar inventory-service ─────────────────────────────────────────────────
echo "▸ Parar inventory-service (interrompe consumidor Kafka) …"
docker stop xcommerce-inventory-service >/dev/null
date "+  inventory-service parado às %H:%M:%S"
sleep 2
echo

# ─── Adicionar ao carrinho e checkout ────────────────────────────────────────
curl -s -X DELETE "$BASE_URL/rest/shoppingCart" \
  -H "Authorization: Bearer $TOKEN_MICRO" >/dev/null
curl -s -X PATCH "$BASE_URL/rest/shoppingCart/addProduct" \
  -H "Authorization: Bearer $TOKEN_MICRO" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}' >/dev/null

echo "▸ Fazer checkout com inventory-service parado …"
RESP=$(curl -s -X POST "$BASE_URL/rest/order/checkout" \
  -H "Authorization: Bearer $TOKEN_MICRO" \
  -H "Content-Type: application/json" -d '{}')
ORDER_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
echo "  Order criada: id=$ORDER_ID"
echo

echo "▸ Aguardar 10s — inventory continua parado, evento não consumido …"
sleep 10
echo

# ─── Captura de evidência ────────────────────────────────────────────────────
echo "═══════════════════════════════════════════════════════════════"
echo "  EVIDÊNCIA DO ESTADO ZOMBIE"
echo "═══════════════════════════════════════════════════════════════"
echo

echo "▸ Estado da encomenda $ORDER_ID:"
docker exec xcommerce-db-orders psql -U postgres -d xcommerce_orders \
  -c "SELECT id, username, status, total_amount FROM orders WHERE id=$ORDER_ID;" \
  | tee "$OUT_DIR/zombie-order.txt"

STATUS=$(docker exec xcommerce-db-orders psql -U postgres -d xcommerce_orders \
  -t -c "SELECT status FROM orders WHERE id=$ORDER_ID;" | tr -d ' ')

echo
echo "▸ Stock do produto 1:"
docker exec xcommerce-db-inventory psql -U postgres -d xcommerce_inventory \
  -c "SELECT product_id, quantity, quantity_on_hand FROM inventory WHERE product_id=1;" \
  | tee "$OUT_DIR/zombie-stock.txt"

STOCK_DEPOIS=$(docker exec xcommerce-db-inventory psql -U postgres -d xcommerce_inventory \
  -t -c "SELECT quantity FROM inventory WHERE product_id=1;" | tr -d ' ')

echo
echo "▸ Estado do consumer group inventory-group (Kafka):"
docker exec xcommerce-kafka /usr/bin/kafka-consumer-groups \
  --bootstrap-server kafka:29092 \
  --describe --group inventory-group 2>&1 | head -10 \
  | tee "$OUT_DIR/zombie-kafka.txt"
echo

# ─── Restaurar inventory ─────────────────────────────────────────────────────
echo "▸ Reiniciar inventory-service para restaurar o sistema …"
docker compose -f "$(dirname "$SCRIPT_DIR")/docker-compose.yml" up -d inventory-service \
  >/dev/null 2>&1
sleep 8
echo "  inventory-service reiniciado"
echo

# ─── Resumo ──────────────────────────────────────────────────────────────────
echo "═══════════════════════════════════════════════════════════════"
echo "  RESUMO"
echo "═══════════════════════════════════════════════════════════════"
echo "  Order $ORDER_ID em estado: $STATUS"
echo "  Stock decrementado: $STOCK_ANTES → $STOCK_DEPOIS (Δ=$((STOCK_ANTES - STOCK_DEPOIS)))"
echo
if [ "$STATUS" = "HANDLING" ]; then
  echo "  ✅ ESTADO ZOMBIE CONFIRMADO"
  echo "     A encomenda ficou presa em HANDLING."
  echo "     O evento Kafka tem lag não consumido."
  echo "     Não existe mecanismo automático de recuperação."
else
  echo "  ⚠ Estado inesperado ($STATUS) — verificar manualmente"
fi
echo
echo "Evidência em:"
echo "  • $OUT_DIR/zombie-order.txt"
echo "  • $OUT_DIR/zombie-stock.txt"
echo "  • $OUT_DIR/zombie-kafka.txt"
