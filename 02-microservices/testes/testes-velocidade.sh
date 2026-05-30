#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# T-MICRO-VELOCIDADE — Teste de velocidade dos microserviços XCommerce
# ─────────────────────────────────────────────────────────────────────────────
# Sustenta a hipótese H2a (custo super-linear da decomposição).
# Executa T1 (leitura simples) e T2 (checkout) com a ferramenta k6.
#
# Pré-requisitos: microserviços a correr (docker compose up -d em 02-microservices/).
# Saída: ficheiros JSON em testes/k6/output/.
#
# Uso: ./testes-velocidade.sh
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K6_DIR="$SCRIPT_DIR/k6"
OUT_DIR="$K6_DIR/output"
BASE_URL="http://localhost:9000"

mkdir -p "$OUT_DIR"

echo "═══════════════════════════════════════════════════════════════"
echo "  TESTE DE VELOCIDADE — MICROSERVIÇOS"
echo "═══════════════════════════════════════════════════════════════"
echo

# ─── Verificar gateway ────────────────────────────────────────────────────────
echo "▸ Verificar gateway em $BASE_URL …"
if ! curl -fsS -o /dev/null "$BASE_URL/actuator/health"; then
  echo "  ✗ Gateway não responde. Sobe primeiro com:"
  echo "    cd 02-microservices && docker compose up -d"
  exit 1
fi
echo "  ✓ Gateway UP"
echo

# ─── Garantir que existem produtos no catálogo e stock no inventário ─────────
echo "▸ Verificar BDs (catálogo e inventário) …"
PRODS=$(docker exec xcommerce-db-catalog psql -U postgres -d xcommerce_catalog \
  -t -c "SELECT COUNT(*) FROM products;" | tr -d ' ')
INVS=$(docker exec xcommerce-db-inventory psql -U postgres -d xcommerce_inventory \
  -t -c "SELECT COUNT(*) FROM inventory;" | tr -d ' ')

if [ "$PRODS" = "0" ]; then
  echo "  ⚠ Catálogo vazio — a popular …"
  docker exec xcommerce-db-catalog psql -U postgres -d xcommerce_catalog -c "
    INSERT INTO brands (name, description, active) VALUES ('Samsung', 'Samsung tech', true), ('Apple', 'Apple tech', true);
    INSERT INTO categories (name, description, active) VALUES ('Smartphones', '', true), ('Laptops', '', true);
    INSERT INTO products (sku, name, description, category_id, brand_id, price, status, active, quantity)
    VALUES
      ('SKU-S24', 'Samsung Galaxy S24', 'Smartphone', 1, 1, 899.99, 'ACTIVE', true, 10000000),
      ('SKU-IP15', 'iPhone 15 Pro', 'Smartphone', 1, 2, 1199.99, 'ACTIVE', true, 10000000),
      ('SKU-MB14', 'MacBook Pro 14', 'Laptop', 2, 2, 1999.99, 'ACTIVE', true, 10000000);
  " >/dev/null
fi

if [ "$INVS" = "0" ]; then
  echo "  ⚠ Inventário vazio — a popular …"
  docker exec xcommerce-db-inventory psql -U postgres -d xcommerce_inventory -c "
    INSERT INTO inventory (product_id, sku, quantity_on_hand, quantity_reserved, quantity_damaged, reorder_level, reorder_quantity, warehouse_location, quantity)
    VALUES
      (1, 'SKU-S24', 10000000, 0, 0, 10, 100, 'WH-LX', 10000000),
      (2, 'SKU-IP15', 10000000, 0, 0, 10, 100, 'WH-LX', 10000000),
      (3, 'SKU-MB14', 10000000, 0, 0, 10, 100, 'WH-LX', 10000000);
  " >/dev/null
fi

# Repor stock alto
docker exec xcommerce-db-inventory psql -U postgres -d xcommerce_inventory \
  -c "UPDATE inventory SET quantity=10000000, quantity_on_hand=10000000;" >/dev/null
echo "  ✓ BDs OK (produtos: $PRODS, stock alto reposto)"
echo

# ─── Login para T2 ────────────────────────────────────────────────────────────
echo "▸ Login (admin/123456) para obter token JWT …"
TOKEN_MICRO=$(curl -fsS -X POST -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' \
  "$BASE_URL/rest/user/login" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo "  ✓ Token obtido (${#TOKEN_MICRO} caracteres)"
curl -s -X DELETE "$BASE_URL/rest/shoppingCart" \
  -H "Authorization: Bearer $TOKEN_MICRO" >/dev/null
echo

# ─── T1: Leitura simples ─────────────────────────────────────────────────────
echo "═══════════════════════════════════════════════════════════════"
echo "  T1 — Leitura de catálogo (10 VUs, sem think time, 100s)"
echo "═══════════════════════════════════════════════════════════════"
date "+  Início: %H:%M:%S"
k6 run --quiet \
  --summary-export="$OUT_DIR/t-micro-1.json" \
  "$K6_DIR/t-micro-1-leitura.js" 2>&1 | tail -8
date "+  Fim:    %H:%M:%S"
echo

# ─── T2: Checkout ────────────────────────────────────────────────────────────
echo "═══════════════════════════════════════════════════════════════"
echo "  T2 — Checkout (1 VU, sem think time, 100s)"
echo "═══════════════════════════════════════════════════════════════"
docker exec xcommerce-db-inventory psql -U postgres -d xcommerce_inventory \
  -c "UPDATE inventory SET quantity=10000000, quantity_on_hand=10000000;" >/dev/null
date "+  Início: %H:%M:%S"
k6 run --quiet \
  --summary-export="$OUT_DIR/t-micro-2.json" \
  -e TOKEN="$TOKEN_MICRO" \
  "$K6_DIR/t-micro-2-checkout.js" 2>&1 | tail -8
date "+  Fim:    %H:%M:%S"
echo

# ─── Resumo ──────────────────────────────────────────────────────────────────
echo "═══════════════════════════════════════════════════════════════"
echo "  RESUMO"
echo "═══════════════════════════════════════════════════════════════"
python3 - <<EOF
import json
for label, path in [
    ("T1 leitura ", "$OUT_DIR/t-micro-1.json"),
    ("T2 checkout", "$OUT_DIR/t-micro-2.json"),
]:
    with open(path) as f:
        d = json.load(f)
    it = d["metrics"]["iteration_duration"]
    its = d["metrics"]["iterations"]
    print(f"  {label}: p50={it['med']:.2f}ms · p95={it['p(95)']:.2f}ms · "
          f"{int(its['count'])} iter · {its['rate']:.0f}/s")
EOF
echo
echo "Ficheiros gerados:"
echo "  • $OUT_DIR/t-micro-1.json"
echo "  • $OUT_DIR/t-micro-2.json"
