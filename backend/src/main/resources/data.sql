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
(3, '家居生活', NULL, 3, 1),
(4, '美妆个护', NULL, 4, 1),
(5, '运动户外', NULL, 5, 1),
(6, '图书音像', NULL, 6, 1),
(7, '美食', NULL, 7, 1),
(8, '其他', NULL, 8, 1),
(101, '手机', 1, 1, 1),
(102, '电脑/平板', 1, 2, 1),
(103, '摄影摄像', 1, 3, 1),
(104, '影音娱乐', 1, 4, 1),
(105, '智能穿戴', 1, 5, 1),
(201, '女装', 2, 1, 1),
(202, '男装', 2, 2, 1),
(203, '运动服饰', 2, 3, 1),
(204, '鞋包', 2, 4, 1),
(205, '配饰', 2, 5, 1),
(301, '家具家装', 3, 1, 1),
(302, '厨房用具', 3, 2, 1),
(303, '居家日用', 3, 3, 1),
(304, '家用电器', 3, 4, 1),
(305, '收纳整理', 3, 5, 1),
(401, '面部护肤', 4, 1, 1),
(402, '彩妆', 4, 2, 1),
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
(604, '办公文具', 6, 4, 1),
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
(1, 3, 'Used Bicycle', '', 'Gently used and works well', 1200.00, 650.00, 5, 504, '9鎴愭柊', 1, 1),
(2, 3, 'Spare Headphones', '', 'Minor usage marks, fully functional', 399.00, 180.00, 1, 104, '8鎴愭柊鍙婁互涓?, 0, 1);

INSERT INTO `order_info` (`id`, `order_no`, `buyer_user_id`, `total_amount`, `pay_status`, `order_status`, `refund_status`, `refund_reason`, `remark`, `create_time`, `paid_time`, `shipped_time`, `received_time`, `completed_time`, `closed_time`, `refund_apply_time`, `refund_decision_time`)
VALUES
(1, 'ORD202604020001', 3, 388.00, 1, 1, 0, NULL, 'Seed order for demo', '2026-04-08 10:00:00', '2026-04-08 10:01:00', NULL, NULL, NULL, NULL, NULL, NULL),
(11, 'ORD202604080011', 3, 299.00, 0, 0, 0, NULL, '鏍蜂緥-寰呬粯娆?, '2026-04-08 10:05:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(12, 'ORD202604080012', 3, 89.00, 1, 1, 0, NULL, '鏍蜂緥-寰呭彂璐?, '2026-04-08 10:10:00', '2026-04-08 10:10:30', NULL, NULL, NULL, NULL, NULL, NULL),
(13, 'ORD202604080013', 3, 1299.00, 1, 2, 0, NULL, '鏍蜂緥-寰呮敹璐?, '2026-04-08 10:15:00', '2026-04-08 10:15:30', '2026-04-08 10:16:00', NULL, NULL, NULL, NULL, NULL),
(14, 'ORD202604080014', 3, 180.00, 1, 3, 0, NULL, '鏍蜂緥-寰呰瘎浠凤紙浜屾墜锛?, '2026-04-08 10:20:00', '2026-04-08 10:20:30', '2026-04-08 10:21:00', '2026-04-08 10:22:00', NULL, NULL, NULL, NULL),
(15, 'ORD202604080015', 3, 388.00, 1, 4, 0, NULL, '鏍蜂緥-宸插畬鎴?, '2026-04-08 10:25:00', '2026-04-08 10:25:30', '2026-04-08 10:26:00', '2026-04-08 10:27:00', '2026-04-08 10:28:00', NULL, NULL, NULL),
(16, 'ORD202604080016', 3, 299.00, 0, 9, 0, NULL, '鏍蜂緥-宸插叧闂?, '2026-04-08 10:30:00', NULL, NULL, NULL, NULL, '2026-04-08 10:31:00', NULL, NULL),
(17, 'ORD202604080017', 3, 89.00, 1, 1, 1, '灏虹爜涓嶅悎閫?, '鏍蜂緥-閫€娆句腑', '2026-04-08 10:35:00', '2026-04-08 10:35:30', NULL, NULL, NULL, NULL, '2026-04-08 10:36:00', NULL),
(18, 'ORD202604080018', 3, 299.00, 1, 9, 2, '璐ㄩ噺闂宸查€€娆?, '鏍蜂緥-宸查€€娆?, '2026-04-08 10:40:00', '2026-04-08 10:40:30', '2026-04-08 10:41:00', '2026-04-08 10:42:00', NULL, '2026-04-08 10:44:00', '2026-04-08 10:43:00', '2026-04-08 10:44:00'),
(19, 'ORD202604080019', 3, 1299.00, 1, 2, 3, '涓嶆敮鎸侀€€璐?, '鏍蜂緥-閫€娆捐鎷掔粷', '2026-04-08 10:45:00', '2026-04-08 10:45:30', '2026-04-08 10:46:00', NULL, NULL, NULL, '2026-04-08 10:47:00', '2026-04-08 10:48:00'),
(20, 'ORD202604080020', 3, 478.00, 1, 4, 0, NULL, '鏍蜂緥-澶氬晢鍝佸凡瀹屾垚', '2026-04-08 10:50:00', '2026-04-08 10:50:30', '2026-04-08 10:51:00', '2026-04-08 10:52:00', '2026-04-08 10:53:00', NULL, NULL, NULL),
(21, 'ORD202604080021', 3, 180.00, 1, 1, 0, NULL, '鏍蜂緥-浜屾墜寰呭彂璐?, '2026-04-08 10:55:00', '2026-04-08 10:55:30', NULL, NULL, NULL, NULL, NULL, NULL),
(22, 'ORD202604080022', 3, 598.00, 0, 0, 0, NULL, '鏍蜂緥-寰呬粯娆惧浠?, '2026-04-08 11:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(23, 'ORD202604080023', 3, 178.00, 0, 0, 0, NULL, '鏍蜂緥-寰呬粯娆撅紙榧犳爣*2锛?, '2026-04-08 11:05:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(24, 'ORD202604080024', 3, 1499.00, 1, 1, 0, NULL, '鏍蜂緥-寰呭彂璐э紙鏄剧ず鎻愰啋鍙戣揣锛?, '2026-04-08 11:10:00', '2026-04-08 11:10:20', NULL, NULL, NULL, NULL, NULL, NULL),
(25, 'ORD202604080025', 3, 299.00, 1, 2, 0, NULL, '鏍蜂緥-寰呮敹璐э紙鐗╂祦涓級', '2026-04-08 11:15:00', '2026-04-08 11:15:20', '2026-04-08 11:16:00', NULL, NULL, NULL, NULL, NULL),
(26, 'ORD202604080026', 3, 89.00, 1, 3, 0, NULL, '鏍蜂緥-寰呰瘎浠凤紙鍙幓璇勪环锛?, '2026-04-08 11:20:00', '2026-04-08 11:20:20', '2026-04-08 11:21:00', '2026-04-08 11:22:00', NULL, NULL, NULL, NULL),
(27, 'ORD202604080027', 3, 1299.00, 1, 4, 0, NULL, '鏍蜂緥-宸插畬鎴愶紙鍗曚欢锛?, '2026-04-08 11:25:00', '2026-04-08 11:25:20', '2026-04-08 11:26:00', '2026-04-08 11:27:00', '2026-04-08 11:28:00', NULL, NULL, NULL),
(28, 'ORD202604080028', 3, 180.00, 0, 9, 0, NULL, '鏍蜂緥-宸插叧闂紙鏈敮浠樺彇娑堬級', '2026-04-08 11:30:00', NULL, NULL, NULL, NULL, '2026-04-08 11:31:00', NULL, NULL),
(29, 'ORD202604080029', 3, 299.00, 1, 1, 1, '閲嶅涓嬪崟', '鏍蜂緥-閫€娆剧敵璇蜂腑锛堝緟鍙戣揣锛?, '2026-04-08 11:35:00', '2026-04-08 11:35:20', NULL, NULL, NULL, NULL, '2026-04-08 11:36:00', NULL),
(30, 'ORD202604080030', 3, 388.00, 1, 9, 2, '鍟嗗搧鎹熷潖宸查€€娆?, '鏍蜂緥-閫€娆惧畬鎴愬悗鍏抽棴', '2026-04-08 11:40:00', '2026-04-08 11:40:20', '2026-04-08 11:41:00', '2026-04-08 11:42:00', NULL, '2026-04-08 11:44:00', '2026-04-08 11:43:00', '2026-04-08 11:44:00')
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
(2, 15, 'NEW', 1, 3, 5, '鏍蜂緥-宸插畬鎴愯鍗曡瘎浠?, 1),
(3, 20, 'NEW', 1, 3, 4, '閿洏鎵嬫劅涓嶉敊锛岀墿娴佹甯?, 1),
(4, 20, 'NEW', 2, 3, 5, '榧犳爣寰堣交锛岀画鑸彲浠?, 1);

INSERT IGNORE INTO `review` (`id`, `order_id`, `product_type`, `product_id`, `user_id`, `score`, `content`, `status`, `review_type`)
VALUES
(101, 1, 'NEW', 1, 3, 4, '杩借瘎锛氶敭鐩樻洿绋充簡锛屼綋楠屽緢濂?, 1, 'FOLLOWUP'),
(102, 15, 'NEW', 1, 3, 5, '杩借瘎锛氬敭鍚庝篃寰堢粰鍔?, 1, 'FOLLOWUP');

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
-- 杩戞湡璁㈠崟鏀舵锛堢粡钀ヨ处鎴凤級
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 299.00,  15680.00, '璁㈠崟缁撶畻鍏ヨ处', 'INCOME_BUSINESS', NOW() - INTERVAL 1 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 158.00,  15381.00, '璁㈠崟缁撶畻鍏ヨ处', 'INCOME_BUSINESS', NOW() - INTERVAL 2 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 520.00,  15223.00, '璁㈠崟缁撶畻鍏ヨ处', 'INCOME_BUSINESS', NOW() - INTERVAL 3 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 88.00,   14703.00, '璁㈠崟缁撶畻鍏ヨ处', 'INCOME_BUSINESS', NOW() - INTERVAL 4 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 1280.00, 14615.00, '璁㈠崟缁撶畻鍏ヨ处', 'INCOME_BUSINESS', NOW() - INTERVAL 5 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 366.00,  13335.00, '璁㈠崟缁撶畻鍏ヨ处', 'INCOME_BUSINESS', NOW() - INTERVAL 6 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 99.00,   12969.00, '璁㈠崟缁撶畻鍏ヨ处', 'INCOME_BUSINESS', NOW() - INTERVAL 7 DAY),
-- 閫€娆炬墸闄?(NULL, @seller_id, 'BUSINESS', 'REFUND_ONLY',            -128.00, 12870.00, '浠呴€€娆惧洖娴?,   'REFUND_BACKFLOW', NOW() - INTERVAL 3 DAY),
(NULL, @seller_id, 'BUSINESS', 'REFUND_RETURN',          -299.00, 12998.00, '閫€璐ч€€娆惧洖娴?, 'REFUND_BACKFLOW', NOW() - INTERVAL 8 DAY),
-- 鏇存棭鐨勬敹娆捐褰曪紙涓婃湀锛?(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 688.00,  13297.00, '璁㈠崟缁撶畻鍏ヨ处', 'INCOME_BUSINESS', NOW() - INTERVAL 12 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 450.00,  12609.00, '璁㈠崟缁撶畻鍏ヨ处', 'INCOME_BUSINESS', NOW() - INTERVAL 15 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 1580.00, 12159.00, '璁㈠崟缁撶畻鍏ヨ处', 'INCOME_BUSINESS', NOW() - INTERVAL 18 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 320.00,  10579.00, '璁㈠崟缁撶畻鍏ヨ处', 'INCOME_BUSINESS', NOW() - INTERVAL 22 DAY),
(NULL, @seller_id, 'BUSINESS', 'ESCROW_RELEASE_BUSINESS', 199.00,  10259.00, '璁㈠崟缁撶畻鍏ヨ处', 'INCOME_BUSINESS', NOW() - INTERVAL 25 DAY),
-- 涓汉璐︽埛鍏呭€艰褰?(NULL, @seller_id, 'PERSONAL', 'RECHARGE',                500.00,  1280.50, '閽卞寘鍏呭€?,     'RECHARGE',        NOW() - INTERVAL 5 DAY),
(NULL, @seller_id, 'PERSONAL', 'RECHARGE',                1000.00, 780.50,  '閽卞寘鍏呭€?,     'RECHARGE',        NOW() - INTERVAL 20 DAY);

SELECT '=== 浼樻儬鍒?===' AS info;
SELECT id, name, type, discount_amount, discount_rate, min_amount, total_count, used_count, status FROM voucher WHERE shop_id = @seller_id;

SELECT '=== 璐︽埛浣欓 ===' AS info;
SELECT * FROM balance WHERE user_id = @seller_id;
 
SELECT '=== 娴佹按璁板綍锛堟渶杩?0鏉★級===' AS info;
SELECT id, account_type, change_type, amount, balance_after, remark, trade_type, create_time
FROM transaction_record WHERE user_id = @seller_id ORDER BY create_time DESC LIMIT 10;


