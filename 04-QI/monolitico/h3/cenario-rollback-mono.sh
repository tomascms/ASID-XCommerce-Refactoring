#!/usr/bin/env bash
# H3 — Cenário 2: Rollback ACID no monolito
# Objetivo: mostrar que a transação JPA faz rollback automático quando a BD falha a meio
# Uso: ./cenario-rollback-mono.sh
# PRÉ-REQUISITO: monolito a correr + seed executado (04-QI/seed/seed-monolito.sh)

set -euo pipefail

BASE="http://localhost:18080"
DB_PORT=15432
DB_USER=postgres
DB_PASS=postgres
DB_NAME=xcommerce

USER="user1"
PASS="password"
PRODUCT_IDS=(1 2)

OUT_DIR="$(cd "$(dirname "$0")" && pwd)/resultados"
mkdir -p "$OUT_DIR"

echo "=== H3 Rollback Monolito — $(date) ===" | tee "$OUT_DIR/rollback-log.txt"

# 1. Login
echo ""
echo "1. Login como $USER..."
TOKEN=$(curl -sf -X POST "$BASE/rest/user/authenticate" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}")
echo "Token obtido: ${TOKEN:0:30}..." | tee -a "$OUT_DIR/rollback-log.txt"

AUTH_HDR="Authorization: Bearer $TOKEN"

# 2. Repor carrinho
echo ""
echo "2. A repor carrinho..."
for pid in "${PRODUCT_IDS[@]}"; do
  curl -sf -X PATCH "$BASE/rest/shoppingCart/addProduct" \
    -H "$AUTH_HDR" \
    -H 'Content-Type: application/json' \
    -d "{\"id\":$pid,\"quantity\":2}" > /dev/null
done
echo "Carrinho reposto com produtos: ${PRODUCT_IDS[*]}" | tee -a "$OUT_DIR/rollback-log.txt"

# 3. Estado ANTES
echo ""
echo "3. Estado ANTES (encomendas + stock + carrinho):"
PGPASSWORD=$DB_PASS psql -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -c "SELECT count(*) AS total_orders FROM orders;" \
  2>/dev/null | tee "$OUT_DIR/rollback-antes-orders.txt"

PGPASSWORD=$DB_PASS psql -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -c "SELECT id, quantity FROM products WHERE id IN (${PRODUCT_IDS[*]// /,});" \
  2>/dev/null | tee "$OUT_DIR/rollback-antes-stock.txt"

# 4. Parar a BD a meio do checkout
# Estratégia: iniciar checkout em background, parar BD após 100ms
echo ""
echo "4. A iniciar checkout + parar BD a meio..."
(curl -sf -X GET "$BASE/rest/order/checkout" \
  -H "$AUTH_HDR" > "$OUT_DIR/rollback-checkout-resp.txt" 2>&1) &
CHECKOUT_PID=$!
sleep 0.1
docker stop monolith-db 2>/dev/null
echo "monolith-db PARADA" | tee -a "$OUT_DIR/rollback-log.txt"
wait $CHECKOUT_PID || true
echo "Resposta do checkout:" | tee -a "$OUT_DIR/rollback-log.txt"
cat "$OUT_DIR/rollback-checkout-resp.txt" | tee -a "$OUT_DIR/rollback-log.txt"

# 5. Reiniciar BD e verificar estado
echo ""
echo "5. A reiniciar BD e verificar estado..."
docker start monolith-db
sleep 10  # aguardar BD estar pronta

PGPASSWORD=$DB_PASS psql -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -c "SELECT count(*) AS total_orders FROM orders;" \
  2>/dev/null | tee "$OUT_DIR/rollback-depois-orders.txt"

PGPASSWORD=$DB_PASS psql -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -c "SELECT id, quantity FROM products WHERE id IN (${PRODUCT_IDS[*]// /,});" \
  2>/dev/null | tee "$OUT_DIR/rollback-depois-stock.txt"

PGPASSWORD=$DB_PASS psql -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -c "SELECT * FROM shopping_cart_items sc JOIN users u ON sc.user_id=u.id WHERE u.username='$USER';" \
  2>/dev/null | tee "$OUT_DIR/rollback-cart.txt"

# 6. Resumo
echo ""
echo "=== RESUMO ==="
echo "Encomendas antes vs depois (devem ser iguais — rollback automático):"
diff "$OUT_DIR/rollback-antes-orders.txt" "$OUT_DIR/rollback-depois-orders.txt" && \
  echo "IGUAL — rollback confirmado (H3)" || echo "DIFERENTE — encomenda persistiu (inesperado)"
echo ""
echo "Stock antes vs depois (deve ser igual):"
diff "$OUT_DIR/rollback-antes-stock.txt" "$OUT_DIR/rollback-depois-stock.txt" && \
  echo "IGUAL — stock não foi decrementado" || echo "DIFERENTE"
echo ""
echo "Evidências em: $OUT_DIR"
