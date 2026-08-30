CREATE TABLE order_info (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(64) NOT NULL,
  business_key VARCHAR(160),
  buyer_user_id BIGINT NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  payable_amount DECIMAL(12,2) NOT NULL,
  pay_status VARCHAR(24) NOT NULL DEFAULT 'UNPAID',
  order_status VARCHAR(32) NOT NULL,
  refund_status VARCHAR(32) NOT NULL DEFAULT 'NONE',
  refund_reason VARCHAR(255),
  refund_proof_urls TEXT,
  receiver_name VARCHAR(50) NOT NULL,
  receiver_phone VARCHAR(20) NOT NULL,
  receiver_province VARCHAR(50) NOT NULL,
  receiver_city VARCHAR(50) NOT NULL,
  receiver_detail_address VARCHAR(255) NOT NULL,
  pay_method VARCHAR(30),
  delivery_no VARCHAR(60),
  remark VARCHAR(255),
  voucher_id BIGINT,
  voucher_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  seller_bear_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  platform_bear_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  reservation_id VARCHAR(96),
  payment_request_id VARCHAR(96),
  refund_request_id VARCHAR(96),
  paid_time TIMESTAMP NULL,
  shipped_time TIMESTAMP NULL,
  received_time TIMESTAMP NULL,
  completed_time TIMESTAMP NULL,
  closed_time TIMESTAMP NULL,
  version INT NOT NULL DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_order_no UNIQUE(order_no),
  CONSTRAINT uk_order_business_key UNIQUE(business_key)
);
CREATE INDEX idx_order_buyer_status ON order_info(buyer_user_id, order_status, create_time);

CREATE TABLE order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_type VARCHAR(20) NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(120) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  quantity INT NOT NULL,
  seller_user_id BIGINT,
  shop_id BIGINT,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_order_item_order FOREIGN KEY(order_id) REFERENCES order_info(id)
);
CREATE INDEX idx_order_item_order ON order_item(order_id);

CREATE TABLE order_after_sale_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  action VARCHAR(30) NOT NULL,
  operator_user_id BIGINT,
  operator_role VARCHAR(30),
  remark VARCHAR(255),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE review (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_type VARCHAR(20) NOT NULL,
  product_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  score TINYINT NOT NULL,
  content VARCHAR(500) NOT NULL,
  review_type VARCHAR(20) NOT NULL DEFAULT 'ORIGINAL',
  seller_reply VARCHAR(500),
  seller_reply_time TIMESTAMP NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_review_once UNIQUE(order_id, product_type, product_id, review_type)
);

CREATE TABLE logistics_path_template (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  origin_region VARCHAR(50) NOT NULL,
  dest_region VARCHAR(50) NOT NULL,
  path_nodes TEXT NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_logistics_template UNIQUE(origin_region, dest_region)
);

CREATE TABLE logistics_trace (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  node_name VARCHAR(80) NOT NULL,
  status_desc VARCHAR(120) NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_logistics_trace_order ON logistics_trace(order_id, create_time);

CREATE TABLE idempotency_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  operation_scope VARCHAR(64) NOT NULL,
  idempotency_key VARCHAR(160) NOT NULL,
  request_hash VARCHAR(64) NOT NULL,
  resource_id BIGINT,
  result_code VARCHAR(64) NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_idempotency_scope_key UNIQUE(operation_scope, idempotency_key)
);

CREATE TABLE order_saga (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  saga_id VARCHAR(96) NOT NULL,
  order_id BIGINT,
  saga_type VARCHAR(40) NOT NULL,
  state VARCHAR(40) NOT NULL,
  failed_step VARCHAR(80),
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_time TIMESTAMP NULL,
  last_error VARCHAR(500),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_order_saga_id UNIQUE(saga_id)
);

CREATE TABLE outbox_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  event_id VARCHAR(96) NOT NULL,
  aggregate_type VARCHAR(40) NOT NULL,
  aggregate_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(80) NOT NULL,
  payload TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  attempts INT NOT NULL DEFAULT 0,
  available_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  published_time TIMESTAMP NULL,
  CONSTRAINT uk_outbox_event_id UNIQUE(event_id)
);
CREATE INDEX idx_outbox_delivery ON outbox_event(status, available_at);
