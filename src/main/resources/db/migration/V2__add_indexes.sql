-- V2__add_indexes.sql
-- OCS Provisioning Service - Performance Indexes
-- Creates indexes for frequently queried fields and foreign keys

-- =====================================================
-- Subscriber Indexes
-- =====================================================
CREATE INDEX idx_subscriber_msisdn ON subscribers(msisdn);
CREATE INDEX idx_subscriber_imsi ON subscribers(imsi);
CREATE INDEX idx_subscriber_icc_id ON subscribers(icc_id);
CREATE INDEX idx_subscriber_state ON subscribers(current_state);
CREATE INDEX idx_subscriber_business_account ON subscribers(business_account_id);
CREATE INDEX idx_subscriber_email ON subscribers(email);
CREATE INDEX idx_subscriber_creation_date ON subscribers(creation_date);
CREATE INDEX idx_subscriber_activation_date ON subscribers(activation_date);

-- =====================================================
-- Subscription Indexes
-- =====================================================
CREATE INDEX idx_subscription_subscriber_id ON subscriptions(subscriber_id);
CREATE INDEX idx_subscription_state ON subscriptions(state);
CREATE INDEX idx_subscription_offer_id ON subscriptions(offer_id);
CREATE INDEX idx_subscription_type ON subscriptions(subscription_type);
CREATE INDEX idx_subscription_creation_date ON subscriptions(creation_date);
CREATE INDEX idx_subscription_expiration_date ON subscriptions(expiration_date);

-- =====================================================
-- Balance Indexes
-- =====================================================
CREATE INDEX idx_balance_subscription_id ON balances(subscription_id);
CREATE INDEX idx_balance_type ON balances(balance_type);
CREATE INDEX idx_balance_unit_type ON balances(unit_type);
CREATE INDEX idx_balance_effective_date ON balances(effective_date);
CREATE INDEX idx_balance_expiration_date ON balances(expiration_date);

-- =====================================================
-- Group Indexes (using backticks because `groups` is a reserved keyword)
-- =====================================================
CREATE INDEX idx_group_name ON `groups`(group_name);
CREATE INDEX idx_group_state ON `groups`(state);
CREATE INDEX idx_group_owner_subscriber_id ON `groups`(owner_subscriber_id);
CREATE INDEX idx_group_type ON `groups`(group_type);
CREATE INDEX idx_group_created_date ON `groups`(created_date);

-- =====================================================
-- Notification Address Indexes
-- =====================================================
CREATE INDEX idx_notif_type ON notification_addresses(notification_type);
CREATE INDEX idx_notif_address ON notification_addresses(notification_address);
CREATE INDEX idx_notif_created_date ON notification_addresses(created_date);

-- =====================================================
-- Timer Indexes
-- =====================================================
CREATE INDEX idx_timer_entity_id ON timers(timer_entity_id);
CREATE INDEX idx_timer_execution_date ON timers(timer_execution_date);
CREATE INDEX idx_timer_created_date ON timers(created_date);
CREATE INDEX idx_timer_name ON timers(timer_name);

-- =====================================================
-- Account History Indexes
-- =====================================================
CREATE INDEX idx_history_entity_id ON account_history(entity_id);
CREATE INDEX idx_history_entity_type ON account_history(entity_type);
CREATE INDEX idx_history_creation_date ON account_history(creation_date);
CREATE INDEX idx_history_status ON account_history(status);
CREATE INDEX idx_history_entity_composite ON account_history(entity_id, entity_type);

-- =====================================================
-- Junction Table Indexes (Foreign Key Indexes)
-- =====================================================
CREATE INDEX idx_group_members_subscriber ON group_members(subscriber_id);
CREATE INDEX idx_group_subscriptions_subscription ON group_subscriptions(subscription_id);
CREATE INDEX idx_subscriber_groups_group ON subscriber_groups(group_id);
CREATE INDEX idx_subscriber_notif_notif ON subscriber_notification_addresses(notification_address_id);
CREATE INDEX idx_group_notif_notif ON group_notification_addresses(notification_address_id);
