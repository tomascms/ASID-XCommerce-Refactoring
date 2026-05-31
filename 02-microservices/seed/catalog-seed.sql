-- Seed: 1 categoria, 1 marca, 20 produtos de teste

INSERT INTO categories (name) VALUES ('Categoria Teste') ON CONFLICT (name) DO NOTHING;
INSERT INTO brands (name) VALUES ('Marca Teste') ON CONFLICT (name) DO NOTHING;

INSERT INTO products (name, description, price, status, category_id, brand_id)
SELECT
    'Produto Teste ' || i,
    'Descrição produto ' || i,
    (10 + i)::numeric,
    'ACTIVE',
    (SELECT id FROM categories WHERE name = 'Categoria Teste'),
    (SELECT id FROM brands WHERE name = 'Marca Teste')
FROM generate_series(1, 20) AS i;
