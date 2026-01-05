-- V3__add_usage_table.sql
-- OCS Provisioning Service - Add Usage Table
-- Creates usage table for tracking service consumption (voice, data, SMS, MMS)

-- =====================================================
-- Table: usage_records (renamed from usage to avoid MySQL reserved keyword)
-- =====================================================
CREATE TABLE usage_records (
    usage_id VARCHAR(255) NOT NULL,
    usage_timestamp TIMESTAMP NOT NULL,
    charged_party_id VARCHAR(255) NOT NULL,
    charged_msisdn VARCHAR(50),
    a_party VARCHAR(50),
    b_party VARCHAR(255),
    usage_type VARCHAR(50) NOT NULL,
    record_type VARCHAR(50) NOT NULL,
    record_opening_time TIMESTAMP,
    record_closing_time TIMESTAMP,
    duration_seconds INTEGER,
    volume_usage BIGINT NOT NULL,
    impacted_balance_id VARCHAR(255) NOT NULL,
    balance_value_before BIGINT,
    balance_value_after BIGINT,
    offer_id VARCHAR(255),
    
    PRIMARY KEY (usage_id),
    CONSTRAINT fk_usage_subscriber 
        FOREIGN KEY (charged_party_id) 
        REFERENCES subscribers(subscriber_id) 
        ON DELETE RESTRICT,
    CONSTRAINT fk_usage_balance 
        FOREIGN KEY (impacted_balance_id) 
        REFERENCES balances(balance_id) 
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Usage Indexes (Performance)
-- =====================================================
CREATE INDEX idx_usage_subscriber ON usage_records(charged_party_id);
CREATE INDEX idx_usage_balance ON usage_records(impacted_balance_id);
CREATE INDEX idx_usage_timestamp ON usage_records(usage_timestamp);
CREATE INDEX idx_usage_type ON usage_records(usage_type);
CREATE INDEX idx_usage_msisdn ON usage_records(charged_msisdn);
CREATE INDEX idx_usage_offer ON usage_records(offer_id);
CREATE INDEX idx_usage_record_type ON usage_records(record_type);
CREATE INDEX idx_usage_composite_subscriber_timestamp ON usage_records(charged_party_id, usage_timestamp);

