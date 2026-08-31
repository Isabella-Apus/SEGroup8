-- MS-06 V2 operator-run, non-destructive producer schema upgrade.
-- Run against the existing segroup8_platform schema before enabling the relay.
-- This script creates no Messaging tables and performs no DROP/TRUNCATE.
CREATE TABLE IF NOT EXISTS `outbox_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `event_id` VARCHAR(64) NOT NULL,
  `event_type` VARCHAR(64) NOT NULL,
  `aggregate_type` VARCHAR(64) NOT NULL,
  `aggregate_id` VARCHAR(128) NOT NULL,
  `payload` LONGTEXT NOT NULL,
  `trace_id` VARCHAR(128) NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  `retry_count` INT NOT NULL DEFAULT 0,
  `next_attempt_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `published_at` DATETIME DEFAULT NULL,
  `last_error` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_producer_outbox_event_id` (`event_id`),
  KEY `idx_producer_outbox_delivery` (`status`, `next_attempt_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
