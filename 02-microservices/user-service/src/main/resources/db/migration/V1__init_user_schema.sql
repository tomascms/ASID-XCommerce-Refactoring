-- User Service Database Schema Migration
-- Migration: V1__init_user_schema.sql
-- Purpose: Initialize user profiles and preferences tables

CREATE TABLE IF NOT EXISTS user_profiles (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(20),
    profile_image_url VARCHAR(500),
    bio TEXT,
    preferred_language VARCHAR(10) DEFAULT 'en',
    timezone VARCHAR(50),
    account_status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_addresses (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    address_type VARCHAR(50) NOT NULL,
    street_address VARCHAR(255) NOT NULL,
    apartment_unit VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    state_province VARCHAR(100),
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    is_default BOOLEAN DEFAULT false,
    is_billing BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_preferences (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE,
    newsletter_opt_in BOOLEAN DEFAULT true,
    promotional_emails BOOLEAN DEFAULT true,
    order_notifications BOOLEAN DEFAULT true,
    reset_password_token VARCHAR(255),
    token_expiry TIMESTAMP,
    two_factor_enabled BOOLEAN DEFAULT false,
    preferred_payment_method VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_loyalty (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE,
    loyalty_points INTEGER DEFAULT 0,
    total_spent NUMERIC(12, 2) DEFAULT 0,
    membership_tier VARCHAR(50) DEFAULT 'BRONZE',
    tier_since TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_purchase_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_loyalty_points CHECK (loyalty_points >= 0)
);

CREATE TABLE IF NOT EXISTS user_activity (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    activity_type VARCHAR(100) NOT NULL,
    activity_description TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_login_history (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP,
    ip_address VARCHAR(45),
    device_info VARCHAR(255),
    success BOOLEAN DEFAULT true,
    failure_reason VARCHAR(255)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_addresses_user_id ON user_addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_user_preferences_user_id ON user_preferences(user_id);
CREATE INDEX IF NOT EXISTS idx_user_loyalty_user_id ON user_loyalty(user_id);
CREATE INDEX IF NOT EXISTS idx_user_loyalty_tier ON user_loyalty(membership_tier);
CREATE INDEX IF NOT EXISTS idx_user_activity_user_id ON user_activity(user_id);
CREATE INDEX IF NOT EXISTS idx_user_activity_type ON user_activity(activity_type);
CREATE INDEX IF NOT EXISTS idx_user_login_history_user_id ON user_login_history(user_id);
CREATE INDEX IF NOT EXISTS idx_user_login_history_login_time ON user_login_history(login_time);
