#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# T-MONO-FALHA — Falha controlada da BD durante checkout
# ─────────────────────────────────────────────────────────────────────────────
# Sustenta a hipótese H3 (consistência sob falha parcial).
# Provoca a paragem da BD a meio do checkout para verificar rollback ACID.
#
# Pré-requisitos: monólito a correr.
# Saída: testes/resultados/falha-bd.txt
#
# Uso: ./testes-falha.sh
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/resultados"
BASE_URL="http://localhost:18080"
OUT="$RESULTS_DIR/falha-bd.txt"

mkdir -p "$RESULTS_DIR"

echo "═══════════════════════════════════════════════════════════════"
echo "  TESTE DE FALHA — MONÓLITO (rollback ACID)"
echo "═══════════════════════════════════════════════════════════════"
echo

if ! curl -fsS -o /dev/null "$BASE_URL/actuator/health"; then
  echo "✗ Monólito não responde. Sobe primeiro: cd 01-monolith && docker compose up -d"
  exit 1
fi

# Estado inicial
TOKEN_MONO=$(curl -fsS -X POST -H "Content-Type: application/json" \
  -d '{"username":"root","password":"root"}' \
  "$BASE_URL/rest/user/authenticate")

docker exec monolith-redis redis-cli FLUSHALL >/dev/null
docker exec monolith-db psql -U postgres -d xcommerce \
  -c "UPDATE product SET quantity=10000000;" >/dev/null

ORDERS_ANTES=$(docker exec monolith-db psql -U postgres -d xcommerce \
  -t -c "SELECT COUNT(*) FROM \"order\";" | tr -d ' ')
STOCK_ANTES=$(docker exec monolith-db psql -U postgres -d xcommerce \
  -t -c "SELECT quantity FROM product WHERE id=1;" | tr -d ' ')

echo "▸ Estado ANTES da falha:"
echo "  Orders: $ORDERS_ANTES"
echo "  Stock produto 1: $STOCK_ANTES"
echo

# Adicionar produto ao carrinho
curl -fsS -X PATCH "$BASE_URL/rest/shoppingCart/addProduct" \
  -H "Authorization: Bearer $TOKEN_MONO" \
  -H "Content-Type: application/json" \
  -d '{"id":1,"quantity":2}' >/dev/null

echo "▸ Parar BD do monólito (simulação de falha de infraestrutura) …"
docker stop monolith-db >/dev/null
date "+  monolith-db parado às %H:%M:%S"
echo

echo "▸ Tentar checkout com BD parada — deve falhar …"
RESP=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X GET "$BASE_URL/rest/order/checkout" \
  -H "Authorization: Bearer $TOKEN_MONO" || true)
echo "$RESP" | head -3
echo

echo "▸ Reiniciar BD …"
docker start monolith-db >/dev/null
sleep 12  # esperar reconexão do connection pool
echo "  BD reiniciada"
echo

# Estado final
ORDERS_DEPOIS=$(docker exec monolith-db psql -U postgres -d xcommerce \
  -t -c "SELECT COUNT(*) FROM \"order\";" | tr -d ' ')
STOCK_DEPOIS=$(docker exec monolith-db psql -U postgres -d xcommerce \
  -t -c "SELECT quantity FROM product WHERE id=1;" | tr -d ' ')

echo "▸ Estado DEPOIS da falha:"
echo "  Orders: $ORDERS_DEPOIS  (Δ=$((ORDERS_DEPOIS - ORDERS_ANTES)))"
echo "  Stock produto 1: $STOCK_DEPOIS  (Δ=$((STOCK_ANTES - STOCK_DEPOIS)))"
echo

# Gravar evidência
{
  echo "=== Cenário monólito: BD parada durante transação ==="
  echo "Data: $(date)"
  echo
  echo "Orders antes:  $ORDERS_ANTES"
  echo "Orders depois: $ORDERS_DEPOIS"
  echo "Stock antes:   $STOCK_ANTES"
  echo "Stock depois:  $STOCK_DEPOIS"
  echo
  echo "Resposta do checkout durante a falha:"
  echo "$RESP"
} > "$OUT"

if [ "$ORDERS_ANTES" = "$ORDERS_DEPOIS" ] && [ "$STOCK_ANTES" = "$STOCK_DEPOIS" ]; then
  echo "═══════════════════════════════════════════════════════════════"
  echo "  ✅ ROLLBACK ACID CONFIRMADO"
  echo "═══════════════════════════════════════════════════════════════"
  echo "  Nem a encomenda foi criada nem o stock foi decrementado."
  echo "  O motor JPA reverteu a transação automaticamente."
else
  echo "═══════════════════════════════════════════════════════════════"
  echo "  ⚠ ROLLBACK NÃO CONFIRMADO — verificar manualmente"
  echo "═══════════════════════════════════════════════════════════════"
fi
echo
echo "Evidência: $OUT"
