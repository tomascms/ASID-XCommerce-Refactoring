-- Fix reviews table to match Review entity (eval, grade instead of rating, user_id)
-- The V1 schema had user_id NOT NULL and rating NOT NULL, but the entity uses eval/grade

ALTER TABLE reviews
    ADD COLUMN IF NOT EXISTS eval TEXT;

ALTER TABLE reviews
    ADD COLUMN IF NOT EXISTS grade FLOAT;

ALTER TABLE reviews
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE reviews
    ALTER COLUMN rating DROP NOT NULL;

UPDATE reviews SET eval = '' WHERE eval IS NULL;

ALTER TABLE reviews
    ALTER COLUMN eval SET NOT NULL;
