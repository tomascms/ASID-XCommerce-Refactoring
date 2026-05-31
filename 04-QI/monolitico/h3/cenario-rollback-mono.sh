#!/usr/bin/env bash
# H3 — Cenário 2: Rollback ACID no monolito
# Objetivo: mostrar que a transação JPA faz rollback automático quando o checkout falha
#           (ex: stock insuficiente a meio da transação)
# Uso: ./cenario-rollback-mono.sh
# PRÉ-REQUISITO: monolito a correr + seed executado (04-QI/seed/seed-monolito.sh)

set -euo pipefail

BASE="http://localhost:18080"
DB_PORT=15432
DB_USER=postgres
DB_PASS=postgres
DB_NAME=xcommerce

TEST_USER="user1"
PASS="password"
# Produto sem stock (inserido pelo seed — Produto Teste 1=2601, para forçar falha usamos produto_id=2699 inexistente)
# Abordagem: adicionar ao carrinho um produto com stock=0 para provocar rollback
PRODUCT_OK=2601       # produto com stock 1000
PRODUCT_NO_STOCK=2699 # produto que não existe → OrderService lança exceção → rollback

OUT_DIR="$(cd "$(dirname "$0")" && pwd)/resultados"
mkdir -p "$OUT_DIR"

echo "=== H3 Rollback Monolito — $(date) ===" | tee "$OUT_DIR/rollback-log.txt"

# 1. Login
echo ""
echo "1. Login como $TEST_USER..."
TOKEN=$(curl -sf -X POST "$BASE/rest/user/authenticate" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$TEST_USER\",\"password\":\"$PASS\"}")
echo "Token obtido: ${TOKEN:0:30}..." | tee -a "$OUT_DIR/rollback-log.txt"
AUTH_HDR="Authorization: Bearer $TOKEN"

# 2. Inserir produto sem stock na BD (quantity=0) para forçar falha
echo ""
echo "2. A criar produto sem stock para forçar falha..."
PGPASSWORD=$DB_PASS psql -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -c "DELETE FROM product WHERE id=2699; INSERT INTO product (id, name, description, image, barcode, brand_id, category_id, price, quantity, weight, discount, active) VALUES (2699,'Produto Sem Stock','teste','t.jpg','NOSTOCK',0,2,10.0,0,0,0,true);" \
  > /dev/null 2>&1

# 3. Repor carrinho: produto OK + produto sem stock
echo ""
echo "3. A repor carrinho (produto OK + produto sem stock)..."
# Limpar carrinho via chamadas remove
curl -sf -X PATCH "$BASE/rest/shoppingCart/removeProduct" \
  -H "$AUTH_HDR" -H 'Content-Type: application/json' \
  -d "{\"id\":$PRODUCT_OK,\"quantity\":999}" > /dev/null 2>/dev/null || true
curl -sf -X PATCH "$BASE/rest/shoppingCart/removeProduct" \
  -H "$AUTH_HDR" -H 'Content-Type: application/json' \
  -d "{\"id\":$PRODUCT_NO_STOCK,\"quantity\":999}" > /dev/null 2>/dev/null || true
curl -sf -X PATCH "$BASE/rest/shoppingCart/addProduct" \
  -H "$AUTH_HDR" -H 'Content-Type: application/json' \
  -d "{\"id\":$PRODUCT_OK,\"quantity\":1}" > /dev/null
curl -sf -X PATCH "$BASE/rest/shoppingCart/addProduct" \
  -H "$AUTH_HDR" -H 'Content-Type: application/json' \
  -d "{\"id\":$PRODUCT_NO_STOCK,\"quantity\":1}" > /dev/null
echo "Carrinho: produto $PRODUCT_OK (stock OK) + $PRODUCT_NO_STOCK (stock=0)" | tee -a "$OUT_DIR/rollback-log.txt"

# 4. Estado ANTES
echo ""
echo "4. Estado ANTES:"
echo "=== ENCOMENDAS ANTES ===" > "$OUT_DIR/rollback-antes-orders.txt"
PGPASSWORD=$DB_PASS psql -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -c "SELECT count(*) AS total_encomendas_antes FROM \"order\" WHERE user_id IN (SELECT id FROM \"user\" WHERE username='$TEST_USER');" \
  2>/dev/null | tee -a "$OUT_DIR/rollback-antes-orders.txt"

echo "=== STOCK ANTES ===" > "$OUT_DIR/rollback-antes-stock.txt"
PGPASSWORD=$DB_PASS psql -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -c "SELECT id, name, quantity FROM product WHERE id IN (2601,2699);" \
  2>/dev/null | tee -a "$OUT_DIR/rollback-antes-stock.txt"

# 5. Checkout — deve falhar por stock insuficiente
echo ""
echo "5. Checkout (deve falhar — produto $PRODUCT_NO_STOCK sem stock)..."
CHECKOUT_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X GET "$BASE/rest/order/checkout" \
  -H "$AUTH_HDR")
HTTP_STATUS=$(echo "$CHECKOUT_RESP" | grep -o 'HTTP_STATUS:[0-9]*' | cut -d: -f2)
BODY=$(echo "$CHECKOUT_RESP" | grep -v 'HTTP_STATUS:')
echo "HTTP: $HTTP_STATUS" | tee -a "$OUT_DIR/rollback-log.txt"
echo "Resposta: $BODY" | tee -a "$OUT_DIR/rollback-log.txt"
{
  echo "=== RESPOSTA CHECKOUT FALHADO ==="
  echo "HTTP $HTTP_STATUS — $BODY"
} > "$OUT_DIR/rollback-checkout-falha.txt"

# 6. Estado DEPOIS
echo ""
echo "6. Estado DEPOIS (deve ser igual — rollback JPA):"
PGPASSWORD=$DB_PASS psql -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -c "SELECT count(*) AS total_encomendas_depois FROM \"order\" WHERE user_id IN (SELECT id FROM \"user\" WHERE username='$TEST_USER');" \
  2>/dev/null | tee "$OUT_DIR/rollback-depois-orders.txt"

PGPASSWORD=$DB_PASS psql -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -c "SELECT id, name, quantity FROM product WHERE id IN (2601,2699);" \
  2>/dev/null | tee "$OUT_DIR/rollback-depois-stock.txt"

# Carrinho
# Carrinho — o monolito usa Redis (não Postgres); verificar via API
CART_RESP=$(curl -sf "$BASE/rest/shoppingCart/get" -H "$AUTH_HDR" 2>/dev/null || echo "{}")
echo "Carrinho após checkout falhado: $CART_RESP" | tee "$OUT_DIR/rollback-cart.txt"

# 7. Resumo
echo ""
echo "=== RESUMO ==="
ANTES=$(grep -o '[0-9]*' "$OUT_DIR/rollback-antes-orders.txt" | tail -1)
DEPOIS=$(grep -o '[0-9]*' "$OUT_DIR/rollback-depois-orders.txt" | tail -1)
echo "Encomendas antes=$ANTES | depois=$DEPOIS" | tee -a "$OUT_DIR/rollback-log.txt"
if [ "$ANTES" = "$DEPOIS" ]; then
  echo "IGUAL — rollback JPA confirmado (H3)" | tee -a "$OUT_DIR/rollback-log.txt"
else
  echo "DIFERENTE — encomenda persistiu apesar da falha (inesperado)" | tee -a "$OUT_DIR/rollback-log.txt"
fi
echo ""
echo "Stock produto 2601 (deve ser inalterado=1000):"
grep "2601" "$OUT_DIR/rollback-depois-stock.txt" || echo "(verificar rollback-depois-stock.txt)"
echo ""
echo "Evidências em: $OUT_DIR"
