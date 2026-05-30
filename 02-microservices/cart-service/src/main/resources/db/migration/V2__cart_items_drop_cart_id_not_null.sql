-- Drop NOT NULL on cart_id and cart_items columns the simplified JPA entity does not populate.
-- The CartItem JPA entity in cart-service only maps id, username, product_id, quantity.
-- Make the legacy schema columns optional so JPA inserts succeed without a parent Cart row.

ALTER TABLE cart_items ALTER COLUMN cart_id DROP NOT NULL;
ALTER TABLE cart_items ALTER COLUMN product_name DROP NOT NULL;
ALTER TABLE cart_items ALTER COLUMN unit_price DROP NOT NULL;
ALTER TABLE cart_items ALTER COLUMN total_price DROP NOT NULL;
