-- Seed: stock 1000 para os produtos 1..20
-- product_id assume IDs gerados pelo catalog-service (SERIAL começando em 1)

INSERT INTO inventory (product_id, quantity)
SELECT i, 1000
FROM generate_series(1, 20) AS i
ON CONFLICT DO NOTHING;
