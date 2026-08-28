INSERT IGNORE INTO `user`
  (`id`, `username`, `password`, `nickname`, `avatar`, `phone`, `email`, `role`, `status`, `credit_score`)
VALUES
  (1, 'admin', 'admin123', 'Platform Admin', '', '13800000000', 'admin@demo.com', 'ADMIN', 'NORMAL', 100),
  (2, 'seller', 'seller123', 'Demo Seller', '', '13800000001', 'seller@demo.com', 'OFFICIAL_SELLER', 'NORMAL', 100),
  (3, 'user', 'user123', 'Demo User', '', '13800000002', 'user@demo.com', 'USER', 'NORMAL', 100),
  (4, 'third', 'third123', 'Third Party User', '', '13800000003', 'third@demo.com', 'USER', 'NORMAL', 100);

CREATE TABLE IF NOT EXISTS `search_keyword_stat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `keyword` VARCHAR(100) NOT NULL,
  `stat_date` DATE NOT NULL,
  `search_count` INT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_search_keyword_stat_keyword_date` (`keyword`, `stat_date`),
  KEY `idx_search_keyword_stat_date_count` (`stat_date`, `search_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `shop`
  (`id`, `owner_user_id`, `name`, `logo`, `description`, `status`)
VALUES
  (1, 2, 'Container Demo Store', '', 'Seed shop for Issue #65 acceptance', 1);

INSERT INTO `category` (`id`, `name`, `parent_id`, `sort_order`, `status`)
VALUES
  (1, 'Digital Products', NULL, 1, 1),
  (101, 'Mobile Devices', 1, 1, 1)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `parent_id` = VALUES(`parent_id`),
  `sort_order` = VALUES(`sort_order`),
  `status` = VALUES(`status`);

-- The seller fixture must be able to exercise the real product editor.
UPDATE `user` SET `category` = '1' WHERE `id` = 2;

INSERT IGNORE INTO `product`
  (`id`, `shop_id`, `name`, `cover`, `description`, `price`, `category_id`, `sub_category_id`, `stock`, `status`)
VALUES
  (1, 1, 'Container Demo Keyboard', '', 'Seed product for Issue #65 acceptance', 299.00, 1, 101, 80, 1);

INSERT IGNORE INTO `address`
  (`id`, `user_id`, `receiver_name`, `receiver_phone`, `province`, `city`, `detail_address`, `is_default`)
VALUES
  (1, 3, 'Demo User', '13800000002', '北京市', '北京市', '测试路1号', 1);

INSERT INTO `balance`
  (`user_id`, `personal_balance`, `business_balance`, `version`)
VALUES
  (2, 0.00, 0.00, 0),
  (3, 100.00, 0.00, 0)
ON DUPLICATE KEY UPDATE
  `personal_balance` = VALUES(`personal_balance`),
  `business_balance` = VALUES(`business_balance`),
  `version` = VALUES(`version`);

-- UC14 browser fixture: a paid, delivered order that can enter the after-sale flow.
INSERT IGNORE INTO `order_info`
  (`id`, `order_no`, `buyer_user_id`, `total_amount`, `payable_amount`, `pay_status`, `order_status`,
   `refund_status`, `logistics_status`, `logistics_current_index`, `can_refund`, `version`)
VALUES
  (14001, 'UC14-E2E-AFTER-SALE', 3, 299.00, 299.00, 1, 2, 0, 'ARRIVED', 2, 1, 0);

INSERT IGNORE INTO `order_item`
  (`id`, `order_id`, `product_type`, `product_id`, `product_name`, `price`, `quantity`, `status`)
VALUES
  (14001, 14001, 'NEW', 1, 'Container Demo Keyboard', 299.00, 1, 1);

INSERT IGNORE INTO `order_info`
  (`id`, `order_no`, `buyer_user_id`, `total_amount`, `payable_amount`, `pay_status`, `order_status`,
   `refund_status`, `logistics_status`, `logistics_current_index`, `can_refund`, `version`)
VALUES
  (14002, 'UC14-E2E-ADMIN-ARBITRATION', 3, 299.00, 299.00, 1, 2, 0, 'ARRIVED', 2, 1, 0);

INSERT IGNORE INTO `order_item`
  (`id`, `order_id`, `product_type`, `product_id`, `product_name`, `price`, `quantity`, `status`)
VALUES
  (14002, 14002, 'NEW', 1, 'Container Demo Keyboard', 299.00, 1, 1);
-- UC12 browser fixtures: one order for payment and one for unpaid cancellation.
INSERT INTO `order_info`
  (`id`, `order_no`, `buyer_user_id`, `total_amount`, `payable_amount`, `pay_status`,
   `order_status`, `refund_status`, `receiver_name`, `receiver_phone`,
   `receiver_province`, `receiver_city`, `receiver_detail_address`, `version`)
VALUES
  (12001, 'UC12-E2E-PAY', 3, 299.00, 299.00, 0, 0, 0, 'Demo User', '13800000002', 'Shanghai', 'Shanghai', 'UC12 Pay Address', 0),
  (12002, 'UC12-E2E-CANCEL', 3, 299.00, 299.00, 0, 0, 0, 'Demo User', '13800000002', 'Shanghai', 'Shanghai', 'UC12 Cancel Address', 0)
ON DUPLICATE KEY UPDATE
  `pay_status` = VALUES(`pay_status`),
  `order_status` = VALUES(`order_status`),
  `version` = VALUES(`version`),
  `closed_time` = NULL,
  `paid_time` = NULL;

INSERT INTO `order_item`
  (`id`, `order_id`, `product_type`, `product_id`, `product_name`, `price`, `quantity`, `status`)
VALUES
  (12001, 12001, 'NEW', 1, 'Container Demo Keyboard', 299.00, 1, 0),
  (12002, 12002, 'NEW', 1, 'Container Demo Keyboard', 299.00, 1, 0)
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);
