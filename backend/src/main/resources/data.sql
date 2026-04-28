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

INSERT INTO `category` (`id`, `name`, `parent_id`, `sort_order`, `status`)
VALUES
(1, '电子数码', NULL, 1, 1),
(2, '服饰鞋包', NULL, 2, 1),
(3, '居家生活', NULL, 3, 1),
(4, '美妆个护', NULL, 4, 1),
(5, '运动户外', NULL, 5, 1),
(6, '图书音像', NULL, 6, 1),
(7, '美食类', NULL, 7, 1),
(8, '其他', NULL, 8, 1),
(101, '手机', 1, 1, 1),
(102, '电脑/平板', 1, 2, 1),
(103, '摄影摄像', 1, 3, 1),
(104, '影音娱乐', 1, 4, 1),
(105, '智能穿戴', 1, 5, 1),
(201, '潮流女装', 2, 1, 1),
(202, '品质男装', 2, 2, 1),
(203, '运动户外', 2, 3, 1),
(204, '精选箱包', 2, 4, 1),
(205, '时尚饰品', 2, 5, 1),
(301, '家具家装', 3, 1, 1),
(302, '厨房用具', 3, 2, 1),
(303, '居家日用', 3, 3, 1),
(304, '家用电器', 3, 4, 1),
(305, '收纳整理', 3, 5, 1),
(401, '面部护肤', 4, 1, 1),
(402, '魅力彩妆', 4, 2, 1),
(403, '个人护理', 4, 3, 1),
(404, '香水香氛', 4, 4, 1),
(405, '美容仪器', 4, 5, 1),
(501, '健身器材', 5, 1, 1),
(502, '户外装备', 5, 2, 1),
(503, '体育用品', 5, 3, 1),
(504, '骑行运动', 5, 4, 1),
(601, '教材教辅', 6, 1, 1),
(602, '小说文学', 6, 2, 1),
(603, '艺术收藏', 6, 3, 1),
(604, '办公用品', 6, 4, 1),
(701, '休闲零食', 7, 1, 1),
(702, '粮油调味', 7, 2, 1),
(703, '生鲜果蔬', 7, 3, 1),
(704, '冲调饮品', 7, 4, 1),
(705, '地方特产', 7, 5, 1),
(801, '未分类', 8, 1, 1)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `parent_id` = VALUES(`parent_id`),
  `sort_order` = VALUES(`sort_order`),
  `status` = VALUES(`status`);

INSERT IGNORE INTO `product` (`id`, `shop_id`, `name`, `cover`, `description`, `price`, `category_id`, `sub_category_id`, `stock`, `status`)
VALUES
(1, 1, 'Mechanical Keyboard K87', '', '87-key hot-swappable keyboard', 299.00, 1, 102, 80, 1),
(2, 1, 'Wireless Mouse M2', '', 'Bluetooth dual-mode mouse', 89.00, 1, 105, 120, 1),
(3, 1, '27-inch Monitor', '', '2K IPS monitor for demo', 1299.00, 1, 102, 30, 1);

INSERT IGNORE INTO `secondhand_product` (`id`, `seller_user_id`, `name`, `cover`, `description`, `origin_price`, `sale_price`, `category_id`, `sub_category_id`, `condition_level`, `is_negotiable`, `status`)
VALUES
(1, 3, 'Used Bicycle', '', 'Gently used and works well', 1200.00, 650.00, 5, 504, '9成新', 1, 1),
(2, 3, 'Spare Headphones', '', 'Minor usage marks, fully functional', 399.00, 180.00, 1, 104, '8成新及以下', 0, 1);

INSERT INTO `order_info` (`id`, `order_no`, `buyer_user_id`, `total_amount`, `pay_status`, `order_status`, `refund_status`, `refund_reason`, `remark`, `create_time`, `paid_time`, `shipped_time`, `received_time`, `completed_time`, `closed_time`, `refund_apply_time`, `refund_decision_time`)
VALUES
(1, 'ORD202604020001', 3, 388.00, 1, 1, 0, NULL, 'Seed order for demo', '2026-04-08 10:00:00', '2026-04-08 10:01:00', NULL, NULL, NULL, NULL, NULL, NULL),
(11, 'ORD202604080011', 3, 299.00, 0, 0, 0, NULL, '样例-待付款', '2026-04-08 10:05:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(12, 'ORD202604080012', 3, 89.00, 1, 1, 0, NULL, '样例-待发货', '2026-04-08 10:10:00', '2026-04-08 10:10:30', NULL, NULL, NULL, NULL, NULL, NULL),
(13, 'ORD202604080013', 3, 1299.00, 1, 2, 0, NULL, '样例-待收货', '2026-04-08 10:15:00', '2026-04-08 10:15:30', '2026-04-08 10:16:00', NULL, NULL, NULL, NULL, NULL),
(14, 'ORD202604080014', 3, 180.00, 1, 3, 0, NULL, '样例-待评价（二手）', '2026-04-08 10:20:00', '2026-04-08 10:20:30', '2026-04-08 10:21:00', '2026-04-08 10:22:00', NULL, NULL, NULL, NULL),
(15, 'ORD202604080015', 3, 388.00, 1, 4, 0, NULL, '样例-已完成', '2026-04-08 10:25:00', '2026-04-08 10:25:30', '2026-04-08 10:26:00', '2026-04-08 10:27:00', '2026-04-08 10:28:00', NULL, NULL, NULL),
(16, 'ORD202604080016', 3, 299.00, 0, 9, 0, NULL, '样例-已关闭', '2026-04-08 10:30:00', NULL, NULL, NULL, NULL, '2026-04-08 10:31:00', NULL, NULL),
(17, 'ORD202604080017', 3, 89.00, 1, 1, 1, '尺码不合适', '样例-退款中', '2026-04-08 10:35:00', '2026-04-08 10:35:30', NULL, NULL, NULL, NULL, '2026-04-08 10:36:00', NULL),
(18, 'ORD202604080018', 3, 299.00, 1, 9, 2, '质量问题已退款', '样例-已退款', '2026-04-08 10:40:00', '2026-04-08 10:40:30', '2026-04-08 10:41:00', '2026-04-08 10:42:00', NULL, '2026-04-08 10:44:00', '2026-04-08 10:43:00', '2026-04-08 10:44:00'),
(19, 'ORD202604080019', 3, 1299.00, 1, 2, 3, '不支持退货', '样例-退款被拒绝', '2026-04-08 10:45:00', '2026-04-08 10:45:30', '2026-04-08 10:46:00', NULL, NULL, NULL, '2026-04-08 10:47:00', '2026-04-08 10:48:00'),
(20, 'ORD202604080020', 3, 478.00, 1, 4, 0, NULL, '样例-多商品已完成', '2026-04-08 10:50:00', '2026-04-08 10:50:30', '2026-04-08 10:51:00', '2026-04-08 10:52:00', '2026-04-08 10:53:00', NULL, NULL, NULL),
(21, 'ORD202604080021', 3, 180.00, 1, 1, 0, NULL, '样例-二手待发货', '2026-04-08 10:55:00', '2026-04-08 10:55:30', NULL, NULL, NULL, NULL, NULL, NULL),
(22, 'ORD202604080022', 3, 598.00, 0, 0, 0, NULL, '样例-待付款多件', '2026-04-08 11:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(23, 'ORD202604080023', 3, 178.00, 0, 0, 0, NULL, '样例-待付款（鼠标*2）', '2026-04-08 11:05:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(24, 'ORD202604080024', 3, 1499.00, 1, 1, 0, NULL, '样例-待发货（显示提醒发货）', '2026-04-08 11:10:00', '2026-04-08 11:10:20', NULL, NULL, NULL, NULL, NULL, NULL),
(25, 'ORD202604080025', 3, 299.00, 1, 2, 0, NULL, '样例-待收货（物流中）', '2026-04-08 11:15:00', '2026-04-08 11:15:20', '2026-04-08 11:16:00', NULL, NULL, NULL, NULL, NULL),
(26, 'ORD202604080026', 3, 89.00, 1, 3, 0, NULL, '样例-待评价（可去评价）', '2026-04-08 11:20:00', '2026-04-08 11:20:20', '2026-04-08 11:21:00', '2026-04-08 11:22:00', NULL, NULL, NULL, NULL),
(27, 'ORD202604080027', 3, 1299.00, 1, 4, 0, NULL, '样例-已完成（单件）', '2026-04-08 11:25:00', '2026-04-08 11:25:20', '2026-04-08 11:26:00', '2026-04-08 11:27:00', '2026-04-08 11:28:00', NULL, NULL, NULL),
(28, 'ORD202604080028', 3, 180.00, 0, 9, 0, NULL, '样例-已关闭（未支付取消）', '2026-04-08 11:30:00', NULL, NULL, NULL, NULL, '2026-04-08 11:31:00', NULL, NULL),
(29, 'ORD202604080029', 3, 299.00, 1, 1, 1, '重复下单', '样例-退款申请中（待发货）', '2026-04-08 11:35:00', '2026-04-08 11:35:20', NULL, NULL, NULL, NULL, '2026-04-08 11:36:00', NULL),
(30, 'ORD202604080030', 3, 388.00, 1, 9, 2, '商品损坏已退款', '样例-退款完成后关闭', '2026-04-08 11:40:00', '2026-04-08 11:40:20', '2026-04-08 11:41:00', '2026-04-08 11:42:00', NULL, '2026-04-08 11:44:00', '2026-04-08 11:43:00', '2026-04-08 11:44:00')
ON DUPLICATE KEY UPDATE
  buyer_user_id = VALUES(buyer_user_id),
  total_amount = VALUES(total_amount),
  pay_status = VALUES(pay_status),
  order_status = VALUES(order_status),
  refund_status = VALUES(refund_status),
  refund_reason = VALUES(refund_reason),
  remark = VALUES(remark),
  create_time = VALUES(create_time),
  paid_time = VALUES(paid_time),
  shipped_time = VALUES(shipped_time),
  received_time = VALUES(received_time),
  completed_time = VALUES(completed_time),
  closed_time = VALUES(closed_time),
  refund_apply_time = VALUES(refund_apply_time),
  refund_decision_time = VALUES(refund_decision_time);

INSERT IGNORE INTO `order_item` (`id`, `order_id`, `product_type`, `product_id`, `product_name`, `price`, `quantity`, `status`)
VALUES
(1, 1, 'NEW', 1, 'Mechanical Keyboard K87', 299.00, 1, 1),
(2, 1, 'NEW', 2, 'Wireless Mouse M2', 89.00, 1, 1),
(11, 11, 'NEW', 1, 'Mechanical Keyboard K87', 299.00, 1, 1),
(12, 12, 'NEW', 2, 'Wireless Mouse M2', 89.00, 1, 1),
(13, 13, 'NEW', 3, '27-inch Monitor', 1299.00, 1, 1),
(14, 14, 'SECONDHAND', 2, 'Spare Headphones', 180.00, 1, 1),
(15, 15, 'NEW', 1, 'Mechanical Keyboard K87', 299.00, 1, 1),
(16, 15, 'NEW', 2, 'Wireless Mouse M2', 89.00, 1, 1),
(17, 16, 'NEW', 1, 'Mechanical Keyboard K87', 299.00, 1, 1),
(18, 17, 'NEW', 2, 'Wireless Mouse M2', 89.00, 1, 1),
(19, 18, 'NEW', 1, 'Mechanical Keyboard K87', 299.00, 1, 1),
(20, 19, 'NEW', 3, '27-inch Monitor', 1299.00, 1, 1),
(21, 20, 'NEW', 1, 'Mechanical Keyboard K87', 299.00, 1, 1),
(22, 20, 'NEW', 2, 'Wireless Mouse M2', 89.00, 2, 1),
(23, 21, 'SECONDHAND', 2, 'Spare Headphones', 180.00, 1, 1),
(24, 22, 'NEW', 1, 'Mechanical Keyboard K87', 299.00, 2, 1),
(25, 23, 'NEW', 2, 'Wireless Mouse M2', 89.00, 2, 1),
(26, 24, 'NEW', 3, '27-inch Monitor', 1299.00, 1, 1),
(27, 24, 'NEW', 2, 'Wireless Mouse M2', 89.00, 1, 1),
(28, 24, 'SECONDHAND', 2, 'Spare Headphones', 180.00, 1, 1),
(29, 25, 'NEW', 1, 'Mechanical Keyboard K87', 299.00, 1, 1),
(30, 26, 'NEW', 2, 'Wireless Mouse M2', 89.00, 1, 1),
(31, 27, 'NEW', 3, '27-inch Monitor', 1299.00, 1, 1),
(32, 28, 'SECONDHAND', 2, 'Spare Headphones', 180.00, 1, 1),
(33, 29, 'NEW', 1, 'Mechanical Keyboard K87', 299.00, 1, 1),
(34, 30, 'NEW', 1, 'Mechanical Keyboard K87', 299.00, 1, 1),
(35, 30, 'NEW', 2, 'Wireless Mouse M2', 89.00, 1, 1);

INSERT IGNORE INTO `review` (`id`, `order_id`, `product_type`, `product_id`, `user_id`, `score`, `content`, `status`)
VALUES
(1, 1, 'NEW', 1, 3, 5, 'Good typing feel and fast delivery', 1),
(2, 15, 'NEW', 1, 3, 5, '样例-已完成订单评价', 1),
(3, 20, 'NEW', 1, 3, 4, '键盘手感不错，物流正常', 1),
(4, 20, 'NEW', 2, 3, 5, '鼠标很轻，续航可以', 1);

INSERT IGNORE INTO `review` (`id`, `order_id`, `product_type`, `product_id`, `user_id`, `score`, `content`, `status`, `review_type`)
VALUES
(101, 1, 'NEW', 1, 3, 4, '追评：键盘更稳了，体验很好', 1, 'FOLLOWUP'),
(102, 15, 'NEW', 1, 3, 5, '追评：售后也很给力', 1, 'FOLLOWUP');

INSERT IGNORE INTO `report` (`id`, `reporter_user_id`, `target_type`, `target_id`, `reason`, `status`)
VALUES
(1, 3, 'SECONDHAND_PRODUCT', 2, 'Description does not fully match the item', 0);

SET @seller_id = 2;

INSERT INTO `balance` (user_id, personal_balance, business_balance, version, create_time, update_time)
VALUES (@seller_id, 1280.50, 15680.00, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  personal_balance = 1280.50,
  business_balance = 15680.00,
  update_time = NOW();

DELETE FROM `user_voucher`;
DELETE FROM `voucher`;

INSERT INTO `transaction_record`
  (order_id, user_id, account_type, change_type, amount, balance_after, remark, trade_type, create_time)
VALUES
-- 近期订单收款（经营账户）
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 299.00,  15680.00, '订单结算入账', 'INCOME_BUSINESS', NOW() - INTERVAL 1 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 158.00,  15381.00, '订单结算入账', 'INCOME_BUSINESS', NOW() - INTERVAL 2 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 520.00,  15223.00, '订单结算入账', 'INCOME_BUSINESS', NOW() - INTERVAL 3 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 88.00,   14703.00, '订单结算入账', 'INCOME_BUSINESS', NOW() - INTERVAL 4 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 1280.00, 14615.00, '订单结算入账', 'INCOME_BUSINESS', NOW() - INTERVAL 5 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 366.00,  13335.00, '订单结算入账', 'INCOME_BUSINESS', NOW() - INTERVAL 6 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 99.00,   12969.00, '订单结算入账', 'INCOME_BUSINESS', NOW() - INTERVAL 7 DAY),
-- 退款扣除
(NULL, @seller_id, 'BUSINESS', 'REFUND_ONLY',            -128.00, 12870.00, '仅退款回流',   'REFUND_BACKFLOW', NOW() - INTERVAL 3 DAY),
(NULL, @seller_id, 'BUSINESS', 'REFUND_RETURN',          -299.00, 12998.00, '退货退款回流', 'REFUND_BACKFLOW', NOW() - INTERVAL 8 DAY),
-- 更早的收款记录（上月）
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 688.00,  13297.00, '订单结算入账', 'INCOME_BUSINESS', NOW() - INTERVAL 12 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 450.00,  12609.00, '订单结算入账', 'INCOME_BUSINESS', NOW() - INTERVAL 15 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 1580.00, 12159.00, '订单结算入账', 'INCOME_BUSINESS', NOW() - INTERVAL 18 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 320.00,  10579.00, '订单结算入账', 'INCOME_BUSINESS', NOW() - INTERVAL 22 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 199.00,  10259.00, '订单结算入账', 'INCOME_BUSINESS', NOW() - INTERVAL 25 DAY),
-- 个人账户充值记录
(NULL, @seller_id, 'PERSONAL', 'RECHARGE',                500.00,  1280.50, '钱包充值',     'RECHARGE',        NOW() - INTERVAL 5 DAY),
(NULL, @seller_id, 'PERSONAL', 'RECHARGE',                1000.00, 780.50,  '钱包充值',     'RECHARGE',        NOW() - INTERVAL 20 DAY);

SELECT '=== 优惠券 ===' AS info;
SELECT id, name, type, discount_amount, discount_rate, min_amount, total_count, used_count, status FROM voucher WHERE shop_id = @seller_id;

SELECT '=== 账户余额 ===' AS info;
SELECT * FROM balance WHERE user_id = @seller_id;
 
SELECT '=== 流水记录（最近10条）===' AS info;
SELECT id, account_type, change_type, amount, balance_after, remark, trade_type, create_time
FROM transaction_record WHERE user_id = @seller_id ORDER BY create_time DESC LIMIT 10;


