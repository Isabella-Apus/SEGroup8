DROP TABLE IF EXISTS `order_after_sale_log`;
DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `order_info`;
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
  `create_time` TIMESTAMP,
  `update_time` TIMESTAMP
);

CREATE TABLE `order_info` (
  `id` BIGINT PRIMARY KEY,
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

CREATE TABLE `order_after_sale_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `action` VARCHAR(30) NOT NULL,
  `operator_user_id` BIGINT,
  `operator_role` VARCHAR(30),
  `remark` VARCHAR(255),
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
  `update_time` TIMESTAMP
);

