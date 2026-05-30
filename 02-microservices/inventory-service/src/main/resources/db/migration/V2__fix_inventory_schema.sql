-- Fix inventory table to match Inventory entity (productId + quantity only)
-- The V1 schema had extra columns (sku NOT NULL, quantity_on_hand, etc.) not mapped in the entity

ALTER TABLE inventory
    ADD COLUMN IF NOT EXISTS quantity INTEGER;

UPDATE inventory SET quantity = quantity_on_hand WHERE quantity IS NULL;

ALTER TABLE inventory
    ALTER COLUMN sku DROP NOT NULL;
