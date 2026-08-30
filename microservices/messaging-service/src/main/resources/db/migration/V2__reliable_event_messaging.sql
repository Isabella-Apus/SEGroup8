CREATE UNIQUE INDEX uk_notification_event_recipient ON notification(event_id, user_id);
CREATE UNIQUE INDEX uk_notification_dedupe_key ON notification(dedupe_key);

CREATE TABLE inbox_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  payload LONGTEXT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'RECEIVED',
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_error VARCHAR(500) DEFAULT NULL,
  received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at DATETIME DEFAULT NULL,
  trace_id VARCHAR(128) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_inbox_event_id (event_id),
  KEY idx_inbox_retry (status, next_retry_at, id)
);

CREATE TABLE idempotency_record (
  dedupe_key VARCHAR(128) NOT NULL,
  service_identity VARCHAR(128) NOT NULL,
  request_hash VARCHAR(128) NOT NULL,
  notification_id BIGINT DEFAULT NULL,
  response_body LONGTEXT DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (dedupe_key)
);

CREATE TABLE outbox_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id VARCHAR(64) NOT NULL,
  source_event_id VARCHAR(64) DEFAULT NULL,
  dedupe_key VARCHAR(128) NOT NULL,
  delivery_kind VARCHAR(32) NOT NULL,
  recipient_user_id BIGINT DEFAULT NULL,
  event_type VARCHAR(64) NOT NULL,
  payload LONGTEXT NOT NULL,
  trace_id VARCHAR(128) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  retry_count INT NOT NULL DEFAULT 0,
  next_attempt_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_error VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  delivered_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_messaging_outbox_event_id (event_id),
  UNIQUE KEY uk_messaging_outbox_dedupe (dedupe_key),
  KEY idx_messaging_outbox_delivery (delivery_kind, status, next_attempt_at, id)
);
