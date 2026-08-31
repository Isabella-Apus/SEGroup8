CREATE TABLE voucher (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  issuer_type VARCHAR(16) NOT NULL,
  voucher_type VARCHAR(16) NOT NULL,
  issuer_user_id BIGINT NOT NULL,
  scope_type VARCHAR(16) NOT NULL,
  shop_id BIGINT NULL,
  product_id BIGINT NULL,
  can_stack BOOLEAN NOT NULL DEFAULT FALSE,
  name VARCHAR(100) NOT NULL,
  discount_type VARCHAR(16) NOT NULL,
  discount_amount DECIMAL(19,2) NULL,
  discount_rate DECIMAL(8,4) NULL,
  min_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
  total_count INT NOT NULL,
  received_count INT NOT NULL DEFAULT 0,
  used_count INT NOT NULL DEFAULT 0,
  grab_start_time TIMESTAMP NULL,
  grab_end_time TIMESTAMP NULL,
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP NOT NULL,
  status VARCHAR(16) NOT NULL,
  version INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_voucher_amount CHECK (min_amount >= 0 AND total_count >= 0 AND received_count >= 0 AND used_count >= 0),
  CONSTRAINT ck_voucher_discount CHECK ((discount_type='AMOUNT' AND discount_amount > 0) OR (discount_type='RATE' AND discount_rate > 0 AND discount_rate < 1))
);
CREATE INDEX idx_voucher_shop_status ON voucher(shop_id, status);

CREATE TABLE user_voucher (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  voucher_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL,
  order_request_id VARCHAR(80) NULL,
  reserved_until TIMESTAMP NULL,
  received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  used_order_id BIGINT NULL,
  used_at TIMESTAMP NULL,
  expires_at TIMESTAMP NOT NULL,
  version INT NOT NULL DEFAULT 0,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_user_voucher UNIQUE(user_id, voucher_id),
  CONSTRAINT fk_user_voucher_voucher FOREIGN KEY(voucher_id) REFERENCES voucher(id)
);
CREATE UNIQUE INDEX uk_voucher_order_request ON user_voucher(order_request_id);
CREATE INDEX idx_user_voucher_user_status ON user_voucher(user_id, status);

CREATE TABLE balance (
  user_id BIGINT PRIMARY KEY,
  personal_balance DECIMAL(19,2) NOT NULL DEFAULT 0,
  business_balance DECIMAL(19,2) NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_balance_non_negative CHECK(personal_balance >= 0 AND business_balance >= 0)
);

CREATE TABLE transaction_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  transaction_id VARCHAR(80) NOT NULL,
  business_request_id VARCHAR(80) NOT NULL,
  order_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  account_type VARCHAR(16) NOT NULL,
  trade_type VARCHAR(24) NOT NULL,
  amount DECIMAL(19,2) NOT NULL,
  balance_after DECIMAL(19,2) NOT NULL,
  currency CHAR(3) NOT NULL,
  reversal_of VARCHAR(80) NULL,
  remark VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_transaction_id UNIQUE(transaction_id),
  CONSTRAINT uk_transaction_business_request UNIQUE(business_request_id)
);
CREATE INDEX idx_transaction_user_account ON transaction_record(user_id, account_type, created_at);
CREATE INDEX idx_transaction_order ON transaction_record(order_id);

CREATE TABLE checkout_quote (
  quote_id VARCHAR(80) PRIMARY KEY,
  quote_version INT NOT NULL,
  order_request_id VARCHAR(80) NOT NULL,
  user_id BIGINT NOT NULL,
  voucher_id BIGINT NULL,
  original_amount DECIMAL(19,2) NOT NULL,
  discount_amount DECIMAL(19,2) NOT NULL,
  payable_amount DECIMAL(19,2) NOT NULL,
  currency CHAR(3) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_checkout_order_request UNIQUE(order_request_id)
);

CREATE TABLE payment_request (
  request_id VARCHAR(80) PRIMARY KEY,
  request_type VARCHAR(16) NOT NULL,
  order_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  seller_id BIGINT NULL,
  amount DECIMAL(19,2) NOT NULL,
  currency CHAR(3) NOT NULL,
  status VARCHAR(16) NOT NULL,
  transaction_id VARCHAR(80) NULL,
  original_request_id VARCHAR(80) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP NULL
);
CREATE UNIQUE INDEX uk_settlement_order_seller ON payment_request(request_type, order_id, seller_id);

CREATE TABLE idempotency_record (
  scope VARCHAR(40) NOT NULL,
  request_key VARCHAR(80) NOT NULL,
  response_body TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NULL,
  PRIMARY KEY(scope, request_key)
);

CREATE TABLE outbox_event (
  event_id VARCHAR(80) PRIMARY KEY,
  aggregate_type VARCHAR(40) NOT NULL,
  aggregate_id VARCHAR(80) NOT NULL,
  event_type VARCHAR(60) NOT NULL,
  payload TEXT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  attempts INT NOT NULL DEFAULT 0,
  available_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  locked_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  published_at TIMESTAMP NULL
);
CREATE INDEX idx_outbox_delivery ON outbox_event(status, available_at);
