-- Relax NOT NULL on products.sku — REST POST /products via admin UI may not provide a sku.
-- Postgres UNIQUE constraints already allow multiple NULL values, so the unique index stays valid.
ALTER TABLE products ALTER COLUMN sku DROP NOT NULL;
