DROP DATABASE IF EXISTS segroup8_platform;
CREATE DATABASE segroup8_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE segroup8_platform;

DROP TABLE IF EXISTS `report`;
DROP TABLE IF EXISTS `review`;
DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `order_info`;
DROP TABLE IF EXISTS `secondhand_product`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `shop`;
DROP TABLE IF EXISTS `address`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(100) NOT NULL,
  `nickname` VARCHAR(50) DEFAULT NULL,
  `avatar` VARCHAR(255) DEFAULT NULL,
  `phone` VARCHAR(20) DEFAULT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  `credit_score` INT NOT NULL DEFAULT 100,
  `seller_credit_score` INT NOT NULL DEFAULT 100,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `address` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `receiver_name` VARCHAR(50) NOT NULL,
  `receiver_phone` VARCHAR(20) NOT NULL,
  `province` VARCHAR(50) NOT NULL,
  `city` VARCHAR(50) NOT NULL,
  `detail_address` VARCHAR(255) NOT NULL,
  `is_default` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_address_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `merchant_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `store_name` VARCHAR(80) NOT NULL,
  `category_id` INT NOT NULL,
  `id_card_no` VARCHAR(30) NOT NULL,
  `bank_card_no` VARCHAR(50) NOT NULL,
  `license_img` VARCHAR(255) NOT NULL,
  `warehouse_addr` VARCHAR(255) NOT NULL,
  `warehouse_province` VARCHAR(50) NOT NULL DEFAULT '',
  `warehouse_city` VARCHAR(50) NOT NULL DEFAULT '',
  `warehouse_detail` VARCHAR(255) NOT NULL DEFAULT '',
  `contact_name` VARCHAR(50) NOT NULL,
  `contact_phone` VARCHAR(20) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0,
  `reject_reason` VARCHAR(255) DEFAULT NULL,
  `apply_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_application_user_id` (`user_id`),
  KEY `idx_merchant_application_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `is_read` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notification_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `chat_conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `buyer_user_id` BIGINT NOT NULL,
  `seller_user_id` BIGINT NOT NULL,
  `source_type` VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
  `source_id` BIGINT NOT NULL DEFAULT 0,
  `source_title` VARCHAR(120) DEFAULT NULL,
  `last_message_content` VARCHAR(1000) DEFAULT NULL,
  `last_message_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chat_conversation_pair` (`buyer_user_id`, `seller_user_id`, `source_type`, `source_id`),
  KEY `idx_chat_conversation_buyer` (`buyer_user_id`),
  KEY `idx_chat_conversation_seller` (`seller_user_id`),
  KEY `idx_chat_conversation_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `conversation_id` BIGINT NOT NULL,
  `sender_user_id` BIGINT NOT NULL,
  `receiver_user_id` BIGINT NOT NULL,
  `content` VARCHAR(1000) NOT NULL,
  `is_read` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chat_message_conversation` (`conversation_id`, `create_time`),
  KEY `idx_chat_message_receiver` (`receiver_user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `admin_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `admin_user_id` BIGINT DEFAULT NULL,
  `admin_username` VARCHAR(50) NOT NULL,
  `action` VARCHAR(80) NOT NULL,
  `target_type` VARCHAR(50) NOT NULL,
  `target_id` BIGINT DEFAULT NULL,
  `detail` VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_audit_log_admin_user_id` (`admin_user_id`),
  KEY `idx_admin_audit_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `shop` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `owner_user_id` BIGINT NOT NULL,
  `name` VARCHAR(80) NOT NULL,
  `logo` VARCHAR(255) DEFAULT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_shop_owner_user_id` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `shop_id` BIGINT NOT NULL,
  `name` VARCHAR(120) NOT NULL,
  `cover` VARCHAR(255) DEFAULT NULL,
  `description` TEXT,
  `price` DECIMAL(10,2) NOT NULL,
  `stock` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `secondhand_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `seller_user_id` BIGINT NOT NULL,
  `name` VARCHAR(120) NOT NULL,
  `cover` VARCHAR(255) DEFAULT NULL,
  `description` TEXT,
  `origin_price` DECIMAL(10,2) DEFAULT NULL,
  `sale_price` DECIMAL(10,2) NOT NULL,
  `condition_level` VARCHAR(30) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_secondhand_seller` (`seller_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `order_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL,
  `buyer_user_id` BIGINT NOT NULL,
  `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `pay_status` TINYINT NOT NULL DEFAULT 0,
  `order_status` TINYINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_info_order_no` (`order_no`),
  KEY `idx_order_info_buyer_user_id` (`buyer_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `product_type` VARCHAR(20) NOT NULL DEFAULT 'NEW',
  `product_id` BIGINT NOT NULL,
  `product_name` VARCHAR(120) NOT NULL,
  `price` DECIMAL(10,2) NOT NULL,
  `quantity` INT NOT NULL DEFAULT 1,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_item_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `product_type` VARCHAR(20) NOT NULL DEFAULT 'NEW',
  `product_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `review_type` VARCHAR(30) NOT NULL DEFAULT 'BUYER_TO_SELLER',
  `score` TINYINT NOT NULL,
  `content` VARCHAR(500) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_review_product` (`product_id`),
  KEY `idx_review_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `credit_score_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role` VARCHAR(10) NOT NULL COMMENT 'BUYER or SELLER',
  `delta` INT NOT NULL,
  `reason_code` VARCHAR(50) NOT NULL,
  `reason_desc` VARCHAR(255) DEFAULT NULL,
  `ref_id` BIGINT DEFAULT NULL,
  `operator_id` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_credit_log_user` (`user_id`, `role`),
  KEY `idx_credit_log_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `user_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reporter_id` BIGINT NOT NULL,
  `reported_id` BIGINT NOT NULL,
  `reporter_role` VARCHAR(10) NOT NULL COMMENT 'BUYER or SELLER',
  `reason_type` VARCHAR(30) NOT NULL,
  `reason_desc` VARCHAR(500) DEFAULT NULL,
  `evidence_urls` VARCHAR(1000) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待审核 1=成立 2=驳回',
  `admin_id` BIGINT DEFAULT NULL,
  `admin_remark` VARCHAR(255) DEFAULT NULL,
  `audit_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_report_reporter` (`reporter_id`),
  KEY `idx_user_report_reported` (`reported_id`),
  KEY `idx_user_report_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `user_block` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `blocker_id` BIGINT NOT NULL,
  `blocked_id` BIGINT NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_block` (`blocker_id`, `blocked_id`),
  KEY `idx_user_block_blocker` (`blocker_id`),
  KEY `idx_user_block_blocked` (`blocked_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reporter_user_id` BIGINT NOT NULL,
  `target_type` VARCHAR(30) NOT NULL,
  `target_id` BIGINT NOT NULL,
  `reason` VARCHAR(255) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_reporter` (`reporter_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`, `phone`, `email`, `role`, `status`)
VALUES
(1, 'admin', 'admin123', 'PlatformAdmin', '', '13800000000', 'admin@demo.com', 'ADMIN', 1),
(2, 'seller', 'seller123', 'DemoSeller', '', '13800000001', 'seller@demo.com', 'SELLER', 1),
(3, 'user', 'user123', 'DemoUser', '', '13800000002', 'user@demo.com', 'USER', 1);

INSERT INTO `address` (`id`, `user_id`, `receiver_name`, `receiver_phone`, `province`, `city`, `district`, `detail`, `is_default`, `status`)
VALUES
(1, 3, 'Zhang San', '13800000002', 'Beijing', 'Beijing', 'Haidian', 'Software Park Building 1', 1, 1);

INSERT INTO `shop` (`id`, `owner_user_id`, `name`, `logo`, `description`, `status`)
VALUES
(1, 2, 'Digital Store', '', 'Demo shop for new products', 1);

INSERT INTO `product` (`id`, `shop_id`, `name`, `cover`, `description`, `price`, `stock`, `status`)
VALUES
(1, 1, 'Mechanical Keyboard K87', '', '87-key hot-swappable keyboard', 299.00, 80, 1),
(2, 1, 'Wireless Mouse M2', '', 'Bluetooth dual-mode mouse', 89.00, 120, 1),
(3, 1, '27-inch Monitor', '', '2K IPS monitor for demo', 1299.00, 30, 1);

INSERT INTO `secondhand_product` (`id`, `seller_user_id`, `name`, `cover`, `description`, `origin_price`, `sale_price`, `condition_level`, `status`)
VALUES
(1, 3, 'Used Bicycle', '', 'Gently used and works well', 1200.00, 650.00, '90%', 1),
(2, 3, 'Spare Headphones', '', 'Minor usage marks, fully functional', 399.00, 180.00, '80%', 1);

INSERT INTO `order_info` (`id`, `order_no`, `buyer_user_id`, `total_amount`, `pay_status`, `order_status`, `remark`)
VALUES
(1, 'ORD202604020001', 3, 388.00, 1, 1, 'Seed order for demo');

INSERT INTO `order_item` (`id`, `order_id`, `product_type`, `product_id`, `product_name`, `price`, `quantity`, `status`)
VALUES
(1, 1, 'NEW', 1, 'Mechanical Keyboard K87', 299.00, 1, 1),
(2, 1, 'NEW', 2, 'Wireless Mouse M2', 89.00, 1, 1);

INSERT INTO `review` (`id`, `order_id`, `product_type`, `product_id`, `user_id`, `score`, `content`, `status`)
VALUES
(1, 1, 'NEW', 1, 3, 5, 'Good typing feel and fast delivery', 1);

INSERT INTO `report` (`id`, `reporter_user_id`, `target_type`, `target_id`, `reason`, `status`)
VALUES
(1, 3, 'SECONDHAND_PRODUCT', 2, 'Description does not fully match the item', 0);