-- Relax NOT NULL constraints on order schema columns the JPA Order entity does not populate.
-- The minimal entity in order-service maps only id, username, productId, quantity, status,
-- orderDate, totalAmount and items. Legacy columns inherited from a richer schema must be
-- nullable so JPA inserts succeed.

ALTER TABLE orders ALTER COLUMN order_number DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN subtotal_amount DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN total_amount DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN shipping_address DROP NOT NULL;

ALTER TABLE order_items ALTER COLUMN order_id DROP NOT NULL;
ALTER TABLE order_items ALTER COLUMN product_name DROP NOT NULL;
ALTER TABLE order_items ALTER COLUMN unit_price DROP NOT NULL;
ALTER TABLE order_items ALTER COLUMN total_price DROP NOT NULL;
