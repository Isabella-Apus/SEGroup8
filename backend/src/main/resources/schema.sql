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
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_shop_owner_user_id` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `product` (
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
  `condition_level` VARCHAR(30) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_secondhand_seller` (`seller_user_id`)
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