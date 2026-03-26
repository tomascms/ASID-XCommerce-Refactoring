-- Payment Service Database Schema Migration
-- Migration: V1__init_payment_schema.sql
-- Purpose: Initialize payment processing and transaction tables

CREATE TABLE IF NOT EXISTS payments (
    id SERIAL PRIMARY KEY,
    payment_number VARCHAR(100) NOT NULL UNIQUE,
    order_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    payment_method VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50) DEFAULT 'PENDING',
    transaction_id VARCHAR(255),
    processor_reference VARCHAR(255),
    processor_message TEXT,
    authorization_code VARCHAR(100),
    billing_address JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT chk_amount CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS payment_methods (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    method_type VARCHAR(50) NOT NULL,
    card_token VARCHAR(255),
    last_four VARCHAR(4),
    card_holder_name VARCHAR(255),
    expiry_month INTEGER,
    expiry_year INTEGER,
    is_default BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_expiry_month CHECK (expiry_month >= 1 AND expiry_month <= 12)
);

CREATE TABLE IF NOT EXISTS payment_history (
    id SERIAL PRIMARY KEY,
    payment_id INTEGER NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    status_reason VARCHAR(500),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS payment_refunds (
    id SERIAL PRIMARY KEY,
    payment_id INTEGER NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    refund_number VARCHAR(100) NOT NULL UNIQUE,
    refund_amount NUMERIC(10, 2) NOT NULL,
    refund_reason VARCHAR(255),
    refund_status VARCHAR(50) DEFAULT 'PENDING',
    processor_refund_id VARCHAR(255),
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    CONSTRAINT chk_refund_amount CHECK (refund_amount > 0)
);

CREATE TABLE IF NOT EXISTS payment_disputes (
    id SERIAL PRIMARY KEY,
    payment_id INTEGER NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    dispute_reason VARCHAR(255) NOT NULL,
    dispute_amount NUMERIC(10, 2),
    dispute_status VARCHAR(50) DEFAULT 'OPEN',
    disputed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolution_notes TEXT
);

CREATE TABLE IF NOT EXISTS payment_failure_logs (
    id SERIAL PRIMARY KEY,
    payment_id INTEGER NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    failure_code VARCHAR(50),
    failure_message TEXT,
    retry_count INTEGER DEFAULT 0,
    next_retry_at TIMESTAMP,
    failed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment_webhooks (
    id SERIAL PRIMARY KEY,
    payment_id INTEGER NOT NULL,
    webhook_event_type VARCHAR(100) NOT NULL,
    webhook_payload JSONB,
    webhook_status VARCHAR(50) DEFAULT 'RECEIVED',
    processed_at TIMESTAMP,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments(user_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(payment_status);
CREATE INDEX IF NOT EXISTS idx_payments_number ON payments(payment_number);
CREATE INDEX IF NOT EXISTS idx_payment_methods_user_id ON payment_methods(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_methods_active ON payment_methods(is_active);
CREATE INDEX IF NOT EXISTS idx_payment_history_payment_id ON payment_history(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_refunds_payment_id ON payment_refunds(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_refunds_status ON payment_refunds(refund_status);
CREATE INDEX IF NOT EXISTS idx_payment_disputes_payment_id ON payment_disputes(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_failure_logs_payment_id ON payment_failure_logs(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_webhooks_payment_id ON payment_webhooks(payment_id);
