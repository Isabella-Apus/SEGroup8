CREATE TABLE chat_conversation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  buyer_user_id BIGINT NOT NULL,
  seller_user_id BIGINT NOT NULL,
  buyer_display_name VARCHAR(80) DEFAULT NULL,
  buyer_avatar_url VARCHAR(255) DEFAULT NULL,
  buyer_role VARCHAR(32) DEFAULT NULL,
  seller_display_name VARCHAR(80) DEFAULT NULL,
  seller_avatar_url VARCHAR(255) DEFAULT NULL,
  seller_role VARCHAR(32) DEFAULT NULL,
  source_type VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
  source_id BIGINT NOT NULL DEFAULT 0,
  source_title VARCHAR(120) DEFAULT NULL,
  last_message_content VARCHAR(1000) DEFAULT NULL,
  last_message_time DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_chat_conversation_pair (buyer_user_id, seller_user_id, source_type, source_id),
  KEY idx_chat_conversation_buyer (buyer_user_id),
  KEY idx_chat_conversation_seller (seller_user_id),
  KEY idx_chat_conversation_last_message_time (last_message_time)
);

CREATE TABLE chat_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  sender_user_id BIGINT NOT NULL,
  receiver_user_id BIGINT NOT NULL,
  content VARCHAR(1000) NOT NULL,
  is_read TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_chat_message_conversation (conversation_id, create_time),
  KEY idx_chat_message_receiver (receiver_user_id, is_read),
  CONSTRAINT fk_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversation(id)
);

CREATE TABLE notification (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  content VARCHAR(500) NOT NULL,
  target_path VARCHAR(255) DEFAULT NULL,
  scope VARCHAR(16) NOT NULL DEFAULT 'buyer',
  is_read TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  notification_type VARCHAR(64) DEFAULT NULL,
  business_type VARCHAR(64) DEFAULT NULL,
  business_id VARCHAR(128) DEFAULT NULL,
  event_id VARCHAR(128) DEFAULT NULL,
  dedupe_key VARCHAR(128) DEFAULT NULL,
  trace_id VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_notification_user_read_time (user_id, is_read, create_time)
);

CREATE TABLE user_access_projection (
  user_id BIGINT NOT NULL,
  access_status VARCHAR(32) NOT NULL,
  role VARCHAR(32) DEFAULT NULL,
  display_name VARCHAR(80) DEFAULT NULL,
  avatar_url VARCHAR(255) DEFAULT NULL,
  source_version BIGINT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id)
);

CREATE TABLE user_block_projection (
  blocker_user_id BIGINT NOT NULL,
  blocked_user_id BIGINT NOT NULL,
  active TINYINT NOT NULL,
  source_version BIGINT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (blocker_user_id, blocked_user_id),
  KEY idx_block_projection_blocked (blocked_user_id, blocker_user_id)
);
