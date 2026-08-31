CREATE TABLE IF NOT EXISTS secondhand_product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  seller_user_id BIGINT NOT NULL,
  seller_name_snapshot VARCHAR(120),
  name VARCHAR(120) NOT NULL,
  cover VARCHAR(255),
  images TEXT NOT NULL,
  description VARCHAR(2000),
  origin_price DECIMAL(10,2) NOT NULL,
  sale_price DECIMAL(10,2) NOT NULL,
  category_id INT NOT NULL,
  sub_category_id INT NOT NULL,
  condition_level VARCHAR(30) NOT NULL,
  is_negotiable TINYINT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 2,
  risk_status VARCHAR(24) NOT NULL DEFAULT 'RISK_PENDING',
  version INT NOT NULL DEFAULT 0,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_secondhand_price CHECK (origin_price > 0 AND sale_price > 0 AND sale_price <= origin_price),
  CONSTRAINT ck_secondhand_status CHECK (status IN (1, 2, 3, 4)),
  CONSTRAINT ck_secondhand_negotiable CHECK (is_negotiable IN (0, 1))
);
CREATE INDEX idx_secondhand_public ON secondhand_product(status, risk_status, deleted, create_time);
CREATE INDEX idx_secondhand_seller ON secondhand_product(seller_user_id, deleted, create_time);
CREATE INDEX idx_secondhand_category ON secondhand_product(category_id, sub_category_id);

CREATE TABLE IF NOT EXISTS category_projection (
  category_id INT NOT NULL,
  sub_category_id INT NOT NULL,
  category_name VARCHAR(80) NOT NULL,
  sub_category_name VARCHAR(80) NOT NULL,
  active TINYINT NOT NULL DEFAULT 1,
  version BIGINT NOT NULL DEFAULT 1,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (category_id, sub_category_id)
);

CREATE TABLE IF NOT EXISTS product_negotiation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  buyer_user_id BIGINT NOT NULL,
  seller_user_id BIGINT NOT NULL,
  conversation_id BIGINT,
  proposed_price DECIMAL(10,2) NOT NULL,
  confirmed_price DECIMAL(10,2),
  status VARCHAR(20) NOT NULL,
  effective_from TIMESTAMP,
  effective_until TIMESTAMP,
  used_order_id BIGINT,
  version INT NOT NULL DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_negotiation_product FOREIGN KEY (product_id) REFERENCES secondhand_product(id),
  CONSTRAINT ck_negotiation_status CHECK (status IN ('PENDING','ACCEPTING','ACCEPTED','REJECTED','FAILED'))
);
CREATE INDEX idx_negotiation_product ON product_negotiation(product_id, status);
CREATE INDEX idx_negotiation_buyer ON product_negotiation(buyer_user_id, status);
CREATE INDEX idx_negotiation_seller ON product_negotiation(seller_user_id, status);

CREATE TABLE IF NOT EXISTS product_auction (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  seller_user_id BIGINT NOT NULL,
  start_price DECIMAL(10,2) NOT NULL,
  increment_amount DECIMAL(10,2) NOT NULL,
  current_price DECIMAL(10,2) NOT NULL,
  current_bidder_user_id BIGINT,
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP NOT NULL,
  status VARCHAR(20) NOT NULL,
  settled_order_id BIGINT,
  version INT NOT NULL DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_auction_product FOREIGN KEY (product_id) REFERENCES secondhand_product(id),
  CONSTRAINT ck_auction_status CHECK (status IN ('ONGOING','SETTLING','FINISHED','FLOW'))
);
CREATE INDEX idx_auction_product ON product_auction(product_id, status);
CREATE INDEX idx_auction_seller ON product_auction(seller_user_id, status);
CREATE INDEX idx_auction_due ON product_auction(status, end_time);

CREATE TABLE IF NOT EXISTS auction_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  auction_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  bidder_user_id BIGINT NOT NULL,
  bidder_name_snapshot VARCHAR(120),
  bid_amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'LEADING',
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_bid_auction FOREIGN KEY (auction_id) REFERENCES product_auction(id)
);
CREATE INDEX idx_bid_auction ON auction_log(auction_id, create_time);
CREATE INDEX idx_bid_bidder ON auction_log(bidder_user_id);

CREATE TABLE IF NOT EXISTS trade_order_request (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  trade_type VARCHAR(24) NOT NULL,
  trade_id VARCHAR(80) NOT NULL,
  order_business_key VARCHAR(120) NOT NULL,
  product_id BIGINT NOT NULL,
  buyer_user_id BIGINT NOT NULL,
  seller_user_id BIGINT NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  address_id BIGINT,
  remark VARCHAR(255),
  request_status VARCHAR(24) NOT NULL,
  order_id BIGINT,
  order_no VARCHAR(80),
  order_status VARCHAR(32),
  attempts INT NOT NULL DEFAULT 0,
  last_error VARCHAR(500),
  next_retry_at TIMESTAMP,
  version INT NOT NULL DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_trade_request UNIQUE (trade_type, trade_id),
  CONSTRAINT uk_order_business_key UNIQUE (order_business_key),
  CONSTRAINT ck_trade_request_status CHECK (request_status IN ('PENDING','RETRY','CREATED','FAILED','CANCELLED'))
);
CREATE INDEX idx_trade_retry ON trade_order_request(request_status, next_retry_at);
CREATE INDEX idx_trade_product ON trade_order_request(product_id);

CREATE TABLE IF NOT EXISTS idempotency_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  scope_name VARCHAR(80) NOT NULL,
  idempotency_key VARCHAR(160) NOT NULL,
  response_reference VARCHAR(160),
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_idempotency UNIQUE (scope_name, idempotency_key)
);

CREATE TABLE IF NOT EXISTS outbox_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  event_id VARCHAR(64) NOT NULL,
  aggregate_type VARCHAR(64) NOT NULL,
  aggregate_id VARCHAR(80) NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  payload TEXT NOT NULL,
  event_status VARCHAR(20) NOT NULL DEFAULT 'NEW',
  attempts INT NOT NULL DEFAULT 0,
  available_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  published_at TIMESTAMP,
  CONSTRAINT uk_outbox_event UNIQUE (event_id)
);
CREATE INDEX idx_outbox_pending ON outbox_event(event_status, available_at);

INSERT INTO category_projection(category_id, sub_category_id, category_name, sub_category_name, active)
VALUES (8, 801, '教材书籍', '教材', 1), (8, 802, '教材书籍', '课外书', 1),
       (9, 901, '数码闲置', '电脑配件', 1), (9, 902, '数码闲置', '耳机音响', 1),
       (10, 1001, '宿舍生活', '收纳用品', 1), (11, 1101, '运动器材', '球类器材', 1);
