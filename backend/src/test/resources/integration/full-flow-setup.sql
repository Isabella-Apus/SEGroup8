CREATE TABLE IF NOT EXISTS `shop` (
  `id` BIGINT PRIMARY KEY,
  `owner_user_id` BIGINT,
  `name` VARCHAR(80),
  `status` TINYINT,
  `create_time` TIMESTAMP,
  `update_time` TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `product` (
  `id` BIGINT PRIMARY KEY,
  `shop_id` BIGINT,
  `name` VARCHAR(120),
  `cover` VARCHAR(255),
  `description` VARCHAR(255),
  `price` DECIMAL(10,2),
  `stock` INT,
  `status` TINYINT,
  `create_time` TIMESTAMP,
  `update_time` TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `secondhand_product` (
  `id` BIGINT PRIMARY KEY,
  `seller_user_id` BIGINT,
  `name` VARCHAR(120),
  `cover` VARCHAR(255),
  `description` VARCHAR(255),
  `origin_price` DECIMAL(10,2),
  `sale_price` DECIMAL(10,2),
  `condition_level` VARCHAR(30),
  `status` TINYINT,
  `create_time` TIMESTAMP,
  `update_time` TIMESTAMP
);

ALTER TABLE `product` ADD COLUMN IF NOT EXISTS `cover` VARCHAR(255);
ALTER TABLE `product` ADD COLUMN IF NOT EXISTS `description` VARCHAR(255);
ALTER TABLE `secondhand_product` ADD COLUMN IF NOT EXISTS `cover` VARCHAR(255);
ALTER TABLE `secondhand_product` ADD COLUMN IF NOT EXISTS `description` VARCHAR(255);
ALTER TABLE `secondhand_product` ADD COLUMN IF NOT EXISTS `origin_price` DECIMAL(10,2);

CREATE TABLE IF NOT EXISTS `balance` (
  `user_id` BIGINT PRIMARY KEY,
  `personal_balance` DECIMAL(10,2) DEFAULT 0,
  `business_balance` DECIMAL(10,2) DEFAULT 0,
  `version` INT DEFAULT 0,
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `transaction_record` (
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

ALTER TABLE `order_info` ADD COLUMN IF NOT EXISTS `logistics_template_id` BIGINT;
ALTER TABLE `order_info` ADD COLUMN IF NOT EXISTS `logistics_status` VARCHAR(20);
ALTER TABLE `order_info` ADD COLUMN IF NOT EXISTS `logistics_current_index` INT;
ALTER TABLE `order_info` ADD COLUMN IF NOT EXISTS `can_refund` INT DEFAULT 1;
ALTER TABLE `order_info` ADD COLUMN IF NOT EXISTS `after_sales_deadline` TIMESTAMP;
ALTER TABLE `order_info` ADD COLUMN IF NOT EXISTS `delivery_time` TIMESTAMP;
ALTER TABLE `order_info` ADD COLUMN IF NOT EXISTS `arrival_time` TIMESTAMP;
ALTER TABLE `order_info` ADD COLUMN IF NOT EXISTS `auto_confirm_deadline` TIMESTAMP;
ALTER TABLE `order_info` ADD COLUMN IF NOT EXISTS `refund_mode` VARCHAR(30);

DELETE FROM `transaction_record`;
DELETE FROM `balance`;
DELETE FROM `order_item` WHERE `order_id` IN (301, 302, 303);
DELETE FROM `order_info` WHERE `id` IN (301, 302, 303);
DELETE FROM `product` WHERE `id` IN (401, 402, 403);
DELETE FROM `shop` WHERE `id` IN (501, 502);
DELETE FROM `user` WHERE `id` IN (10, 11);

INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `status`, `create_time`, `update_time`)
VALUES
  (10, 'sellerA', 'x', '卖家A', 'OFFICIAL_SELLER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (11, 'sellerB', 'x', '卖家B', 'OFFICIAL_SELLER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `shop` (`id`, `owner_user_id`, `name`, `status`, `create_time`, `update_time`)
VALUES
  (501, 10, '商家A店铺', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (502, 11, '商家B店铺', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `product` (`id`, `shop_id`, `name`, `price`, `stock`, `status`, `create_time`, `update_time`)
VALUES
  (401, 501, '自动确认测试商品', 60.00, 100, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (402, 501, '仅退款测试商品', 50.00, 100, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (403, 502, '超时退款测试商品', 70.00, 100, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `balance` (`user_id`, `personal_balance`, `business_balance`, `version`, `create_time`, `update_time`)
VALUES
  (1, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (10, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (11, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `order_info` (`id`, `order_no`, `buyer_user_id`, `total_amount`, `pay_status`, `order_status`, `refund_status`,
  `logistics_status`, `logistics_current_index`, `auto_confirm_deadline`, `can_refund`, `version`, `create_time`, `update_time`)
VALUES
  (301, 'ORD_FLOW_301', 1, 120.00, 1, 2, 0, 'ARRIVED', 2, DATEADD('HOUR', -2, CURRENT_TIMESTAMP), 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `order_item` (`id`, `order_id`, `product_type`, `product_id`, `product_name`, `price`, `quantity`, `status`, `create_time`, `update_time`)
VALUES
  (30101, 301, 'NEW', 401, '自动确认测试商品', 60.00, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `order_info` (`id`, `order_no`, `buyer_user_id`, `total_amount`, `pay_status`, `order_status`, `refund_status`,
  `can_refund`, `version`, `create_time`, `update_time`)
VALUES
  (302, 'ORD_FLOW_302', 1, 50.00, 1, 1, 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `order_item` (`id`, `order_id`, `product_type`, `product_id`, `product_name`, `price`, `quantity`, `status`, `create_time`, `update_time`)
VALUES
  (30201, 302, 'NEW', 402, '仅退款测试商品', 50.00, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `order_info` (`id`, `order_no`, `buyer_user_id`, `total_amount`, `pay_status`, `order_status`, `refund_status`,
  `refund_mode`, `refund_apply_time`, `can_refund`, `version`, `create_time`, `update_time`)
VALUES
  (303, 'ORD_FLOW_303', 1, 70.00, 1, 2, 1, 'RETURN_REFUND', DATEADD('DAY', -8, CURRENT_TIMESTAMP), 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `order_item` (`id`, `order_id`, `product_type`, `product_id`, `product_name`, `price`, `quantity`, `status`, `create_time`, `update_time`)
VALUES
  (30301, 303, 'NEW', 403, '超时退款测试商品', 70.00, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
