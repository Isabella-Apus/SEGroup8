DROP TABLE IF EXISTS `idempotency_record`;
DROP TABLE IF EXISTS `address`;
DROP TABLE IF EXISTS `chat_message`;
DROP TABLE IF EXISTS `chat_conversation`;
DROP TABLE IF EXISTS `user_voucher`;
DROP TABLE IF EXISTS `voucher`;
DROP TABLE IF EXISTS `user_block`;
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `transaction_record`;
DROP TABLE IF EXISTS `balance`;
DROP TABLE IF EXISTS `order_after_sale_log`;
DROP TABLE IF EXISTS `logistics_trace`;
DROP TABLE IF EXISTS `logistics_path_template`;
DROP TABLE IF EXISTS `auction_log`;
DROP TABLE IF EXISTS `product_auction`;
DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `order_info`;
DROP TABLE IF EXISTS `secondhand_product`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `shop`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `user_report`;
DROP TABLE IF EXISTS `credit_score_log`;
DROP TABLE IF EXISTS `admin_audit_log`;
DROP TABLE IF EXISTS `merchant_application`;
DROP TABLE IF EXISTS `review`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50),
  `password` VARCHAR(100),
  `nickname` VARCHAR(50),
  `avatar` VARCHAR(255),
  `phone` VARCHAR(30),
  `email` VARCHAR(100),
  `role` VARCHAR(20),
  `status` VARCHAR(20),
  `credit_score` INT,
  `shop_name` VARCHAR(100),
  `shop_desc` VARCHAR(255),
  `banner_url` VARCHAR(255),
  `category` VARCHAR(100),
  `region` VARCHAR(100),
  `business_hours` VARCHAR(100),
  `return_policy` VARCHAR(255),
  `shipping_policy` VARCHAR(255),
  `announcement` VARCHAR(255),
  `seller_credit_score` INT,
  `buyer_credit_score` INT,
  `create_time` TIMESTAMP,
  `update_time` TIMESTAMP
);

CREATE TABLE `address` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `receiver_name` VARCHAR(50) NOT NULL,
  `receiver_phone` VARCHAR(20) NOT NULL,
  `province` VARCHAR(50) NOT NULL,
  `city` VARCHAR(50) NOT NULL,
  `detail_address` VARCHAR(255) NOT NULL,
  `is_default` TINYINT NOT NULL DEFAULT 0,
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `merchant_application` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `store_name` VARCHAR(80),
  `category_id` INT,
  `id_card_no` VARCHAR(30),
  `bank_card_no` VARCHAR(50),
  `license_img` VARCHAR(255),
  `warehouse_addr` VARCHAR(255),
  `warehouse_province` VARCHAR(50),
  `warehouse_city` VARCHAR(50),
  `warehouse_detail` VARCHAR(255),
  `contact_name` VARCHAR(50),
  `contact_phone` VARCHAR(20),
  `status` INT DEFAULT 0,
  `reject_reason` VARCHAR(255),
  `apply_time` TIMESTAMP
);

CREATE TABLE `admin_audit_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `admin_user_id` BIGINT,
  `admin_username` VARCHAR(50),
  `action` VARCHAR(80),
  `target_type` VARCHAR(50),
  `target_id` BIGINT,
  `detail` VARCHAR(1000),
  `create_time` TIMESTAMP
);

CREATE TABLE `credit_score_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role` VARCHAR(30),
  `delta` INT,
  `reason_code` VARCHAR(80),
  `reason_desc` VARCHAR(500),
  `ref_id` BIGINT,
  `operator_id` BIGINT,
  `create_time` TIMESTAMP
);

CREATE TABLE `user_report` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `reporter_id` BIGINT NOT NULL,
  `reported_id` BIGINT NOT NULL,
  `reporter_role` VARCHAR(30),
  `trade_context` VARCHAR(30),
  `reason_type` VARCHAR(50),
  `reason_desc` VARCHAR(500),
  `evidence_urls` VARCHAR(1000),
  `status` INT DEFAULT 0,
  `admin_id` BIGINT,
  `admin_remark` VARCHAR(500),
  `audit_time` TIMESTAMP,
  `create_time` TIMESTAMP,
  `update_time` TIMESTAMP
);

CREATE TABLE `review` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_id` BIGINT,
  `product_type` VARCHAR(20),
  `product_id` BIGINT,
  `user_id` BIGINT,
  `score` INT,
  `content` VARCHAR(1000),
  `review_type` VARCHAR(30),
  `seller_reply` VARCHAR(1000),
  `seller_reply_time` TIMESTAMP,
  `status` INT,
  `create_time` TIMESTAMP,
  `update_time` TIMESTAMP
);

CREATE TABLE `category` (
  `id` INT PRIMARY KEY,
  `name` VARCHAR(100),
  `parent_id` INT,
  `sort_order` INT,
  `status` INT
);

CREATE TABLE `order_info` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_no` VARCHAR(64),
  `buyer_user_id` BIGINT,
  `total_amount` DECIMAL(10,2),
  `pay_status` INT,
  `order_status` INT,
  `refund_status` INT,
  `refund_reason` VARCHAR(255),
  `refund_proof_urls` TEXT,
  `receiver_name` VARCHAR(50),
  `receiver_phone` VARCHAR(20),
  `receiver_province` VARCHAR(50),
  `receiver_city` VARCHAR(50),
  `receiver_detail_address` VARCHAR(255),
  `pay_method` VARCHAR(30),
  `delivery_no` VARCHAR(60),
  `remark` VARCHAR(255),
  `paid_time` TIMESTAMP,
  `shipped_time` TIMESTAMP,
  `received_time` TIMESTAMP,
  `completed_time` TIMESTAMP,
  `refund_apply_time` TIMESTAMP,
  `refund_decision_time` TIMESTAMP,
  `refund_decision_user_id` BIGINT,
  `refund_decision_remark` VARCHAR(255),
  `refund_decision_source` VARCHAR(20),
  `logistics_template_id` BIGINT,
  `logistics_status` VARCHAR(30),
  `logistics_current_index` INT,
  `can_refund` INT,
  `after_sales_deadline` TIMESTAMP,
  `delivery_time` TIMESTAMP,
  `arrival_time` TIMESTAMP,
  `auto_confirm_deadline` TIMESTAMP,
  `refund_mode` VARCHAR(30),
  `voucher_id` BIGINT,
  `voucher_discount_amount` DECIMAL(10,2),
  `seller_bear_amount` DECIMAL(10,2),
  `platform_bear_amount` DECIMAL(10,2),
  `payable_amount` DECIMAL(10,2),
  `version` INT DEFAULT 0,
  `closed_time` TIMESTAMP,
  `create_time` TIMESTAMP,
  `update_time` TIMESTAMP
);

CREATE TABLE `order_item` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_id` BIGINT,
  `product_type` VARCHAR(20),
  `product_id` BIGINT,
  `product_name` VARCHAR(100),
  `price` DECIMAL(10,2),
  `quantity` INT,
  `status` INT,
  `create_time` TIMESTAMP,
  `update_time` TIMESTAMP
);

CREATE TABLE `shop` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `owner_user_id` BIGINT,
  `name` VARCHAR(80),
  `logo` VARCHAR(255),
  `description` VARCHAR(255),
  `region` VARCHAR(100),
  `contact_name` VARCHAR(50),
  `contact_phone` VARCHAR(30),
  `id_card_no_masked` VARCHAR(50),
  `warehouse_addr` VARCHAR(255),
  `decoration_json` TEXT,
  `status` TINYINT,
  `create_time` TIMESTAMP,
  `update_time` TIMESTAMP
);

CREATE TABLE `product` (
  `id` BIGINT PRIMARY KEY,
  `shop_id` BIGINT,
  `name` VARCHAR(120),
  `cover` VARCHAR(255),
  `images` TEXT,
  `description` VARCHAR(255),
  `price` DECIMAL(10,2),
  `category_id` INT,
  `sub_category_id` INT,
  `stock` INT,
  `status` TINYINT,
  `create_time` TIMESTAMP,
  `update_time` TIMESTAMP
);

CREATE TABLE `secondhand_product` (
  `id` BIGINT PRIMARY KEY,
  `seller_user_id` BIGINT NOT NULL,
  `name` VARCHAR(120) NOT NULL,
  `cover` VARCHAR(255),
  `images` TEXT,
  `description` VARCHAR(255),
  `origin_price` DECIMAL(10,2),
  `sale_price` DECIMAL(10,2) NOT NULL,
  `category_id` INT,
  `sub_category_id` INT,
  `condition_level` VARCHAR(30),
  `is_negotiable` TINYINT DEFAULT 1,
  `status` INT NOT NULL,
  `create_time` TIMESTAMP,
  `update_time` TIMESTAMP
);

CREATE TABLE `product_auction` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `seller_user_id` BIGINT NOT NULL,
  `start_price` DECIMAL(10,2) NOT NULL,
  `increment_amount` DECIMAL(10,2) NOT NULL,
  `current_price` DECIMAL(10,2) NOT NULL,
  `current_bidder_user_id` BIGINT,
  `start_time` TIMESTAMP NOT NULL,
  `end_time` TIMESTAMP NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `settled_order_id` BIGINT,
  `version` INT DEFAULT 0,
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `auction_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `auction_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `bidder_user_id` BIGINT NOT NULL,
  `bid_amount` DECIMAL(10,2) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `order_after_sale_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `action` VARCHAR(30) NOT NULL,
  `operator_user_id` BIGINT,
  `operator_role` VARCHAR(30),
  `remark` VARCHAR(255),
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `logistics_path_template` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `origin_region` VARCHAR(50),
  `dest_region` VARCHAR(50),
  `path_nodes` TEXT,
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_logistics_template_region` (`origin_region`, `dest_region`)
);

CREATE TABLE `logistics_trace` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT,
  `node_name` VARCHAR(100),
  `status_desc` VARCHAR(255),
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `balance` (
  `user_id` BIGINT PRIMARY KEY,
  `personal_balance` DECIMAL(10,2) DEFAULT 0,
  `business_balance` DECIMAL(10,2) DEFAULT 0,
  `version` INT DEFAULT 0,
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `transaction_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT,
  `user_id` BIGINT,
  `account_type` VARCHAR(20),
  `change_type` VARCHAR(40),
  `trade_type` VARCHAR(40),
  `amount` DECIMAL(10,2),
  `balance_after` DECIMAL(10,2),
  `remark` VARCHAR(255),
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `notification` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT,
  `title` VARCHAR(100),
  `content` VARCHAR(500),
  `target_path` VARCHAR(255),
  `is_read` INT DEFAULT 0,
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `voucher` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `issuer_type` TINYINT NOT NULL DEFAULT 1,
  `voucher_type` TINYINT NOT NULL DEFAULT 1,
  `issuer_user_id` BIGINT,
  `scope_type` TINYINT NOT NULL DEFAULT 1,
  `shop_id` BIGINT,
  `product_id` BIGINT,
  `can_stack` TINYINT NOT NULL DEFAULT 0,
  `name` VARCHAR(100) NOT NULL,
  `type` TINYINT NOT NULL DEFAULT 1,
  `discount_amount` DECIMAL(10,2),
  `discount_rate` DECIMAL(4,2),
  `min_amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `total_count` INT NOT NULL DEFAULT 0,
  `received_count` INT NOT NULL DEFAULT 0,
  `used_count` INT NOT NULL DEFAULT 0,
  `grab_start_time` TIMESTAMP,
  `grab_end_time` TIMESTAMP,
  `start_time` TIMESTAMP,
  `end_time` TIMESTAMP,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `user_voucher` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `voucher_id` BIGINT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `received_time` TIMESTAMP,
  `used_order_id` BIGINT,
  `used_time` TIMESTAMP,
  `expire_time` TIMESTAMP,
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `chat_conversation` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `buyer_user_id` BIGINT NOT NULL,
  `seller_user_id` BIGINT NOT NULL,
  `source_type` VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
  `source_id` BIGINT NOT NULL DEFAULT 0,
  `source_title` VARCHAR(120),
  `last_message_content` VARCHAR(1000),
  `last_message_time` TIMESTAMP,
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `uk_chat_conversation_pair`
    UNIQUE (`buyer_user_id`, `seller_user_id`, `source_type`, `source_id`)
);

CREATE TABLE `chat_message` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `conversation_id` BIGINT NOT NULL,
  `sender_user_id` BIGINT NOT NULL,
  `receiver_user_id` BIGINT NOT NULL,
  `content` VARCHAR(1000) NOT NULL,
  `is_read` TINYINT NOT NULL DEFAULT 0,
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `user_block` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `blocker_id` BIGINT NOT NULL,
  `blocked_id` BIGINT NOT NULL,
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `uk_user_block` UNIQUE (`blocker_id`, `blocked_id`)
);

CREATE TABLE `idempotency_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT,
  `request_method` VARCHAR(10),
  `request_path` VARCHAR(255),
  `idempotency_key` VARCHAR(128),
  `status` INT,
  `http_status` INT,
  `response_body` TEXT,
  `expire_time` TIMESTAMP,
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP,
  UNIQUE (`user_id`, `request_method`, `request_path`, `idempotency_key`)
);
