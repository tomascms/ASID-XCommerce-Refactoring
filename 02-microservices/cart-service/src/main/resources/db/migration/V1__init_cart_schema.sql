-- Cart Service Database Schema Migration
-- Migration: V1__init_cart_schema.sql
-- Purpose: Initialize shopping cart tables

CREATE TABLE IF NOT EXISTS carts (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    session_id VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    total_items INTEGER DEFAULT 0,
    subtotal_amount NUMERIC(10, 2) DEFAULT 0,
    discount_amount NUMERIC(10, 2) DEFAULT 0,
    tax_amount NUMERIC(10, 2) DEFAULT 0,
    total_amount NUMERIC(10, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    CONSTRAINT chk_cart_amounts CHECK (subtotal_amount >= 0 AND total_amount >= 0)
);

CREATE TABLE IF NOT EXISTS cart_items (
    id SERIAL PRIMARY KEY,
    cart_id INTEGER NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id INTEGER NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL,
    discount_percentage NUMERIC(5, 2) DEFAULT 0,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_quantity CHECK (quantity > 0),
    CONSTRAINT chk_prices CHECK (unit_price >= 0 AND total_price >= 0)
);

CREATE TABLE IF NOT EXISTS cart_coupons (
    id SERIAL PRIMARY KEY,
    cart_id INTEGER NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    coupon_code VARCHAR(100) NOT NULL,
    discount_amount NUMERIC(10, 2) NOT NULL,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (cart_id, coupon_code)
);

CREATE TABLE IF NOT EXISTS cart_abandoned_history (
    id SERIAL PRIMARY KEY,
    cart_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    total_amount NUMERIC(10, 2),
    abandoned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    recovered BOOLEAN DEFAULT false,
    recovered_at TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_carts_user_id ON carts(user_id);
CREATE INDEX IF NOT EXISTS idx_carts_session_id ON carts(session_id);
CREATE INDEX IF NOT EXISTS idx_carts_status ON carts(status);
CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_product_id ON cart_items(product_id);
CREATE INDEX IF NOT EXISTS idx_cart_coupons_cart_id ON cart_coupons(cart_id);
CREATE INDEX IF NOT EXISTS idx_abandoned_user_id ON cart_abandoned_history(user_id);
CREATE INDEX IF NOT EXISTS idx_abandoned_at ON cart_abandoned_history(abandoned_at);
