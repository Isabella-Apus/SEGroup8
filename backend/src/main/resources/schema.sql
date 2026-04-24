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

CREATE TABLE IF NOT EXISTS `voucher` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `issuer_type` TINYINT NOT NULL DEFAULT 1,
  `voucher_type` TINYINT NOT NULL DEFAULT 1,
  `issuer_user_id` BIGINT DEFAULT NULL,
  `scope_type` TINYINT NOT NULL DEFAULT 1,
  `shop_id` BIGINT DEFAULT NULL,
  `product_id` BIGINT DEFAULT NULL,
  `name` VARCHAR(100) NOT NULL,
  `type` TINYINT NOT NULL DEFAULT 1,
  `discount_amount` DECIMAL(10,2) DEFAULT NULL,
  `discount_rate` DECIMAL(4,2) DEFAULT NULL,
  `min_amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `can_stack` TINYINT(1) NOT NULL DEFAULT 0,
  `total_count` INT NOT NULL DEFAULT 0,
  `received_count` INT NOT NULL DEFAULT 0,
  `used_count` INT NOT NULL DEFAULT 0,
  `receive_limit` INT NOT NULL DEFAULT 1,
  `grab_start_time` DATETIME NOT NULL,
  `grab_end_time` DATETIME NOT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0=已关闭,1=进行中,2=未开始,3=已结束,4=已抢光',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_voucher_scope_shop` (`scope_type`, `shop_id`),
  KEY `idx_voucher_scope_product` (`scope_type`, `product_id`),
  KEY `idx_voucher_issuer` (`issuer_type`, `issuer_user_id`),
  KEY `idx_voucher_type_status_time` (`voucher_type`, `status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @voucher_issuer_type_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
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
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
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
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
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
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
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

SET @voucher_shop_id_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'shop_id'
);
SET @voucher_shop_id_sql = IF(
  @voucher_shop_id_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `shop_id` BIGINT DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_voucher_shop_id FROM @voucher_shop_id_sql;
EXECUTE stmt_voucher_shop_id;
DEALLOCATE PREPARE stmt_voucher_shop_id;

SET @voucher_shop_id_nullable = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'voucher'
    AND COLUMN_NAME = 'shop_id'
    AND IS_NULLABLE = 'NO'
);
SET @voucher_shop_id_modify_sql = IF(
  @voucher_shop_id_nullable = 1,
  'ALTER TABLE `voucher` MODIFY COLUMN `shop_id` BIGINT NULL DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt_voucher_shop_id_modify FROM @voucher_shop_id_modify_sql;
EXECUTE stmt_voucher_shop_id_modify;
DEALLOCATE PREPARE stmt_voucher_shop_id_modify;

SET @voucher_product_id_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
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
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
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

SET @voucher_grab_start_time_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'grab_start_time'
);
SET @voucher_grab_start_time_sql = IF(
  @voucher_grab_start_time_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `grab_start_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
  'SELECT 1'
);
PREPARE stmt_voucher_grab_start_time FROM @voucher_grab_start_time_sql;
EXECUTE stmt_voucher_grab_start_time;
DEALLOCATE PREPARE stmt_voucher_grab_start_time;

SET @voucher_grab_end_time_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'grab_end_time'
);
SET @voucher_grab_end_time_sql = IF(
  @voucher_grab_end_time_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `grab_end_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
  'SELECT 1'
);
PREPARE stmt_voucher_grab_end_time FROM @voucher_grab_end_time_sql;
EXECUTE stmt_voucher_grab_end_time;
DEALLOCATE PREPARE stmt_voucher_grab_end_time;

SET @voucher_total_count_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'total_count'
);
SET @voucher_total_count_sql = IF(
  @voucher_total_count_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `total_count` INT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt_voucher_total_count FROM @voucher_total_count_sql;
EXECUTE stmt_voucher_total_count;
DEALLOCATE PREPARE stmt_voucher_total_count;

SET @voucher_received_count_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
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

SET @voucher_used_count_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'used_count'
);
SET @voucher_used_count_sql = IF(
  @voucher_used_count_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `used_count` INT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt_voucher_used_count FROM @voucher_used_count_sql;
EXECUTE stmt_voucher_used_count;
DEALLOCATE PREPARE stmt_voucher_used_count;

SET @voucher_status_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'status'
);
SET @voucher_status_sql = IF(
  @voucher_status_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1',
  'SELECT 1'
);
PREPARE stmt_voucher_status FROM @voucher_status_sql;
EXECUTE stmt_voucher_status;
DEALLOCATE PREPARE stmt_voucher_status;

SET @voucher_create_time_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'create_time'
);
SET @voucher_create_time_sql = IF(
  @voucher_create_time_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
  'SELECT 1'
);
PREPARE stmt_voucher_create_time FROM @voucher_create_time_sql;
EXECUTE stmt_voucher_create_time;
DEALLOCATE PREPARE stmt_voucher_create_time;

SET @voucher_create_time_need_modify = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'voucher'
    AND COLUMN_NAME = 'create_time'
    AND (
      IS_NULLABLE = 'YES'
      OR COLUMN_DEFAULT IS NULL
    )
);
SET @voucher_create_time_modify_sql = IF(
  @voucher_create_time_need_modify = 1,
  'ALTER TABLE `voucher` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
  'SELECT 1'
);
PREPARE stmt_voucher_create_time_modify FROM @voucher_create_time_modify_sql;
EXECUTE stmt_voucher_create_time_modify;
DEALLOCATE PREPARE stmt_voucher_create_time_modify;

SET @voucher_update_time_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher' AND COLUMN_NAME = 'update_time'
);
SET @voucher_update_time_sql = IF(
  @voucher_update_time_exists = 0,
  'ALTER TABLE `voucher` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
  'SELECT 1'
);
PREPARE stmt_voucher_update_time FROM @voucher_update_time_sql;
EXECUTE stmt_voucher_update_time;
DEALLOCATE PREPARE stmt_voucher_update_time;

SET @voucher_update_time_need_modify = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'voucher'
    AND COLUMN_NAME = 'update_time'
    AND (
      IS_NULLABLE = 'YES'
      OR COLUMN_DEFAULT IS NULL
    )
);
SET @voucher_update_time_modify_sql = IF(
  @voucher_update_time_need_modify = 1,
  'ALTER TABLE `voucher` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
  'SELECT 1'
);
PREPARE stmt_voucher_update_time_modify FROM @voucher_update_time_modify_sql;
EXECUTE stmt_voucher_update_time_modify;
DEALLOCATE PREPARE stmt_voucher_update_time_modify;

CREATE TABLE IF NOT EXISTS `user_voucher` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `voucher_id` BIGINT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `received_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `used_order_id` BIGINT DEFAULT NULL,
  `used_time` DATETIME DEFAULT NULL,
  `expire_time` DATETIME NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_voucher_user_status` (`user_id`, `status`, `expire_time`),
  KEY `idx_user_voucher_voucher` (`voucher_id`),
  UNIQUE KEY `uk_user_voucher_once` (`user_id`, `voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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

-- 其余既有表结构与兼容迁移脚本保持不变

-- 统一补齐常见时间列默认值，避免旧库插入时报 NULL
SET @ts_fix_address_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='address' AND COLUMN_NAME='create_time'), 'ALTER TABLE `address` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_address_ct FROM @ts_fix_address_ct; EXECUTE stmt_ts_fix_address_ct; DEALLOCATE PREPARE stmt_ts_fix_address_ct;
SET @ts_fix_address_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='address' AND COLUMN_NAME='update_time'), 'ALTER TABLE `address` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_address_ut FROM @ts_fix_address_ut; EXECUTE stmt_ts_fix_address_ut; DEALLOCATE PREPARE stmt_ts_fix_address_ut;

SET @ts_fix_voucher_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='voucher' AND COLUMN_NAME='create_time'), 'ALTER TABLE `voucher` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_voucher_ct FROM @ts_fix_voucher_ct; EXECUTE stmt_ts_fix_voucher_ct; DEALLOCATE PREPARE stmt_ts_fix_voucher_ct;
SET @ts_fix_voucher_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='voucher' AND COLUMN_NAME='update_time'), 'ALTER TABLE `voucher` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_voucher_ut FROM @ts_fix_voucher_ut; EXECUTE stmt_ts_fix_voucher_ut; DEALLOCATE PREPARE stmt_ts_fix_voucher_ut;

SET @ts_fix_user_voucher_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='user_voucher' AND COLUMN_NAME='create_time'), 'ALTER TABLE `user_voucher` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_user_voucher_ct FROM @ts_fix_user_voucher_ct; EXECUTE stmt_ts_fix_user_voucher_ct; DEALLOCATE PREPARE stmt_ts_fix_user_voucher_ct;
SET @ts_fix_user_voucher_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='user_voucher' AND COLUMN_NAME='update_time'), 'ALTER TABLE `user_voucher` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_user_voucher_ut FROM @ts_fix_user_voucher_ut; EXECUTE stmt_ts_fix_user_voucher_ut; DEALLOCATE PREPARE stmt_ts_fix_user_voucher_ut;

SET @ts_fix_shop_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='shop' AND COLUMN_NAME='create_time'), 'ALTER TABLE `shop` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_shop_ct FROM @ts_fix_shop_ct; EXECUTE stmt_ts_fix_shop_ct; DEALLOCATE PREPARE stmt_ts_fix_shop_ct;
SET @ts_fix_shop_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='shop' AND COLUMN_NAME='update_time'), 'ALTER TABLE `shop` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_shop_ut FROM @ts_fix_shop_ut; EXECUTE stmt_ts_fix_shop_ut; DEALLOCATE PREPARE stmt_ts_fix_shop_ut;

SET @ts_fix_product_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='product' AND COLUMN_NAME='create_time'), 'ALTER TABLE `product` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_product_ct FROM @ts_fix_product_ct; EXECUTE stmt_ts_fix_product_ct; DEALLOCATE PREPARE stmt_ts_fix_product_ct;
SET @ts_fix_product_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='product' AND COLUMN_NAME='update_time'), 'ALTER TABLE `product` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_product_ut FROM @ts_fix_product_ut; EXECUTE stmt_ts_fix_product_ut; DEALLOCATE PREPARE stmt_ts_fix_product_ut;

SET @ts_fix_secondhand_product_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='secondhand_product' AND COLUMN_NAME='create_time'), 'ALTER TABLE `secondhand_product` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_secondhand_product_ct FROM @ts_fix_secondhand_product_ct; EXECUTE stmt_ts_fix_secondhand_product_ct; DEALLOCATE PREPARE stmt_ts_fix_secondhand_product_ct;
SET @ts_fix_secondhand_product_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='secondhand_product' AND COLUMN_NAME='update_time'), 'ALTER TABLE `secondhand_product` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_secondhand_product_ut FROM @ts_fix_secondhand_product_ut; EXECUTE stmt_ts_fix_secondhand_product_ut; DEALLOCATE PREPARE stmt_ts_fix_secondhand_product_ut;

SET @ts_fix_review_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='review' AND COLUMN_NAME='create_time'), 'ALTER TABLE `review` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_review_ct FROM @ts_fix_review_ct; EXECUTE stmt_ts_fix_review_ct; DEALLOCATE PREPARE stmt_ts_fix_review_ct;
SET @ts_fix_review_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='review' AND COLUMN_NAME='update_time'), 'ALTER TABLE `review` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_review_ut FROM @ts_fix_review_ut; EXECUTE stmt_ts_fix_review_ut; DEALLOCATE PREPARE stmt_ts_fix_review_ut;

SET @ts_fix_notification_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='notification' AND COLUMN_NAME='create_time'), 'ALTER TABLE `notification` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_notification_ct FROM @ts_fix_notification_ct; EXECUTE stmt_ts_fix_notification_ct; DEALLOCATE PREPARE stmt_ts_fix_notification_ct;
SET @ts_fix_notification_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='notification' AND COLUMN_NAME='update_time'), 'ALTER TABLE `notification` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_notification_ut FROM @ts_fix_notification_ut; EXECUTE stmt_ts_fix_notification_ut; DEALLOCATE PREPARE stmt_ts_fix_notification_ut;

SET @ts_fix_transaction_record_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='transaction_record' AND COLUMN_NAME='create_time'), 'ALTER TABLE `transaction_record` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_transaction_record_ct FROM @ts_fix_transaction_record_ct; EXECUTE stmt_ts_fix_transaction_record_ct; DEALLOCATE PREPARE stmt_ts_fix_transaction_record_ct;
SET @ts_fix_transaction_record_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='transaction_record' AND COLUMN_NAME='update_time'), 'ALTER TABLE `transaction_record` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_transaction_record_ut FROM @ts_fix_transaction_record_ut; EXECUTE stmt_ts_fix_transaction_record_ut; DEALLOCATE PREPARE stmt_ts_fix_transaction_record_ut;

SET @ts_fix_balance_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='balance' AND COLUMN_NAME='create_time'), 'ALTER TABLE `balance` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_balance_ct FROM @ts_fix_balance_ct; EXECUTE stmt_ts_fix_balance_ct; DEALLOCATE PREPARE stmt_ts_fix_balance_ct;
SET @ts_fix_balance_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='balance' AND COLUMN_NAME='update_time'), 'ALTER TABLE `balance` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_balance_ut FROM @ts_fix_balance_ut; EXECUTE stmt_ts_fix_balance_ut; DEALLOCATE PREPARE stmt_ts_fix_balance_ut;

SET @ts_fix_chat_conversation_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='chat_conversation' AND COLUMN_NAME='create_time'), 'ALTER TABLE `chat_conversation` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_chat_conversation_ct FROM @ts_fix_chat_conversation_ct; EXECUTE stmt_ts_fix_chat_conversation_ct; DEALLOCATE PREPARE stmt_ts_fix_chat_conversation_ct;
SET @ts_fix_chat_conversation_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='chat_conversation' AND COLUMN_NAME='update_time'), 'ALTER TABLE `chat_conversation` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_chat_conversation_ut FROM @ts_fix_chat_conversation_ut; EXECUTE stmt_ts_fix_chat_conversation_ut; DEALLOCATE PREPARE stmt_ts_fix_chat_conversation_ut;

SET @ts_fix_chat_message_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='chat_message' AND COLUMN_NAME='create_time'), 'ALTER TABLE `chat_message` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_chat_message_ct FROM @ts_fix_chat_message_ct; EXECUTE stmt_ts_fix_chat_message_ct; DEALLOCATE PREPARE stmt_ts_fix_chat_message_ct;
SET @ts_fix_chat_message_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='chat_message' AND COLUMN_NAME='update_time'), 'ALTER TABLE `chat_message` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_chat_message_ut FROM @ts_fix_chat_message_ut; EXECUTE stmt_ts_fix_chat_message_ut; DEALLOCATE PREPARE stmt_ts_fix_chat_message_ut;

SET @ts_fix_order_info_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='order_info' AND COLUMN_NAME='create_time'), 'ALTER TABLE `order_info` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_order_info_ct FROM @ts_fix_order_info_ct; EXECUTE stmt_ts_fix_order_info_ct; DEALLOCATE PREPARE stmt_ts_fix_order_info_ct;
SET @ts_fix_order_info_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='order_info' AND COLUMN_NAME='update_time'), 'ALTER TABLE `order_info` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_order_info_ut FROM @ts_fix_order_info_ut; EXECUTE stmt_ts_fix_order_info_ut; DEALLOCATE PREPARE stmt_ts_fix_order_info_ut;

SET @ts_fix_order_item_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='order_item' AND COLUMN_NAME='create_time'), 'ALTER TABLE `order_item` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_order_item_ct FROM @ts_fix_order_item_ct; EXECUTE stmt_ts_fix_order_item_ct; DEALLOCATE PREPARE stmt_ts_fix_order_item_ct;
SET @ts_fix_order_item_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='order_item' AND COLUMN_NAME='update_time'), 'ALTER TABLE `order_item` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_order_item_ut FROM @ts_fix_order_item_ut; EXECUTE stmt_ts_fix_order_item_ut; DEALLOCATE PREPARE stmt_ts_fix_order_item_ut;

SET @ts_fix_order_after_sale_log_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='order_after_sale_log' AND COLUMN_NAME='create_time'), 'ALTER TABLE `order_after_sale_log` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_order_after_sale_log_ct FROM @ts_fix_order_after_sale_log_ct; EXECUTE stmt_ts_fix_order_after_sale_log_ct; DEALLOCATE PREPARE stmt_ts_fix_order_after_sale_log_ct;
SET @ts_fix_order_after_sale_log_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='order_after_sale_log' AND COLUMN_NAME='update_time'), 'ALTER TABLE `order_after_sale_log` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_order_after_sale_log_ut FROM @ts_fix_order_after_sale_log_ut; EXECUTE stmt_ts_fix_order_after_sale_log_ut; DEALLOCATE PREPARE stmt_ts_fix_order_after_sale_log_ut;

SET @ts_fix_browse_history_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='browse_history' AND COLUMN_NAME='create_time'), 'ALTER TABLE `browse_history` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_browse_history_ct FROM @ts_fix_browse_history_ct; EXECUTE stmt_ts_fix_browse_history_ct; DEALLOCATE PREPARE stmt_ts_fix_browse_history_ct;
SET @ts_fix_browse_history_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='browse_history' AND COLUMN_NAME='update_time'), 'ALTER TABLE `browse_history` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_browse_history_ut FROM @ts_fix_browse_history_ut; EXECUTE stmt_ts_fix_browse_history_ut; DEALLOCATE PREPARE stmt_ts_fix_browse_history_ut;

SET @ts_fix_merchant_application_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='merchant_application' AND COLUMN_NAME='create_time'), 'ALTER TABLE `merchant_application` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_merchant_application_ct FROM @ts_fix_merchant_application_ct; EXECUTE stmt_ts_fix_merchant_application_ct; DEALLOCATE PREPARE stmt_ts_fix_merchant_application_ct;
SET @ts_fix_merchant_application_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='merchant_application' AND COLUMN_NAME='update_time'), 'ALTER TABLE `merchant_application` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_merchant_application_ut FROM @ts_fix_merchant_application_ut; EXECUTE stmt_ts_fix_merchant_application_ut; DEALLOCATE PREPARE stmt_ts_fix_merchant_application_ut;

SET @ts_fix_admin_audit_log_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='admin_audit_log' AND COLUMN_NAME='create_time'), 'ALTER TABLE `admin_audit_log` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_admin_audit_log_ct FROM @ts_fix_admin_audit_log_ct; EXECUTE stmt_ts_fix_admin_audit_log_ct; DEALLOCATE PREPARE stmt_ts_fix_admin_audit_log_ct;
SET @ts_fix_admin_audit_log_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='admin_audit_log' AND COLUMN_NAME='update_time'), 'ALTER TABLE `admin_audit_log` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_admin_audit_log_ut FROM @ts_fix_admin_audit_log_ut; EXECUTE stmt_ts_fix_admin_audit_log_ut; DEALLOCATE PREPARE stmt_ts_fix_admin_audit_log_ut;

SET @ts_fix_logistics_trace_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='logistics_trace' AND COLUMN_NAME='create_time'), 'ALTER TABLE `logistics_trace` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_logistics_trace_ct FROM @ts_fix_logistics_trace_ct; EXECUTE stmt_ts_fix_logistics_trace_ct; DEALLOCATE PREPARE stmt_ts_fix_logistics_trace_ct;
SET @ts_fix_logistics_trace_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='logistics_trace' AND COLUMN_NAME='update_time'), 'ALTER TABLE `logistics_trace` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_logistics_trace_ut FROM @ts_fix_logistics_trace_ut; EXECUTE stmt_ts_fix_logistics_trace_ut; DEALLOCATE PREPARE stmt_ts_fix_logistics_trace_ut;

SET @ts_fix_logistics_path_template_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='logistics_path_template' AND COLUMN_NAME='create_time'), 'ALTER TABLE `logistics_path_template` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_logistics_path_template_ct FROM @ts_fix_logistics_path_template_ct; EXECUTE stmt_ts_fix_logistics_path_template_ct; DEALLOCATE PREPARE stmt_ts_fix_logistics_path_template_ct;
SET @ts_fix_logistics_path_template_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='logistics_path_template' AND COLUMN_NAME='update_time'), 'ALTER TABLE `logistics_path_template` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_logistics_path_template_ut FROM @ts_fix_logistics_path_template_ut; EXECUTE stmt_ts_fix_logistics_path_template_ut; DEALLOCATE PREPARE stmt_ts_fix_logistics_path_template_ut;

SET @ts_fix_idempotency_record_ct = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='idempotency_record' AND COLUMN_NAME='create_time'), 'ALTER TABLE `idempotency_record` MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_idempotency_record_ct FROM @ts_fix_idempotency_record_ct; EXECUTE stmt_ts_fix_idempotency_record_ct; DEALLOCATE PREPARE stmt_ts_fix_idempotency_record_ct;
SET @ts_fix_idempotency_record_ut = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='idempotency_record' AND COLUMN_NAME='update_time'), 'ALTER TABLE `idempotency_record` MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt_ts_fix_idempotency_record_ut FROM @ts_fix_idempotency_record_ut; EXECUTE stmt_ts_fix_idempotency_record_ut; DEALLOCATE PREPARE stmt_ts_fix_idempotency_record_ut;
