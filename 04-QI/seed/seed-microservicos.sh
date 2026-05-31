#!/usr/bin/env bash
# =============================================================================
# seed-microservicos.sh — Popula as BDs dos microserviços para os testes
#
# PRÉ-REQUISITO: microserviços a correr e healthy
#   cd 02-microservices && docker compose up -d
#   Aguardar (~2 min): docker compose ps
#
# Uso: ./seed-microservicos.sh
# =============================================================================

set -euo pipefail

DB_IDENTITY="xcommerce-db-identity"
DB_CATALOG="xcommerce-db-catalog"
DB_CART="xcommerce-db-cart"
DB_ORDERS="xcommerce-db-orders"
DB_INVENTORY="xcommerce-db-inventory"
PG_USER=postgres
PG_PASS=postgres

psql_exec() {
  local container="$1" db="$2"
  docker exec -i -e PGPASSWORD="$PG_PASS" "$container" psql -U "$PG_USER" -d "$db"
}

echo "=== Seed Microserviços — $(date) ==="

# Testar ligações
for c in "$DB_IDENTITY" "$DB_CATALOG" "$DB_CART" "$DB_ORDERS" "$DB_INVENTORY"; do
  docker exec -e PGPASSWORD="$PG_PASS" "$c" psql -U "$PG_USER" -c "SELECT 1;" > /dev/null 2>&1 || {
    echo "ERRO: contentor '$c' não disponível. Corre: docker compose up -d"
    exit 1
  }
done
echo "Todas as BDs acessíveis."

# -----------------------------------------------------------------------
# 1. identity — 50 utilizadores
# BCrypt de 'password': $2a$10$Sbh0IfZdkaW4qaiHgtgP5Ofe2VNcazCGf9DxnGqOAExsjWp/h8kRG
# -----------------------------------------------------------------------
echo ""
echo "1/5 identity — a criar 50 utilizadores..."
psql_exec "$DB_IDENTITY" "xcommerce_identity" <<'SQL'
DELETE FROM users WHERE username LIKE 'user%';
INSERT INTO users (username, email, password_hash, first_name, last_name, address, role, active)
SELECT
  'user' || i, 'user' || i || '@test.com',
  '$2a$10$Sbh0IfZdkaW4qaiHgtgP5Ofe2VNcazCGf9DxnGqOAExsjWp/h8kRG',
  'Nome' || i, 'Apelido' || i, 'Rua Teste ' || i, 'CUSTOMER', true
FROM generate_series(1, 50) AS i
ON CONFLICT (username) DO NOTHING;
SELECT count(*) AS utilizadores FROM users WHERE username LIKE 'user%';
SQL

# -----------------------------------------------------------------------
# 2. catalog — 20 produtos
# -----------------------------------------------------------------------
echo ""
echo "2/5 catalog — a criar 20 produtos..."
psql_exec "$DB_CATALOG" "xcommerce_catalog" <<'SQL'
DELETE FROM products WHERE name LIKE 'Produto Teste%';
DELETE FROM brands WHERE name = 'Marca Teste';
DELETE FROM categories WHERE name = 'Categoria Teste';
INSERT INTO categories (name) VALUES ('Categoria Teste') ON CONFLICT (name) DO NOTHING;
INSERT INTO brands (name) VALUES ('Marca Teste') ON CONFLICT (name) DO NOTHING;
INSERT INTO products (name, description, price, status, category_id, brand_id)
SELECT
  'Produto Teste ' || i, 'Descrição produto ' || i, (10 + i)::numeric, 'ACTIVE',
  (SELECT id FROM categories WHERE name = 'Categoria Teste'),
  (SELECT id FROM brands WHERE name = 'Marca Teste')
FROM generate_series(1, 20) AS i;
SELECT count(*) AS produtos FROM products WHERE name LIKE 'Produto Teste%';
SQL

# -----------------------------------------------------------------------
# 3. inventory — stock 1000 para produtos 1..20
# -----------------------------------------------------------------------
echo ""
echo "3/5 inventory — a criar stock..."
psql_exec "$DB_INVENTORY" "xcommerce_inventory" <<'SQL'
DELETE FROM inventory WHERE product_id IN (SELECT generate_series(1, 20));
INSERT INTO inventory (product_id, quantity)
SELECT i, 1000 FROM generate_series(1, 20) AS i
ON CONFLICT DO NOTHING;
SELECT count(*) AS entradas FROM inventory;
SQL

# -----------------------------------------------------------------------
# 4. cart — limpar carrinhos de teste
# -----------------------------------------------------------------------
echo ""
echo "4/5 cart — a limpar carrinhos..."
psql_exec "$DB_CART" "xcommerce_cart" <<'SQL'
DELETE FROM cart_items WHERE username LIKE 'user%';
SELECT count(*) AS cart_items FROM cart_items WHERE username LIKE 'user%';
SQL

# -----------------------------------------------------------------------
# 5. orders — 250 encomendas (5 por utilizador, 4 itens cada)
# Tabelas Hibernate: orders + order_item + orders_items (join @OneToMany)
# -----------------------------------------------------------------------
echo ""
echo "5/5 orders — a criar 250 encomendas..."
psql_exec "$DB_ORDERS" "xcommerce_orders" <<'SQL'
DELETE FROM orders_items;
DELETE FROM order_item;
DELETE FROM orders WHERE username LIKE 'user%';

INSERT INTO orders (username, status, order_date, total_amount)
SELECT 'user' || u, 'CONFIRMED', NOW(), 100.00
FROM generate_series(1, 50) AS u,
     generate_series(1, 5)  AS o;

INSERT INTO order_item (price, product_id, quantity)
SELECT 10.00, p.pid, 1
FROM orders o
JOIN (VALUES (1),(2),(3),(4)) AS p(pid) ON true
WHERE o.username LIKE 'user%';

INSERT INTO orders_items (order_id, items_id)
SELECT o.id, oi.id
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn FROM orders WHERE username LIKE 'user%') o
JOIN (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn FROM order_item) oi
  ON oi.rn BETWEEN (o.rn - 1) * 4 + 1 AND o.rn * 4;

SELECT count(*) AS encomendas FROM orders WHERE username LIKE 'user%';
SELECT count(*) AS order_items FROM order_item;
SQL

# -----------------------------------------------------------------------
# Resumo
# -----------------------------------------------------------------------
echo ""
echo "=== IDs dos primeiros 4 produtos (usar em PRODUCT_IDS nos scripts k6) ==="
psql_exec "$DB_CATALOG" "xcommerce_catalog" <<'SQL'
SELECT id, name, price FROM products WHERE name LIKE 'Produto Teste%' ORDER BY id LIMIT 4;
SQL

echo ""
echo "Seed microserviços concluído."
