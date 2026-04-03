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
  `remark` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_info_order_no` (`order_no`),
  KEY `idx_order_info_buyer_user_id` (`buyer_user_id`)
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
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_review_product` (`product_id`),
  KEY `idx_review_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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

UPDATE `user`
SET `role` = 'OFFICIAL_SELLER'
WHERE `role` = 'SELLER';
