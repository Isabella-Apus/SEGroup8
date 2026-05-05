CREATE TABLE IF NOT EXISTS `user` (
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
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @user_credit_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'credit_score'
);
SET @user_credit_sql = IF(
  @user_credit_col_exists = 0,
  'ALTER TABLE `user` ADD COLUMN `credit_score` INT NOT NULL DEFAULT 100',
  'SELECT 1'
);
PREPARE stmt_user_credit FROM @user_credit_sql;
EXECUTE stmt_user_credit;
DEALLOCATE PREPARE stmt_user_credit;

SET @user_status_need_modify = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user'
    AND COLUMN_NAME = 'status'
    AND DATA_TYPE <> 'varchar'
);
SET @user_status_sql = IF(
  @user_status_need_modify = 1,
  'ALTER TABLE `user` MODIFY COLUMN `status` VARCHAR(20) NOT NULL DEFAULT ''NORMAL''',
  'SELECT 1'
);
PREPARE stmt_user_status FROM @user_status_sql;
EXECUTE stmt_user_status;
DEALLOCATE PREPARE stmt_user_status;

UPDATE `user`
SET `status` = CASE
  WHEN `status` IN ('1', 'NORMAL', 'normal') THEN 'NORMAL'
  ELSE 'BANNED'
END
WHERE `status` NOT IN ('NORMAL', 'BANNED');

CREATE TABLE IF NOT EXISTS `address` (
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

SET @addr_detail_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'address' AND COLUMN_NAME = 'detail_address'
);
SET @addr_detail_add_sql = IF(
  @addr_detail_col_exists = 0,
  'ALTER TABLE `address` ADD COLUMN `detail_address` VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE stmt_addr_add_detail_address FROM @addr_detail_add_sql;
EXECUTE stmt_addr_add_detail_address;
DEALLOCATE PREPARE stmt_addr_add_detail_address;

SET @addr_old_detail_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'address' AND COLUMN_NAME = 'detail'
);
SET @addr_detail_migrate_sql = IF(
  @addr_old_detail_col_exists = 1,
  'UPDATE `address` SET `detail_address` = `detail` WHERE (`detail_address` IS NULL OR `detail_address` = '''')',
  'SELECT 1'
);
PREPARE stmt_addr_migrate_detail FROM @addr_detail_migrate_sql;
EXECUTE stmt_addr_migrate_detail;
DEALLOCATE PREPARE stmt_addr_migrate_detail;

SET @addr_legacy_detail_need_fix = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'address'
    AND COLUMN_NAME = 'detail'
    AND IS_NULLABLE = 'NO'
    AND COLUMN_DEFAULT IS NULL
);
SET @addr_legacy_detail_fix_sql = IF(
  @addr_legacy_detail_need_fix = 1,
  'ALTER TABLE `address` MODIFY COLUMN `detail` VARCHAR(255) NULL DEFAULT ''''',
  'SELECT 1'
);
PREPARE stmt_addr_fix_detail_col FROM @addr_legacy_detail_fix_sql;
EXECUTE stmt_addr_fix_detail_col;
DEALLOCATE PREPARE stmt_addr_fix_detail_col;

CREATE TABLE IF NOT EXISTS `shop` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `owner_user_id` BIGINT NOT NULL,
  `name` VARCHAR(80) NOT NULL,
  `logo` VARCHAR(255) DEFAULT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `region` VARCHAR(100) DEFAULT NULL,
  `contact_name` VARCHAR(50) DEFAULT NULL,
  `contact_phone` VARCHAR(20) DEFAULT NULL,
  `id_card_no_masked` VARCHAR(50) DEFAULT NULL,
  `warehouse_addr` VARCHAR(255) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_shop_owner_user_id` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @shop_region_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop' AND COLUMN_NAME = 'region'
);
SET @sql = IF(@shop_region_exists = 0,
  'ALTER TABLE `shop` ADD COLUMN `region` VARCHAR(100) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @shop_contact_name_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop' AND COLUMN_NAME = 'contact_name'
);
SET @sql = IF(@shop_contact_name_exists = 0,
  'ALTER TABLE `shop` ADD COLUMN `contact_name` VARCHAR(50) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @shop_contact_phone_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop' AND COLUMN_NAME = 'contact_phone'
);
SET @sql = IF(@shop_contact_phone_exists = 0,
  'ALTER TABLE `shop` ADD COLUMN `contact_phone` VARCHAR(20) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @shop_id_card_no_masked_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop' AND COLUMN_NAME = 'id_card_no_masked'
);
SET @sql = IF(@shop_id_card_no_masked_exists = 0,
  'ALTER TABLE `shop` ADD COLUMN `id_card_no_masked` VARCHAR(50) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @shop_warehouse_addr_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop' AND COLUMN_NAME = 'warehouse_addr'
);
SET @sql = IF(@shop_warehouse_addr_exists = 0,
  'ALTER TABLE `shop` ADD COLUMN `warehouse_addr` VARCHAR(255) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `shop_id` BIGINT NOT NULL,
  `name` VARCHAR(120) NOT NULL,
  `cover` VARCHAR(255) DEFAULT NULL,
  `description` TEXT,
  `price` DECIMAL(10,2) NOT NULL,
  `category_id` INT NOT NULL DEFAULT 8,
  `sub_category_id` INT NOT NULL DEFAULT 801,
  `stock` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_shop_id` (`shop_id`),
  KEY `idx_product_category` (`category_id`, `sub_category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @product_category_id_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'category_id'
);
SET @product_category_id_add_sql = IF(
  @product_category_id_col_exists = 0,
  'ALTER TABLE `product` ADD COLUMN `category_id` INT NOT NULL DEFAULT 8 AFTER `price`',
  'SELECT 1'
);
PREPARE stmt_product_category_id_add FROM @product_category_id_add_sql;
EXECUTE stmt_product_category_id_add;
DEALLOCATE PREPARE stmt_product_category_id_add;

SET @product_sub_category_id_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'sub_category_id'
);
SET @product_sub_category_id_add_sql = IF(
  @product_sub_category_id_col_exists = 0,
  'ALTER TABLE `product` ADD COLUMN `sub_category_id` INT NOT NULL DEFAULT 801 AFTER `category_id`',
  'SELECT 1'
);
PREPARE stmt_product_sub_category_id_add FROM @product_sub_category_id_add_sql;
EXECUTE stmt_product_sub_category_id_add;
DEALLOCATE PREPARE stmt_product_sub_category_id_add;

CREATE TABLE IF NOT EXISTS `browse_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `product_type` VARCHAR(20) NOT NULL DEFAULT 'NEW',
  `product_id` BIGINT NOT NULL,
  `browse_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_browse_history_user_product_type_product` (`user_id`, `product_type`, `product_id`),
  KEY `idx_browse_history_user_browse_time` (`user_id`, `browse_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 兼容已存在库：补齐浏览记录类型字段和唯一索引（按 user + type + product 维度去重）
SET @browse_history_product_type_add_sql = IF (
  EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'browse_history'
      AND COLUMN_NAME = 'product_type'
  ),
  'SELECT 1',
  'ALTER TABLE `browse_history` ADD COLUMN `product_type` VARCHAR(20) NOT NULL DEFAULT ''NEW'' AFTER `user_id`'
);
PREPARE stmt_browse_history_add_product_type FROM @browse_history_product_type_add_sql;
EXECUTE stmt_browse_history_add_product_type;
DEALLOCATE PREPARE stmt_browse_history_add_product_type;

SET @browse_history_drop_old_uk_sql = IF (
  EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'browse_history'
      AND INDEX_NAME = 'uk_browse_history_user_product'
  ),
  'ALTER TABLE `browse_history` DROP INDEX `uk_browse_history_user_product`',
  'SELECT 1'
);
PREPARE stmt_browse_history_drop_old_uk FROM @browse_history_drop_old_uk_sql;
EXECUTE stmt_browse_history_drop_old_uk;
DEALLOCATE PREPARE stmt_browse_history_drop_old_uk;

SET @browse_history_add_new_uk_sql = IF (
  EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'browse_history'
      AND INDEX_NAME = 'uk_browse_history_user_product_type_product'
  ),
  'SELECT 1',
  'ALTER TABLE `browse_history` ADD UNIQUE KEY `uk_browse_history_user_product_type_product` (`user_id`, `product_type`, `product_id`)'
);
PREPARE stmt_browse_history_add_new_uk FROM @browse_history_add_new_uk_sql;
EXECUTE stmt_browse_history_add_new_uk;
DEALLOCATE PREPARE stmt_browse_history_add_new_uk;

CREATE TABLE IF NOT EXISTS `secondhand_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `seller_user_id` BIGINT NOT NULL,
  `name` VARCHAR(120) NOT NULL,
  `cover` VARCHAR(255) DEFAULT NULL,
  `description` TEXT,
  `origin_price` DECIMAL(10,2) DEFAULT NULL,
  `sale_price` DECIMAL(10,2) NOT NULL,
  `category_id` INT NOT NULL DEFAULT 8,
  `sub_category_id` INT NOT NULL DEFAULT 801,
  `condition_level` VARCHAR(30) DEFAULT NULL,
  `is_negotiable` TINYINT NOT NULL DEFAULT 1,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_secondhand_seller` (`seller_user_id`),
  KEY `idx_secondhand_category` (`category_id`, `sub_category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @secondhand_category_id_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'secondhand_product' AND COLUMN_NAME = 'category_id'
);
SET @secondhand_category_id_add_sql = IF(
  @secondhand_category_id_col_exists = 0,
  'ALTER TABLE `secondhand_product` ADD COLUMN `category_id` INT NOT NULL DEFAULT 8 AFTER `sale_price`',
  'SELECT 1'
);
PREPARE stmt_secondhand_category_id_add FROM @secondhand_category_id_add_sql;
EXECUTE stmt_secondhand_category_id_add;
DEALLOCATE PREPARE stmt_secondhand_category_id_add;

SET @secondhand_sub_category_id_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'secondhand_product' AND COLUMN_NAME = 'sub_category_id'
);
SET @secondhand_sub_category_id_add_sql = IF(
  @secondhand_sub_category_id_col_exists = 0,
  'ALTER TABLE `secondhand_product` ADD COLUMN `sub_category_id` INT NOT NULL DEFAULT 801 AFTER `category_id`',
  'SELECT 1'
);
PREPARE stmt_secondhand_sub_category_id_add FROM @secondhand_sub_category_id_add_sql;
EXECUTE stmt_secondhand_sub_category_id_add;
DEALLOCATE PREPARE stmt_secondhand_sub_category_id_add;

SET @secondhand_is_negotiable_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'secondhand_product' AND COLUMN_NAME = 'is_negotiable'
);
SET @secondhand_is_negotiable_add_sql = IF(
  @secondhand_is_negotiable_col_exists = 0,
  'ALTER TABLE `secondhand_product` ADD COLUMN `is_negotiable` TINYINT NOT NULL DEFAULT 1 AFTER `condition_level`',
  'SELECT 1'
);
PREPARE stmt_secondhand_is_negotiable_add FROM @secondhand_is_negotiable_add_sql;
EXECUTE stmt_secondhand_is_negotiable_add;
DEALLOCATE PREPARE stmt_secondhand_is_negotiable_add;

CREATE TABLE IF NOT EXISTS `product_auction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `seller_user_id` BIGINT NOT NULL,
  `start_price` DECIMAL(10,2) NOT NULL,
  `increment_amount` DECIMAL(10,2) NOT NULL,
  `current_price` DECIMAL(10,2) NOT NULL,
  `current_bidder_user_id` BIGINT DEFAULT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `settled_order_id` BIGINT DEFAULT NULL,
  `version` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_auction_product` (`product_id`),
  KEY `idx_product_auction_seller` (`seller_user_id`),
  KEY `idx_product_auction_status_end` (`status`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `auction_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `auction_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `bidder_user_id` BIGINT NOT NULL,
  `bid_amount` DECIMAL(10,2) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_auction_log_auction` (`auction_id`, `create_time`),
  KEY `idx_auction_log_bidder` (`bidder_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `product_negotiation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `buyer_user_id` BIGINT NOT NULL,
  `seller_user_id` BIGINT NOT NULL,
  `conversation_id` BIGINT DEFAULT NULL,
  `proposed_price` DECIMAL(10,2) NOT NULL,
  `confirmed_price` DECIMAL(10,2) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL,
  `effective_from` DATETIME DEFAULT NULL,
  `effective_until` DATETIME DEFAULT NULL,
  `used_order_id` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_negotiation_product` (`product_id`),
  KEY `idx_product_negotiation_buyer` (`buyer_user_id`),
  KEY `idx_product_negotiation_seller` (`seller_user_id`),
  KEY `idx_product_negotiation_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `order_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL,
  `buyer_user_id` BIGINT NOT NULL,
  `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `pay_status` TINYINT NOT NULL DEFAULT 0,
  `order_status` TINYINT NOT NULL DEFAULT 0,
  `refund_status` TINYINT NOT NULL DEFAULT 0,
  `refund_reason` VARCHAR(255) DEFAULT NULL,
  `refund_proof_urls` TEXT,
  `paid_time` DATETIME DEFAULT NULL,
  `shipped_time` DATETIME DEFAULT NULL,
  `received_time` DATETIME DEFAULT NULL,
  `completed_time` DATETIME DEFAULT NULL,
  `closed_time` DATETIME DEFAULT NULL,
  `refund_apply_time` DATETIME DEFAULT NULL,
  `refund_decision_time` DATETIME DEFAULT NULL,
  `refund_decision_user_id` BIGINT DEFAULT NULL,
  `refund_decision_remark` VARCHAR(255) DEFAULT NULL,
  `refund_decision_source` VARCHAR(20) DEFAULT NULL,
  `version` INT NOT NULL DEFAULT 0,
  `receiver_name` VARCHAR(50) DEFAULT NULL,
  `receiver_phone` VARCHAR(20) DEFAULT NULL,
  `receiver_province` VARCHAR(50) DEFAULT NULL,
  `receiver_city` VARCHAR(50) DEFAULT NULL,
  `receiver_detail_address` VARCHAR(255) DEFAULT NULL,
  `pay_method` VARCHAR(30) DEFAULT NULL,
  `delivery_no` VARCHAR(60) DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `voucher_id` BIGINT DEFAULT NULL,
  `voucher_discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `seller_bear_amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `platform_bear_amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `payable_amount` DECIMAL(10,2) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_info_order_no` (`order_no`),
  KEY `idx_order_info_buyer_user_id` (`buyer_user_id`),
  KEY `idx_order_info_status_refund_create` (`order_status`, `refund_status`, `create_time`),
  KEY `idx_order_info_refund_create` (`refund_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @order_refund_status_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'refund_status'
);
SET @order_refund_status_add_sql = IF(
  @order_refund_status_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `refund_status` TINYINT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt_order_add_refund_status FROM @order_refund_status_add_sql;
EXECUTE stmt_order_add_refund_status;
DEALLOCATE PREPARE stmt_order_add_refund_status;

SET @order_refund_reason_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'refund_reason'
);
SET @order_refund_reason_add_sql = IF(
  @order_refund_reason_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `refund_reason` VARCHAR(255) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_refund_reason FROM @order_refund_reason_add_sql;
EXECUTE stmt_order_add_refund_reason;
DEALLOCATE PREPARE stmt_order_add_refund_reason;

SET @order_refund_proof_urls_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'refund_proof_urls'
);
SET @order_refund_proof_urls_add_sql = IF(
  @order_refund_proof_urls_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `refund_proof_urls` TEXT',
  'SELECT 1'
);
PREPARE stmt_order_add_refund_proof_urls FROM @order_refund_proof_urls_add_sql;
EXECUTE stmt_order_add_refund_proof_urls;
DEALLOCATE PREPARE stmt_order_add_refund_proof_urls;

SET @order_receiver_name_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'receiver_name'
);
SET @order_add_receiver_name_sql = IF(
  @order_receiver_name_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `receiver_name` VARCHAR(50) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_receiver_name FROM @order_add_receiver_name_sql;
EXECUTE stmt_order_add_receiver_name;
DEALLOCATE PREPARE stmt_order_add_receiver_name;

SET @order_receiver_phone_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'receiver_phone'
);
SET @order_add_receiver_phone_sql = IF(
  @order_receiver_phone_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `receiver_phone` VARCHAR(20) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_receiver_phone FROM @order_add_receiver_phone_sql;
EXECUTE stmt_order_add_receiver_phone;
DEALLOCATE PREPARE stmt_order_add_receiver_phone;

SET @order_receiver_province_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'receiver_province'
);
SET @order_add_receiver_province_sql = IF(
  @order_receiver_province_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `receiver_province` VARCHAR(50) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_receiver_province FROM @order_add_receiver_province_sql;
EXECUTE stmt_order_add_receiver_province;
DEALLOCATE PREPARE stmt_order_add_receiver_province;

SET @order_receiver_city_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'receiver_city'
);
SET @order_add_receiver_city_sql = IF(
  @order_receiver_city_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `receiver_city` VARCHAR(50) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_receiver_city FROM @order_add_receiver_city_sql;
EXECUTE stmt_order_add_receiver_city;
DEALLOCATE PREPARE stmt_order_add_receiver_city;

SET @order_receiver_detail_address_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'receiver_detail_address'
);
SET @order_add_receiver_detail_address_sql = IF(
  @order_receiver_detail_address_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `receiver_detail_address` VARCHAR(255) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_receiver_detail_address FROM @order_add_receiver_detail_address_sql;
EXECUTE stmt_order_add_receiver_detail_address;
DEALLOCATE PREPARE stmt_order_add_receiver_detail_address;

SET @order_pay_method_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'pay_method'
);
SET @order_add_pay_method_sql = IF(
  @order_pay_method_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `pay_method` VARCHAR(30) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_pay_method FROM @order_add_pay_method_sql;
EXECUTE stmt_order_add_pay_method;
DEALLOCATE PREPARE stmt_order_add_pay_method;

SET @order_delivery_no_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'delivery_no'
);
SET @order_add_delivery_no_sql = IF(
  @order_delivery_no_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `delivery_no` VARCHAR(60) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_delivery_no FROM @order_add_delivery_no_sql;
EXECUTE stmt_order_add_delivery_no;
DEALLOCATE PREPARE stmt_order_add_delivery_no;

SET @order_paid_time_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'paid_time'
);
SET @order_paid_time_add_sql = IF(
  @order_paid_time_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `paid_time` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_paid_time FROM @order_paid_time_add_sql;
EXECUTE stmt_order_add_paid_time;
DEALLOCATE PREPARE stmt_order_add_paid_time;

SET @order_shipped_time_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'shipped_time'
);
SET @order_shipped_time_add_sql = IF(
  @order_shipped_time_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `shipped_time` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_shipped_time FROM @order_shipped_time_add_sql;
EXECUTE stmt_order_add_shipped_time;
DEALLOCATE PREPARE stmt_order_add_shipped_time;

SET @order_received_time_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'received_time'
);
SET @order_received_time_add_sql = IF(
  @order_received_time_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `received_time` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_received_time FROM @order_received_time_add_sql;
EXECUTE stmt_order_add_received_time;
DEALLOCATE PREPARE stmt_order_add_received_time;

SET @order_completed_time_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'completed_time'
);
SET @order_completed_time_add_sql = IF(
  @order_completed_time_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `completed_time` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_completed_time FROM @order_completed_time_add_sql;
EXECUTE stmt_order_add_completed_time;
DEALLOCATE PREPARE stmt_order_add_completed_time;

SET @order_closed_time_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'closed_time'
);
SET @order_closed_time_add_sql = IF(
  @order_closed_time_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `closed_time` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_closed_time FROM @order_closed_time_add_sql;
EXECUTE stmt_order_add_closed_time;
DEALLOCATE PREPARE stmt_order_add_closed_time;

SET @order_refund_apply_time_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'refund_apply_time'
);
SET @order_refund_apply_time_add_sql = IF(
  @order_refund_apply_time_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `refund_apply_time` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_refund_apply_time FROM @order_refund_apply_time_add_sql;
EXECUTE stmt_order_add_refund_apply_time;
DEALLOCATE PREPARE stmt_order_add_refund_apply_time;

SET @order_refund_decision_time_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'refund_decision_time'
);
SET @order_refund_decision_time_add_sql = IF(
  @order_refund_decision_time_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `refund_decision_time` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_refund_decision_time FROM @order_refund_decision_time_add_sql;
EXECUTE stmt_order_add_refund_decision_time;
DEALLOCATE PREPARE stmt_order_add_refund_decision_time;

SET @order_refund_decision_user_id_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'refund_decision_user_id'
);
SET @order_refund_decision_user_id_add_sql = IF(
  @order_refund_decision_user_id_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `refund_decision_user_id` BIGINT DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_refund_decision_user_id FROM @order_refund_decision_user_id_add_sql;
EXECUTE stmt_order_add_refund_decision_user_id;
DEALLOCATE PREPARE stmt_order_add_refund_decision_user_id;

SET @order_refund_decision_remark_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'refund_decision_remark'
);
SET @order_refund_decision_remark_add_sql = IF(
  @order_refund_decision_remark_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `refund_decision_remark` VARCHAR(255) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_refund_decision_remark FROM @order_refund_decision_remark_add_sql;
EXECUTE stmt_order_add_refund_decision_remark;
DEALLOCATE PREPARE stmt_order_add_refund_decision_remark;

SET @order_refund_decision_source_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'refund_decision_source'
);
SET @order_refund_decision_source_add_sql = IF(
  @order_refund_decision_source_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `refund_decision_source` VARCHAR(20) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_add_refund_decision_source FROM @order_refund_decision_source_add_sql;
EXECUTE stmt_order_add_refund_decision_source;
DEALLOCATE PREPARE stmt_order_add_refund_decision_source;

SET @order_version_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'version'
);
SET @order_version_add_sql = IF(
  @order_version_col_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `version` INT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt_order_add_version FROM @order_version_add_sql;
EXECUTE stmt_order_add_version;
DEALLOCATE PREPARE stmt_order_add_version;

SET @order_voucher_id_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'voucher_id'
);
SET @order_voucher_id_sql = IF(
  @order_voucher_id_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `voucher_id` BIGINT DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_voucher_id FROM @order_voucher_id_sql;
EXECUTE stmt_order_voucher_id;
DEALLOCATE PREPARE stmt_order_voucher_id;

SET @order_voucher_discount_amount_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'voucher_discount_amount'
);
SET @order_voucher_discount_amount_sql = IF(
  @order_voucher_discount_amount_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `voucher_discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt_order_voucher_discount_amount FROM @order_voucher_discount_amount_sql;
EXECUTE stmt_order_voucher_discount_amount;
DEALLOCATE PREPARE stmt_order_voucher_discount_amount;

SET @order_seller_bear_amount_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'seller_bear_amount'
);
SET @order_seller_bear_amount_sql = IF(
  @order_seller_bear_amount_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `seller_bear_amount` DECIMAL(10,2) NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt_order_seller_bear_amount FROM @order_seller_bear_amount_sql;
EXECUTE stmt_order_seller_bear_amount;
DEALLOCATE PREPARE stmt_order_seller_bear_amount;

SET @order_platform_bear_amount_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'platform_bear_amount'
);
SET @order_platform_bear_amount_sql = IF(
  @order_platform_bear_amount_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `platform_bear_amount` DECIMAL(10,2) NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt_order_platform_bear_amount FROM @order_platform_bear_amount_sql;
EXECUTE stmt_order_platform_bear_amount;
DEALLOCATE PREPARE stmt_order_platform_bear_amount;

SET @order_payable_amount_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'payable_amount'
);
SET @order_payable_amount_sql = IF(
  @order_payable_amount_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `payable_amount` DECIMAL(10,2) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_payable_amount FROM @order_payable_amount_sql;
EXECUTE stmt_order_payable_amount;
DEALLOCATE PREPARE stmt_order_payable_amount;

CREATE TABLE IF NOT EXISTS `order_after_sale_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `action` VARCHAR(30) NOT NULL,
  `operator_user_id` BIGINT DEFAULT NULL,
  `operator_role` VARCHAR(30) DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_after_sale_log_order_id` (`order_id`),
  KEY `idx_order_after_sale_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `order_item` (
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

CREATE TABLE IF NOT EXISTS `review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `product_type` VARCHAR(20) NOT NULL DEFAULT 'NEW',
  `product_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `score` TINYINT NOT NULL,
  `content` VARCHAR(500) DEFAULT NULL,
  `review_type` VARCHAR(20) NOT NULL DEFAULT 'ORIGINAL',
  `seller_reply` VARCHAR(500) DEFAULT NULL,
  `seller_reply_time` DATETIME DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_review_product` (`product_id`),
  KEY `idx_review_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @review_type_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review' AND COLUMN_NAME = 'review_type'
);
SET @review_type_add_sql = IF(
  @review_type_col_exists = 0,
  'ALTER TABLE `review` ADD COLUMN `review_type` VARCHAR(20) NOT NULL DEFAULT ''ORIGINAL''',
  'SELECT 1'
);
PREPARE stmt_review_type_add FROM @review_type_add_sql;
EXECUTE stmt_review_type_add;
DEALLOCATE PREPARE stmt_review_type_add;

SET @review_seller_reply_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review' AND COLUMN_NAME = 'seller_reply'
);
SET @review_seller_reply_add_sql = IF(
  @review_seller_reply_col_exists = 0,
  'ALTER TABLE `review` ADD COLUMN `seller_reply` VARCHAR(500) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_review_seller_reply_add FROM @review_seller_reply_add_sql;
EXECUTE stmt_review_seller_reply_add;
DEALLOCATE PREPARE stmt_review_seller_reply_add;

SET @review_seller_reply_time_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review' AND COLUMN_NAME = 'seller_reply_time'
);
SET @review_seller_reply_time_add_sql = IF(
  @review_seller_reply_time_col_exists = 0,
  'ALTER TABLE `review` ADD COLUMN `seller_reply_time` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_review_seller_reply_time_add FROM @review_seller_reply_time_add_sql;
EXECUTE stmt_review_seller_reply_time_add;
DEALLOCATE PREPARE stmt_review_seller_reply_time_add;

CREATE TABLE IF NOT EXISTS `report` (
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

CREATE TABLE IF NOT EXISTS `category` (
  `id` INT NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `parent_id` INT DEFAULT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_category_parent_id` (`parent_id`),
  KEY `idx_category_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `merchant_application` (
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

SET @ma_province_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_application' AND COLUMN_NAME = 'warehouse_province'
);
SET @ma_add_province_sql = IF(
  @ma_province_col_exists = 0,
  'ALTER TABLE `merchant_application` ADD COLUMN `warehouse_province` VARCHAR(50) NOT NULL DEFAULT ''''',
  'SELECT 1'
);
PREPARE stmt_ma_add_province FROM @ma_add_province_sql;
EXECUTE stmt_ma_add_province;
DEALLOCATE PREPARE stmt_ma_add_province;

SET @ma_city_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_application' AND COLUMN_NAME = 'warehouse_city'
);
SET @ma_add_city_sql = IF(
  @ma_city_col_exists = 0,
  'ALTER TABLE `merchant_application` ADD COLUMN `warehouse_city` VARCHAR(50) NOT NULL DEFAULT ''''',
  'SELECT 1'
);
PREPARE stmt_ma_add_city FROM @ma_add_city_sql;
EXECUTE stmt_ma_add_city;
DEALLOCATE PREPARE stmt_ma_add_city;

SET @ma_detail_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_application' AND COLUMN_NAME = 'warehouse_detail'
);
SET @ma_add_detail_sql = IF(
  @ma_detail_col_exists = 0,
  'ALTER TABLE `merchant_application` ADD COLUMN `warehouse_detail` VARCHAR(255) NOT NULL DEFAULT ''''',
  'SELECT 1'
);
PREPARE stmt_ma_add_detail FROM @ma_add_detail_sql;
EXECUTE stmt_ma_add_detail;
DEALLOCATE PREPARE stmt_ma_add_detail;

UPDATE `merchant_application`
SET `warehouse_detail` = `warehouse_addr`
WHERE (`warehouse_province` = '' AND `warehouse_city` = '')
  AND (`warehouse_detail` = '' OR `warehouse_detail` IS NULL)
  AND `warehouse_addr` IS NOT NULL;

CREATE TABLE IF NOT EXISTS `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `is_read` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notification_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `admin_audit_log` (
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

CREATE TABLE IF NOT EXISTS `idempotency_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `request_method` VARCHAR(10) NOT NULL,
  `request_path` VARCHAR(255) NOT NULL,
  `idempotency_key` VARCHAR(128) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0,
  `http_status` INT DEFAULT NULL,
  `response_body` TEXT,
  `expire_time` DATETIME NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotency_record` (`user_id`, `request_method`, `request_path`, `idempotency_key`),
  KEY `idx_idempotency_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @idem_status_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'idempotency_record' AND COLUMN_NAME = 'status'
);
SET @idem_status_add_sql = IF(
  @idem_status_col_exists = 0,
  'ALTER TABLE `idempotency_record` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt_idem_add_status FROM @idem_status_add_sql;
EXECUTE stmt_idem_add_status;
DEALLOCATE PREPARE stmt_idem_add_status;

SET @idem_http_status_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'idempotency_record' AND COLUMN_NAME = 'http_status'
);
SET @idem_http_status_add_sql = IF(
  @idem_http_status_col_exists = 0,
  'ALTER TABLE `idempotency_record` ADD COLUMN `http_status` INT DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_idem_add_http_status FROM @idem_http_status_add_sql;
EXECUTE stmt_idem_add_http_status;
DEALLOCATE PREPARE stmt_idem_add_http_status;

SET @idem_response_body_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'idempotency_record' AND COLUMN_NAME = 'response_body'
);
SET @idem_response_body_add_sql = IF(
  @idem_response_body_col_exists = 0,
  'ALTER TABLE `idempotency_record` ADD COLUMN `response_body` TEXT',
  'SELECT 1'
);
PREPARE stmt_idem_add_response_body FROM @idem_response_body_add_sql;
EXECUTE stmt_idem_add_response_body;
DEALLOCATE PREPARE stmt_idem_add_response_body;

SET @idem_update_time_col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'idempotency_record' AND COLUMN_NAME = 'update_time'
);
SET @idem_update_time_add_sql = IF(
  @idem_update_time_col_exists = 0,
  'ALTER TABLE `idempotency_record` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
  'SELECT 1'
);
PREPARE stmt_idem_add_update_time FROM @idem_update_time_add_sql;
EXECUTE stmt_idem_add_update_time;
DEALLOCATE PREPARE stmt_idem_add_update_time;

UPDATE `user`
SET `role` = 'OFFICIAL_SELLER'
WHERE `role` = 'SELLER';

CREATE TABLE IF NOT EXISTS `balance` (
  `user_id` BIGINT NOT NULL,
  `personal_balance` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `business_balance` DECIMAL(12,2) NOT NULL DEFAULT 0,
  `version` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `transaction_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT DEFAULT NULL,
  `user_id` BIGINT NOT NULL,
  `account_type` VARCHAR(20) NOT NULL,
  `change_type` VARCHAR(60) NOT NULL,
  `amount` DECIMAL(12,2) NOT NULL,
  `balance_after` DECIMAL(12,2) NOT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_transaction_record_user_id` (`user_id`),
  KEY `idx_transaction_record_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `logistics_path_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `origin_region` VARCHAR(20) NOT NULL,
  `dest_region` VARCHAR(20) NOT NULL,
  `path_nodes` JSON NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_logistics_template_region` (`origin_region`, `dest_region`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `logistics_trace` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `node_name` VARCHAR(80) NOT NULL,
  `status_desc` VARCHAR(120) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_logistics_trace_order_id` (`order_id`),
  KEY `idx_logistics_trace_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @order_logistics_template_id_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'logistics_template_id'
);
SET @order_logistics_template_id_sql = IF(
  @order_logistics_template_id_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `logistics_template_id` BIGINT DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_logistics_template_id FROM @order_logistics_template_id_sql;
EXECUTE stmt_order_logistics_template_id;
DEALLOCATE PREPARE stmt_order_logistics_template_id;

SET @order_logistics_status_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'logistics_status'
);
SET @order_logistics_status_sql = IF(
  @order_logistics_status_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `logistics_status` VARCHAR(20) NOT NULL DEFAULT ''PENDING''',
  'SELECT 1'
);
PREPARE stmt_order_logistics_status FROM @order_logistics_status_sql;
EXECUTE stmt_order_logistics_status;
DEALLOCATE PREPARE stmt_order_logistics_status;

SET @order_logistics_current_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'logistics_current_index'
);
SET @order_logistics_current_index_sql = IF(
  @order_logistics_current_index_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `logistics_current_index` INT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt_order_logistics_current_index FROM @order_logistics_current_index_sql;
EXECUTE stmt_order_logistics_current_index;
DEALLOCATE PREPARE stmt_order_logistics_current_index;

SET @order_can_refund_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'can_refund'
);
SET @order_can_refund_sql = IF(
  @order_can_refund_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `can_refund` TINYINT NOT NULL DEFAULT 1',
  'SELECT 1'
);
PREPARE stmt_order_can_refund FROM @order_can_refund_sql;
EXECUTE stmt_order_can_refund;
DEALLOCATE PREPARE stmt_order_can_refund;

SET @order_after_sales_deadline_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'after_sales_deadline'
);
SET @order_after_sales_deadline_sql = IF(
  @order_after_sales_deadline_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `after_sales_deadline` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_after_sales_deadline FROM @order_after_sales_deadline_sql;
EXECUTE stmt_order_after_sales_deadline;
DEALLOCATE PREPARE stmt_order_after_sales_deadline;

SET @order_delivery_time_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'delivery_time'
);
SET @order_delivery_time_sql = IF(
  @order_delivery_time_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `delivery_time` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_delivery_time FROM @order_delivery_time_sql;
EXECUTE stmt_order_delivery_time;
DEALLOCATE PREPARE stmt_order_delivery_time;

SET @order_arrival_time_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'arrival_time'
);
SET @order_arrival_time_sql = IF(
  @order_arrival_time_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `arrival_time` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_arrival_time FROM @order_arrival_time_sql;
EXECUTE stmt_order_arrival_time;
DEALLOCATE PREPARE stmt_order_arrival_time;

SET @order_auto_confirm_deadline_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'auto_confirm_deadline'
);
SET @order_auto_confirm_deadline_sql = IF(
  @order_auto_confirm_deadline_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `auto_confirm_deadline` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_auto_confirm_deadline FROM @order_auto_confirm_deadline_sql;
EXECUTE stmt_order_auto_confirm_deadline;
DEALLOCATE PREPARE stmt_order_auto_confirm_deadline;

SET @order_refund_mode_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_info' AND COLUMN_NAME = 'refund_mode'
);
SET @order_refund_mode_sql = IF(
  @order_refund_mode_exists = 0,
  'ALTER TABLE `order_info` ADD COLUMN `refund_mode` VARCHAR(20) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_order_refund_mode FROM @order_refund_mode_sql;
EXECUTE stmt_order_refund_mode;
DEALLOCATE PREPARE stmt_order_refund_mode;

SET @tr_trade_type_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transaction_record' AND COLUMN_NAME = 'trade_type'
);
SET @tr_trade_type_sql = IF(
  @tr_trade_type_exists = 0,
  'ALTER TABLE `transaction_record` ADD COLUMN `trade_type` VARCHAR(60) DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_tr_trade_type FROM @tr_trade_type_sql;
EXECUTE stmt_tr_trade_type;
DEALLOCATE PREPARE stmt_tr_trade_type;

-- ============================================================
-- 补充 user 表的店铺相关字段（SellerShopSetting 功能需要）
-- 使用 IF NOT EXISTS 模式，已有字段不重复添加
-- ============================================================

SET @user_shop_name_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'shop_name'
);
SET @sql = IF(@user_shop_name_exists = 0,
  'ALTER TABLE `user` ADD COLUMN `shop_name` VARCHAR(80) DEFAULT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @user_shop_desc_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'shop_desc'
);
SET @sql = IF(@user_shop_desc_exists = 0,
  'ALTER TABLE `user` ADD COLUMN `shop_desc` VARCHAR(255) DEFAULT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @user_banner_url_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'banner_url'
);
SET @sql = IF(@user_banner_url_exists = 0,
  'ALTER TABLE `user` ADD COLUMN `banner_url` VARCHAR(255) DEFAULT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @user_category_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'category'
);
SET @sql = IF(@user_category_exists = 0,
  'ALTER TABLE `user` ADD COLUMN `category` VARCHAR(50) DEFAULT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @user_region_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'region'
);
SET @sql = IF(@user_region_exists = 0,
  'ALTER TABLE `user` ADD COLUMN `region` VARCHAR(100) DEFAULT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @user_business_hours_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'business_hours'
);
SET @sql = IF(@user_business_hours_exists = 0,
  'ALTER TABLE `user` ADD COLUMN `business_hours` VARCHAR(100) DEFAULT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @user_return_policy_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'return_policy'
);
SET @sql = IF(@user_return_policy_exists = 0,
  'ALTER TABLE `user` ADD COLUMN `return_policy` VARCHAR(500) DEFAULT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @user_shipping_policy_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'shipping_policy'
);
SET @sql = IF(@user_shipping_policy_exists = 0,
  'ALTER TABLE `user` ADD COLUMN `shipping_policy` VARCHAR(300) DEFAULT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @user_announcement_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'announcement'
);
SET @sql = IF(@user_announcement_exists = 0,
  'ALTER TABLE `user` ADD COLUMN `announcement` VARCHAR(300) DEFAULT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- voucher 表（优惠券功能）
CREATE TABLE IF NOT EXISTS `voucher` (
  `id`              BIGINT NOT NULL AUTO_INCREMENT,
  `shop_id`         BIGINT NOT NULL,
  `name`            VARCHAR(100) NOT NULL,
  `type`            TINYINT NOT NULL DEFAULT 1,
  `discount_amount` DECIMAL(10,2) DEFAULT NULL,
  `discount_rate`   DECIMAL(4,2)  DEFAULT NULL,
  `min_amount`      DECIMAL(10,2) NOT NULL DEFAULT 0,
  `total_count`     INT NOT NULL DEFAULT 0,
  `used_count`      INT NOT NULL DEFAULT 0,
  `start_time`      DATETIME NOT NULL,
  `end_time`        DATETIME NOT NULL,
  `status`          TINYINT NOT NULL DEFAULT 1,
  `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_voucher_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @voucher_issuer_type_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'issuer_type'
);
SET @voucher_issuer_type_sql = IF(
  @voucher_issuer_type_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `issuer_type` TINYINT NOT NULL DEFAULT 1',
  'SELECT 1'
);
PREPARE stmt_voucher_issuer_type FROM @voucher_issuer_type_sql;
EXECUTE stmt_voucher_issuer_type;
DEALLOCATE PREPARE stmt_voucher_issuer_type;

SET @voucher_type_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'voucher_type'
);
SET @voucher_type_sql = IF(
  @voucher_type_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `voucher_type` TINYINT NOT NULL DEFAULT 1',
  'SELECT 1'
);
PREPARE stmt_voucher_type FROM @voucher_type_sql;
EXECUTE stmt_voucher_type;
DEALLOCATE PREPARE stmt_voucher_type;

SET @voucher_issuer_user_id_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'issuer_user_id'
);
SET @voucher_issuer_user_id_sql = IF(
  @voucher_issuer_user_id_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `issuer_user_id` BIGINT DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_voucher_issuer_user_id FROM @voucher_issuer_user_id_sql;
EXECUTE stmt_voucher_issuer_user_id;
DEALLOCATE PREPARE stmt_voucher_issuer_user_id;

SET @voucher_scope_type_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'scope_type'
);
SET @voucher_scope_type_sql = IF(
  @voucher_scope_type_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `scope_type` TINYINT NOT NULL DEFAULT 1',
  'SELECT 1'
);
PREPARE stmt_voucher_scope_type FROM @voucher_scope_type_sql;
EXECUTE stmt_voucher_scope_type;
DEALLOCATE PREPARE stmt_voucher_scope_type;

SET @voucher_product_id_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'product_id'
);
SET @voucher_product_id_sql = IF(
  @voucher_product_id_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `product_id` BIGINT DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_voucher_product_id FROM @voucher_product_id_sql;
EXECUTE stmt_voucher_product_id;
DEALLOCATE PREPARE stmt_voucher_product_id;

SET @voucher_can_stack_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'can_stack'
);
SET @voucher_can_stack_sql = IF(
  @voucher_can_stack_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `can_stack` TINYINT(1) NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt_voucher_can_stack FROM @voucher_can_stack_sql;
EXECUTE stmt_voucher_can_stack;
DEALLOCATE PREPARE stmt_voucher_can_stack;

SET @voucher_received_count_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'received_count'
);
SET @voucher_received_count_sql = IF(
  @voucher_received_count_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `received_count` INT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt_voucher_received_count FROM @voucher_received_count_sql;
EXECUTE stmt_voucher_received_count;
DEALLOCATE PREPARE stmt_voucher_received_count;

SET @voucher_grab_start_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'grab_start_time'
);
SET @voucher_grab_start_sql = IF(
  @voucher_grab_start_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `grab_start_time` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_voucher_grab_start FROM @voucher_grab_start_sql;
EXECUTE stmt_voucher_grab_start;
DEALLOCATE PREPARE stmt_voucher_grab_start;

SET @voucher_grab_end_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'grab_end_time'
);
SET @voucher_grab_end_sql = IF(
  @voucher_grab_end_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `grab_end_time` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_voucher_grab_end FROM @voucher_grab_end_sql;
EXECUTE stmt_voucher_grab_end;
DEALLOCATE PREPARE stmt_voucher_grab_end;
ALTER TABLE `voucher` MODIFY COLUMN `shop_id` BIGINT DEFAULT NULL;
ALTER TABLE `voucher` MODIFY COLUMN `start_time` DATETIME DEFAULT NULL;
ALTER TABLE `voucher` MODIFY COLUMN `end_time` DATETIME DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `user_voucher` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `voucher_id` BIGINT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `received_time` DATETIME DEFAULT NULL,
  `used_order_id` BIGINT DEFAULT NULL,
  `used_time` DATETIME DEFAULT NULL,
  `expire_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_voucher_user_id` (`user_id`),
  KEY `idx_user_voucher_voucher_id` (`voucher_id`),
  KEY `idx_user_voucher_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @user_voucher_used_order_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_voucher' AND COLUMN_NAME = 'used_order_id'
);
SET @user_voucher_used_order_sql = IF(
  @user_voucher_used_order_exists = 0,
  'ALTER TABLE `user_voucher` ADD COLUMN `used_order_id` BIGINT DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_user_voucher_used_order FROM @user_voucher_used_order_sql;
EXECUTE stmt_user_voucher_used_order;
DEALLOCATE PREPARE stmt_user_voucher_used_order;

SET @user_voucher_used_time_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_voucher' AND COLUMN_NAME = 'used_time'
);
SET @user_voucher_used_time_sql = IF(
  @user_voucher_used_time_exists = 0,
  'ALTER TABLE `user_voucher` ADD COLUMN `used_time` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_user_voucher_used_time FROM @user_voucher_used_time_sql;
EXECUTE stmt_user_voucher_used_time;
DEALLOCATE PREPARE stmt_user_voucher_used_time;

SET @user_voucher_expire_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_voucher' AND COLUMN_NAME = 'expire_time'
);
SET @user_voucher_expire_sql = IF(
  @user_voucher_expire_exists = 0,
  'ALTER TABLE `user_voucher` ADD COLUMN `expire_time` DATETIME DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_user_voucher_expire FROM @user_voucher_expire_sql;
EXECUTE stmt_user_voucher_expire;
DEALLOCATE PREPARE stmt_user_voucher_expire;

CREATE TABLE IF NOT EXISTS `chat_conversation` (
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

CREATE TABLE IF NOT EXISTS `chat_message` (
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

-- =====================================================
-- 信用评分、举报、拉黑 相关表（追加到schema.sql末尾）
-- =====================================================

-- 信用分变动日志
CREATE TABLE IF NOT EXISTS `credit_score_log` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT NOT NULL COMMENT '目标用户',
  `role`        VARCHAR(20) NOT NULL COMMENT '变动时身份：BUYER / SELLER',
  `delta`       INT NOT NULL COMMENT '分数变化，正为加分负为扣分',
  `reason_code` VARCHAR(50) NOT NULL COMMENT '原因码',
  `reason_desc` VARCHAR(255) DEFAULT NULL COMMENT '详细说明',
  `ref_id`      BIGINT DEFAULT NULL COMMENT '关联业务ID（订单/举报等）',
  `operator_id` BIGINT DEFAULT NULL COMMENT '触发操作者ID，系统自动则NULL',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_csl_user_id` (`user_id`),
  KEY `idx_csl_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='信用分变动日志';

-- 举报记录
CREATE TABLE IF NOT EXISTS `user_report` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT,
  `reporter_id`   BIGINT NOT NULL COMMENT '举报人ID',
  `reported_id`   BIGINT NOT NULL COMMENT '被举报人ID',
  `reporter_role` VARCHAR(20) NOT NULL COMMENT '举报人当时身份：BUYER/SELLER',
  `trade_context` VARCHAR(20) NOT NULL DEFAULT 'SHOP'
                  COMMENT '交易场景：SHOP=店铺 SH_BUYER=二手买家举报卖家 SH_SELLER=二手卖家举报买家',
  `reason_type`   VARCHAR(50) NOT NULL COMMENT '举报类型',
  `reason_desc`   VARCHAR(500) DEFAULT NULL COMMENT '补充说明',
  `evidence_urls` VARCHAR(1000) DEFAULT NULL COMMENT '证据图片URL，逗号分隔',
  `status`        TINYINT NOT NULL DEFAULT 0
                  COMMENT '0=待审核 1=成立扣分 2=不成立驳回',
  `admin_id`      BIGINT DEFAULT NULL COMMENT '处理管理员ID',
  `admin_remark`  VARCHAR(500) DEFAULT NULL COMMENT '管理员备注',
  `audit_time`    DATETIME DEFAULT NULL,
  `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ur_reporter` (`reporter_id`),
  KEY `idx_ur_reported` (`reported_id`),
  KEY `idx_ur_status`   (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='用户举报记录';

-- 拉黑关系
CREATE TABLE IF NOT EXISTS `user_block` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `blocker_id`  BIGINT NOT NULL COMMENT '拉黑操作者',
  `blocked_id`  BIGINT NOT NULL COMMENT '被拉黑用户',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_block` (`blocker_id`, `blocked_id`),
  KEY `idx_ub_blocker` (`blocker_id`),
  KEY `idx_ub_blocked` (`blocked_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  COMMENT='用户拉黑关系';

-- 为 user 表追加卖家信用分列（幂等，已存在则跳过）
SET @col_seller = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user'
    AND COLUMN_NAME = 'seller_credit_score'
);
SET @sql1 = IF(@col_seller = 0,
  'ALTER TABLE `user` ADD COLUMN `seller_credit_score` INT NOT NULL DEFAULT 100 COMMENT ''卖家信用分''',
  'SELECT 1');
PREPARE s1 FROM @sql1; EXECUTE s1; DEALLOCATE PREPARE s1;

-- 为 user 表追加买家信用分列（幂等，已存在则跳过）
SET @col_buyer = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user'
    AND COLUMN_NAME = 'buyer_credit_score'
);
SET @sql2 = IF(@col_buyer = 0,
  'ALTER TABLE `user` ADD COLUMN `buyer_credit_score` INT NOT NULL DEFAULT 100 COMMENT ''买家信用分''',
  'SELECT 1');
PREPARE s2 FROM @sql2; EXECUTE s2; DEALLOCATE PREPARE s2;

-- 为 user_report 表追加 trade_context 列（幂等，已存在则跳过）
SET @col_tc = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_report'
    AND COLUMN_NAME = 'trade_context'
);
SET @sql_tc = IF(@col_tc = 0,
  'ALTER TABLE `user_report` ADD COLUMN `trade_context` VARCHAR(20) NOT NULL DEFAULT ''SHOP'' COMMENT ''交易场景：SHOP/SH_BUYER/SH_SELLER'' AFTER `reporter_role`',
  'SELECT 1');
PREPARE stc FROM @sql_tc; EXECUTE stc; DEALLOCATE PREPARE stc;

-- =====================================================
-- 信用评分、举报、拉黑 相关新表
-- =====================================================

CREATE TABLE IF NOT EXISTS `credit_score_log` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT NOT NULL,
  `role`        VARCHAR(20) NOT NULL,
  `delta`       INT NOT NULL,
  `reason_code` VARCHAR(50) NOT NULL,
  `reason_desc` VARCHAR(255) DEFAULT NULL,
  `ref_id`      BIGINT DEFAULT NULL,
  `operator_id` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_csl_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_report` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT,
  `reporter_id`   BIGINT NOT NULL,
  `reported_id`   BIGINT NOT NULL,
  `reporter_role` VARCHAR(20) NOT NULL,
  `trade_context` VARCHAR(20) NOT NULL DEFAULT 'SHOP',
  `reason_type`   VARCHAR(50) NOT NULL,
  `reason_desc`   VARCHAR(500) DEFAULT NULL,
  `evidence_urls` VARCHAR(1000) DEFAULT NULL,
  `status`        TINYINT NOT NULL DEFAULT 0,
  `admin_id`      BIGINT DEFAULT NULL,
  `admin_remark`  VARCHAR(500) DEFAULT NULL,
  `audit_time`    DATETIME DEFAULT NULL,
  `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ur_reporter` (`reporter_id`),
  KEY `idx_ur_reported` (`reported_id`),
  KEY `idx_ur_status`   (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_block` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `blocker_id`  BIGINT NOT NULL,
  `blocked_id`  BIGINT NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_block` (`blocker_id`, `blocked_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
