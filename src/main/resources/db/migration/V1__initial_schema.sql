-- V1__initial_schema.sql
-- OCS Provisioning Service - Initial Database Schema
-- Creates all 7 core entities: Subscriber, Subscription, Balance, Group, NotificationAddress, Timer, AccountHistory

-- =====================================================
-- Table: subscribers
-- =====================================================
CREATE TABLE subscribers (
    subscriber_id VARCHAR(255) NOT NULL,
    business_account_id VARCHAR(255),
    msisdn VARCHAR(50),
    imsi VARCHAR(50),
    icc_id VARCHAR(50),
    current_state VARCHAR(50),
    previous_state VARCHAR(50),
    creation_date TIMESTAMP,
    last_transition_date TIMESTAMP,
    activation_date TIMESTAMP,
    expiration_date TIMESTAMP,
    language_id VARCHAR(50),
    carrier_id VARCHAR(50),
    subscriber_type VARCHAR(50),
    
    -- Personal Info (embedded object)
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    date_of_birth DATE,
    email VARCHAR(255),
    contact_number VARCHAR(50),
    
    -- Billing (embedded object)
    billing_cycle VARCHAR(50),
    billcycle_day INTEGER,
    billing_street VARCHAR(255),
    billing_city VARCHAR(100),
    billing_state VARCHAR(100),
    billing_zip_code VARCHAR(20),
    billing_country VARCHAR(100),
    
    -- Services (embedded object)
    service_voice BOOLEAN DEFAULT FALSE,
    service_sms BOOLEAN DEFAULT FALSE,
    service_mms BOOLEAN DEFAULT FALSE,
    service_data BOOLEAN DEFAULT FALSE,
    service_roaming BOOLEAN DEFAULT FALSE,
    value_added_services JSON,
    
    -- Metadata
    custom_fields JSON,
    last_modified_date TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    PRIMARY KEY (subscriber_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Table: subscriptions
-- =====================================================
CREATE TABLE subscriptions (
    subscription_id VARCHAR(255) NOT NULL,
    subscriber_id VARCHAR(255),
    subscription_type VARCHAR(100),
    offer_id VARCHAR(255),
    offer_name VARCHAR(255),
    state VARCHAR(50),
    creation_date TIMESTAMP,
    activation_date TIMESTAMP,
    expiration_date TIMESTAMP,
    renewal_date TIMESTAMP,
    recurring BOOLEAN DEFAULT FALSE,
    paid_flag BOOLEAN DEFAULT FALSE,
    is_group BOOLEAN DEFAULT FALSE,
    max_recurring_cycles INTEGER,
    recurring_cycles_completed INTEGER,
    cycle_length_units INTEGER,
    cycle_length_type VARCHAR(50),
    custom_parameters JSON,
    last_modified_date TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    PRIMARY KEY (subscription_id),
    CONSTRAINT fk_subscription_subscriber 
        FOREIGN KEY (subscriber_id) 
        REFERENCES subscribers(subscriber_id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Table: balances
-- =====================================================
CREATE TABLE balances (
    balance_id VARCHAR(255) NOT NULL,
    subscription_id VARCHAR(255),
    effective_date TIMESTAMP,
    expiration_date TIMESTAMP,
    creation_date TIMESTAMP,
    last_modified_date TIMESTAMP,
    balance_type VARCHAR(50),
    unit_type VARCHAR(50),
    balance_amount DECIMAL(20, 6),
    balance_available DECIMAL(20, 6),
    is_group_balance BOOLEAN DEFAULT FALSE,
    is_recurring BOOLEAN DEFAULT FALSE,
    cycle_length_type VARCHAR(50),
    cycle_length_units INTEGER,
    max_recurring_cycles INTEGER,
    recurring_cycles_completed INTEGER,
    max_rollover_amount DECIMAL(20, 6),
    rollover_amount DECIMAL(20, 6),
    is_rollover_allowed BOOLEAN DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    
    PRIMARY KEY (balance_id),
    CONSTRAINT fk_balance_subscription 
        FOREIGN KEY (subscription_id) 
        REFERENCES subscriptions(subscription_id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Table: groups (using backticks because `groups` is a reserved keyword)
-- =====================================================
CREATE TABLE `groups` (
    group_id VARCHAR(255) NOT NULL,
    group_name VARCHAR(255),
    group_type VARCHAR(100),
    state VARCHAR(50),
    created_date TIMESTAMP,
    modified_date TIMESTAMP,
    expiration_date TIMESTAMP,
    
    -- Group Owner (embedded object)
    owner_subscriber_id VARCHAR(255),
    owner_role VARCHAR(50),
    owner_member_since TIMESTAMP,
    
    -- Settings (embedded object)
    max_members INTEGER,
    auto_renew BOOLEAN DEFAULT FALSE,
    quota_distribution VARCHAR(100),
    
    -- Metadata
    custom_fields JSON,
    last_modified_date TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    PRIMARY KEY (group_id),
    CONSTRAINT fk_group_owner 
        FOREIGN KEY (owner_subscriber_id) 
        REFERENCES subscribers(subscriber_id) 
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Table: notification_addresses
-- =====================================================
CREATE TABLE notification_addresses (
    notification_address_id VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50),
    notification_address VARCHAR(255),
    last_notification_date TIMESTAMP,
    created_date TIMESTAMP,
    modified_date TIMESTAMP,
    expiration_date TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    PRIMARY KEY (notification_address_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Table: timers
-- =====================================================
CREATE TABLE timers (
    timer_id VARCHAR(255) NOT NULL,
    timer_entity_id VARCHAR(255),
    timer_name VARCHAR(255),
    timer_execution_relative_period INTEGER,
    timer_execution_date TIMESTAMP,
    created_date TIMESTAMP,
    modified_date TIMESTAMP,
    expiration_date TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    PRIMARY KEY (timer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Table: account_history
-- =====================================================
CREATE TABLE account_history (
    interaction_id VARCHAR(255) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    description TEXT,
    direction VARCHAR(50),
    reason VARCHAR(255),
    status VARCHAR(50),
    status_change_date TIMESTAMP,
    
    -- Attachment (embedded object)
    attachment_id VARCHAR(255),
    attachment_url VARCHAR(1000),
    attachment_type VARCHAR(100),
    
    channel VARCHAR(100),
    start_date_time TIMESTAMP,
    end_date_time TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    PRIMARY KEY (interaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Junction Tables for Many-to-Many Relationships
-- =====================================================

-- Group Members (Group <-> Subscriber)
CREATE TABLE group_members (
    group_id VARCHAR(255) NOT NULL,
    subscriber_id VARCHAR(255) NOT NULL,
    
    PRIMARY KEY (group_id, subscriber_id),
    CONSTRAINT fk_group_members_group 
        FOREIGN KEY (group_id) 
        REFERENCES `groups`(group_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_group_members_subscriber 
        FOREIGN KEY (subscriber_id) 
        REFERENCES subscribers(subscriber_id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Group Subscriptions (Group <-> Subscription)
CREATE TABLE group_subscriptions (
    group_id VARCHAR(255) NOT NULL,
    subscription_id VARCHAR(255) NOT NULL,
    
    PRIMARY KEY (group_id, subscription_id),
    CONSTRAINT fk_group_subscriptions_group 
        FOREIGN KEY (group_id) 
        REFERENCES `groups`(group_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_group_subscriptions_subscription 
        FOREIGN KEY (subscription_id) 
        REFERENCES subscriptions(subscription_id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Subscriber Groups (denormalized for queries - Subscriber -> Groups)
CREATE TABLE subscriber_groups (
    subscriber_id VARCHAR(255) NOT NULL,
    group_id VARCHAR(255) NOT NULL,
    
    PRIMARY KEY (subscriber_id, group_id),
    CONSTRAINT fk_subscriber_groups_subscriber 
        FOREIGN KEY (subscriber_id) 
        REFERENCES subscribers(subscriber_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_subscriber_groups_group 
        FOREIGN KEY (group_id) 
        REFERENCES `groups`(group_id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Subscriber Notification Addresses
CREATE TABLE subscriber_notification_addresses (
    subscriber_id VARCHAR(255) NOT NULL,
    notification_address_id VARCHAR(255) NOT NULL,
    
    PRIMARY KEY (subscriber_id, notification_address_id),
    CONSTRAINT fk_subscriber_notif_subscriber 
        FOREIGN KEY (subscriber_id) 
        REFERENCES subscribers(subscriber_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_subscriber_notif_address 
        FOREIGN KEY (notification_address_id) 
        REFERENCES notification_addresses(notification_address_id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Group Notification Addresses
CREATE TABLE group_notification_addresses (
    group_id VARCHAR(255) NOT NULL,
    notification_address_id VARCHAR(255) NOT NULL,
    
    PRIMARY KEY (group_id, notification_address_id),
    CONSTRAINT fk_group_notif_group 
        FOREIGN KEY (group_id) 
        REFERENCES `groups`(group_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_group_notif_address 
        FOREIGN KEY (notification_address_id) 
        REFERENCES notification_addresses(notification_address_id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
