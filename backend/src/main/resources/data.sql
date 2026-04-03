INSERT IGNORE INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`, `phone`, `email`, `role`, `status`, `credit_score`)
VALUES
(1, 'admin', 'admin123', 'PlatformAdmin', '', '13800000000', 'admin@demo.com', 'ADMIN', 'NORMAL', 100),
(2, 'seller', 'seller123', 'DemoSeller', '', '13800000001', 'seller@demo.com', 'OFFICIAL_SELLER', 'NORMAL', 100),
(3, 'user', 'user123', 'DemoUser', '', '13800000002', 'user@demo.com', 'USER', 'NORMAL', 100);

INSERT IGNORE INTO `address` (`id`, `user_id`, `receiver_name`, `receiver_phone`, `province`, `city`, `detail_address`, `is_default`)
VALUES
(1, 3, 'Zhang San', '13800000002', 'Beijing', 'Beijing', 'Software Park Building 1', 1);

INSERT IGNORE INTO `shop` (`id`, `owner_user_id`, `name`, `logo`, `description`, `status`)
VALUES
(1, 2, 'Digital Store', '', 'Demo shop for new products', 1);

INSERT IGNORE INTO `product` (`id`, `shop_id`, `name`, `cover`, `description`, `price`, `stock`, `status`)
VALUES
(1, 1, 'Mechanical Keyboard K87', '', '87-key hot-swappable keyboard', 299.00, 80, 1),
(2, 1, 'Wireless Mouse M2', '', 'Bluetooth dual-mode mouse', 89.00, 120, 1),
(3, 1, '27-inch Monitor', '', '2K IPS monitor for demo', 1299.00, 30, 1);

INSERT IGNORE INTO `secondhand_product` (`id`, `seller_user_id`, `name`, `cover`, `description`, `origin_price`, `sale_price`, `condition_level`, `status`)
VALUES
(1, 3, 'Used Bicycle', '', 'Gently used and works well', 1200.00, 650.00, '90%', 1),
(2, 3, 'Spare Headphones', '', 'Minor usage marks, fully functional', 399.00, 180.00, '80%', 1);

INSERT IGNORE INTO `order_info` (`id`, `order_no`, `buyer_user_id`, `total_amount`, `pay_status`, `order_status`, `remark`)
VALUES
(1, 'ORD202604020001', 3, 388.00, 1, 1, 'Seed order for demo');

INSERT IGNORE INTO `order_item` (`id`, `order_id`, `product_type`, `product_id`, `product_name`, `price`, `quantity`, `status`)
VALUES
(1, 1, 'NEW', 1, 'Mechanical Keyboard K87', 299.00, 1, 1),
(2, 1, 'NEW', 2, 'Wireless Mouse M2', 89.00, 1, 1);

INSERT IGNORE INTO `review` (`id`, `order_id`, `product_type`, `product_id`, `user_id`, `score`, `content`, `status`)
VALUES
(1, 1, 'NEW', 1, 3, 5, 'Good typing feel and fast delivery', 1);

INSERT IGNORE INTO `report` (`id`, `reporter_user_id`, `target_type`, `target_id`, `reason`, `status`)
VALUES
(1, 3, 'SECONDHAND_PRODUCT', 2, 'Description does not fully match the item', 0);
