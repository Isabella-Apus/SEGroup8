INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `status`, `create_time`, `update_time`)
VALUES
  (1, 'buyer1', 'x', '买家1', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'admin1', 'x', '管理员1', 'ADMIN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 一个用于集成测试的订单：已支付/待发货，且已有售后申请（refund_status=1）
INSERT INTO `order_info` (
  `id`, `order_no`, `buyer_user_id`, `total_amount`, `pay_status`, `order_status`, `refund_status`,
  `refund_reason`, `refund_apply_time`, `version`, `create_time`, `update_time`
) VALUES (
  101, 'ORD_TEST_101', 1, 99.00, 1, 1, 1,
  '质量问题', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO `order_item` (`order_id`, `product_type`, `product_id`, `product_name`, `price`, `quantity`)
VALUES (101, 'NEW', 1001, '测试商品', 99.00, 1);


INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `status`, `create_time`, `update_time`)
VALUES (3, 'seller1', 'x', 'seller1', 'OFFICIAL_SELLER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `shop` (`id`, `owner_user_id`, `name`, `status`, `create_time`, `update_time`)
VALUES (100, 3, 'test shop', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `product` (`id`, `shop_id`, `name`, `price`, `stock`, `status`, `create_time`, `update_time`)
VALUES (1001, 100, 'test product', 99.00, 100, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
