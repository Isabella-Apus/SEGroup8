DELETE FROM `product_risk_audit` WHERE `seller_user_id` IN (1601, 1602);
DELETE FROM `secondhand_product` WHERE `seller_user_id` IN (1601, 1602);
DELETE FROM `category` WHERE `id` IN (1602, 1601);
DELETE FROM `user` WHERE `id` IN (1601, 1602);

INSERT INTO `user` (
  `id`, `username`, `password`, `nickname`, `role`, `status`, `credit_score`, `create_time`, `update_time`
)
VALUES
  (1601, 'uc16_seller', 'x', 'UC16 Seller', 'USER', 'NORMAL', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1602, 'uc16_other', 'x', 'UC16 Other', 'USER', 'NORMAL', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `category` (`id`, `name`, `parent_id`, `sort_order`, `status`)
VALUES
  (1601, 'UC16 Digital', NULL, 1, 1),
  (1602, 'UC16 Mobile', 1601, 1, 1);

INSERT INTO `secondhand_product` (
  `id`, `seller_user_id`, `name`, `cover`, `images`, `description`, `origin_price`, `sale_price`,
  `category_id`, `sub_category_id`, `condition_level`, `is_negotiable`, `status`, `create_time`, `update_time`
)
VALUES
  (1601, 1601, 'UC16 Owned Product', '/uploads/uc16-owned.png', '["/uploads/uc16-owned.png"]',
   'owned fixture', 200.00, 120.00, 1601, 1602, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1602, 1601, 'UC16 Off Shelf Product', '/uploads/uc16-off.png', '["/uploads/uc16-off.png"]',
   'off-shelf fixture', 180.00, 90.00, 1601, 1602, '80%', 0, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1603, 1601, 'UC16 Sold Product', '/uploads/uc16-sold.png', '["/uploads/uc16-sold.png"]',
   'sold fixture', 300.00, 160.00, 1601, 1602, '95%', 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1604, 1602, 'UC16 Foreign Product', '/uploads/uc16-foreign.png', '["/uploads/uc16-foreign.png"]',
   'foreign fixture', 220.00, 130.00, 1601, 1602, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1605, 1601, 'UC16 Delete Product', '/uploads/uc16-delete.png', '["/uploads/uc16-delete.png"]',
   'delete fixture', 150.00, 70.00, 1601, 1602, '80%', 0, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
