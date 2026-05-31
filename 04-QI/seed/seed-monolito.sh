#!/usr/bin/env bash
# =============================================================================
# seed-monolito.sh — Popula a BD do monolito para os testes
#
# PRÉ-REQUISITO: monolito a correr e healthy
#   cd 01-monolith && docker compose up -d
#   Aguardar healthcheck: docker compose ps
#
# Uso: bash 04-QI/seed/seed-monolito.sh
# =============================================================================

set -euo pipefail

DB_CONTAINER="monolith-db"
DB_HOST="localhost"
DB_PORT="15432"
PG_USER=postgres
PG_PASS=postgres
DB_NAME=xcommerce

psql_exec() {
  docker exec -i -e PGPASSWORD="$PG_PASS" "$DB_CONTAINER" psql -U "$PG_USER" -d "$DB_NAME"
}

echo "=== Seed Monolito — $(date) ==="

# Testar ligação
docker exec -e PGPASSWORD="$PG_PASS" "$DB_CONTAINER" psql -U "$PG_USER" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1 || {
  echo "ERRO: contentor '$DB_CONTAINER' não disponível. Corre: cd 01-monolith && docker compose up -d"
  exit 1
}
echo "BD acessível."

# -----------------------------------------------------------------------
# 1. Utilizadores — 50 users com role ROLE_USER
# BCrypt de 'password': $2a$10$Sbh0IfZdkaW4qaiHgtgP5Ofe2VNcazCGf9DxnGqOAExsjWp/h8kRG
# -----------------------------------------------------------------------
echo ""
echo "1/4 users — a criar 50 utilizadores..."
psql_exec <<'SQL'
DELETE FROM authority WHERE username LIKE 'user%';
DELETE FROM "user" WHERE username LIKE 'user%';

INSERT INTO "user" (id, username, password, first_name, last_name, email_address, address, active)
SELECT
  1000 + i,
  'user' || i,
  '$2a$10$Sbh0IfZdkaW4qaiHgtgP5Ofe2VNcazCGf9DxnGqOAExsjWp/h8kRG',
  'Nome' || i,
  'Apelido' || i,
  'user' || i || '@test.com',
  'Rua Teste ' || i,
  true
FROM generate_series(1, 50) AS i;

INSERT INTO authority (id, username, authority, active)
SELECT 1000 + i, 'user' || i, 'ROLE_USER', true
FROM generate_series(1, 50) AS i;

SELECT count(*) AS utilizadores FROM "user" WHERE username LIKE 'user%';
SQL

# -----------------------------------------------------------------------
# 2. Produtos de teste — 20 produtos na categoria existente (id=2 Phones)
# -----------------------------------------------------------------------
echo ""
echo "2/4 products — a criar 20 produtos de teste..."
psql_exec <<'SQL'
DELETE FROM product WHERE name LIKE 'Produto Teste%';

INSERT INTO product (id, name, description, image, barcode, brand_id, category_id, price, quantity, weight, discount, active)
SELECT
  2600 + i,
  'Produto Teste ' || i,
  'Descrição produto ' || i,
  'test.jpg',
  'TEST' || lpad(i::text, 4, '0'),
  0,  -- brand OPPO (id=0)
  2,  -- category Phones (id=2)
  (10 + i)::float,
  1000,
  100,
  0,
  true
FROM generate_series(1, 20) AS i
ON CONFLICT (id) DO UPDATE SET quantity = 1000;

SELECT count(*) AS produtos, min(id) AS id_min, max(id) AS id_max
FROM product WHERE name LIKE 'Produto Teste%';
SQL

# -----------------------------------------------------------------------
# 3. Encomendas — 250 encomendas HANDLING (5 × 50 utilizadores)
# -----------------------------------------------------------------------
echo ""
echo "3/4 orders — a criar 250 encomendas..."
psql_exec <<'SQL'
DELETE FROM order_line WHERE order_id IN (SELECT id FROM "order" WHERE user_id IN (SELECT id FROM "user" WHERE username LIKE 'user%'));
DELETE FROM "order" WHERE user_id IN (SELECT id FROM "user" WHERE username LIKE 'user%');

INSERT INTO "order" (id, user_id, products_total_price, shipping_cost, status, active)
SELECT
  3000 + ((u.rn - 1) * 5) + o,
  u.id,
  100.00,
  10.00,
  'HANDLING',
  true
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn FROM "user" WHERE username LIKE 'user%') u,
     generate_series(1, 5) AS o;

INSERT INTO order_line (id, order_id, product_id, quantity, unit_price, active)
SELECT
  4000 + (((ord.rn - 1) * 4) + p),
  ord.id,
  2601,
  1,
  11.00,
  true
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn FROM "order" WHERE id >= 3000) ord,
     generate_series(1, 4) AS p;

SELECT count(*) AS encomendas FROM "order" WHERE id >= 3000;
SELECT count(*) AS order_lines FROM order_line WHERE id >= 4000;
SQL

# -----------------------------------------------------------------------
# 4. Resumo — IDs dos produtos para usar nos scripts k6
# -----------------------------------------------------------------------
echo ""
echo "=== IDs dos primeiros 4 produtos de teste (usar em PRODUCT_IDS no t3-checkout.js) ==="
docker exec -e PGPASSWORD="$PG_PASS" "$DB_CONTAINER" psql -U "$PG_USER" -d "$DB_NAME" \
  -c "SELECT id, name, price, quantity FROM product WHERE name LIKE 'Produto Teste%' ORDER BY id LIMIT 4;"

echo ""
echo "Seed monolito concluído."
echo "Produto IDs para t3-checkout.js do monolito: 2601, 2602"
