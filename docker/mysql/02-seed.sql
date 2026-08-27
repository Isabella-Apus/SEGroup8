INSERT IGNORE INTO `user`
  (`id`, `username`, `password`, `nickname`, `avatar`, `phone`, `email`, `role`, `status`, `credit_score`)
VALUES
  (1, 'admin', 'admin123', 'Platform Admin', '', '13800000000', 'admin@demo.com', 'ADMIN', 'NORMAL', 100),
  (2, 'seller', 'seller123', 'Demo Seller', '', '13800000001', 'seller@demo.com', 'OFFICIAL_SELLER', 'NORMAL', 100),
  (3, 'user', 'user123', 'Demo User', '', '13800000002', 'user@demo.com', 'USER', 'NORMAL', 100);

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
