DELETE FROM `notification`;

INSERT INTO `notification` (`id`, `user_id`, `title`, `content`, `target_path`, `is_read`, `create_time`)
VALUES
  (25001, 1, '买家未读通知', '订单状态已更新', '/order/25001', 0, CURRENT_TIMESTAMP),
  (25002, 1, '卖家未读通知', '店铺有新的待处理订单', '/merchant/orders', 0, CURRENT_TIMESTAMP),
  (25003, 1, '买家已读通知', '商品已收藏', '/product/1001', 1, CURRENT_TIMESTAMP),
  (25004, 3, '其他用户通知', '不能泄露给当前用户', '/merchant/orders', 0, CURRENT_TIMESTAMP);
