DELETE FROM `idempotency_record` WHERE `user_id` = 1101;
DELETE FROM `user_block` WHERE `blocker_id` IN (1101, 1102, 1103) OR `blocked_id` IN (1101, 1102, 1103);
DELETE FROM `user_voucher` WHERE `user_id` = 1101 OR `voucher_id` BETWEEN 1110 AND 1113;
DELETE FROM `voucher` WHERE `id` BETWEEN 1110 AND 1113;
DELETE FROM `address` WHERE `id` IN (1101, 1102);
DELETE FROM `order_item` WHERE `order_id` IN (SELECT `id` FROM `order_info` WHERE `buyer_user_id` = 1101);
DELETE FROM `order_info` WHERE `buyer_user_id` = 1101;
DELETE FROM `product` WHERE `id` BETWEEN 1101 AND 1105;
DELETE FROM `shop` WHERE `id` BETWEEN 1101 AND 1103;
DELETE FROM `user` WHERE `id` BETWEEN 1101 AND 1103;

INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `status`, `create_time`, `update_time`)
VALUES
  (1101, 'uc11_buyer', 'x', 'UC11 Buyer', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1102, 'uc11_seller', 'x', 'UC11 Seller', 'OFFICIAL_SELLER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1103, 'uc11_other', 'x', 'UC11 Other', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `shop` (`id`, `owner_user_id`, `name`, `status`)
VALUES
  (1101, 1102, 'UC11 Seller Shop', 1),
  (1102, 1101, 'UC11 Buyer Own Shop', 1),
  (1103, 1103, 'UC11 Other Shop', 1);

INSERT INTO `product` (`id`, `shop_id`, `name`, `price`, `stock`, `status`, `create_time`, `update_time`)
VALUES
  (1101, 1101, 'UC11 Server Price Product', 19.90, 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1102, 1101, 'UC11 Additional Product', 5.00, 8, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1103, 1101, 'UC11 Off Shelf Product', 12.00, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1104, 1101, 'UC11 Low Stock Product', 8.00, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1105, 1102, 'UC11 Buyer Owned Product', 7.00, 5, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `address` (`id`, `user_id`, `receiver_name`, `receiver_phone`, `province`, `city`, `detail_address`, `is_default`)
VALUES
  (1101, 1101, 'UC11 Buyer', '13800001101', 'Guangdong', 'Guangzhou', 'UC11 Test Address', 1),
  (1102, 1103, 'Other User', '13800001103', 'Guangdong', 'Shenzhen', 'Other User Address', 1);

INSERT INTO `voucher` (
  `id`, `issuer_type`, `voucher_type`, `issuer_user_id`, `scope_type`, `shop_id`, `name`, `type`,
  `discount_amount`, `min_amount`, `total_count`, `received_count`, `used_count`, `start_time`, `end_time`, `status`
)
VALUES
  (1110, 1, 1, 1102, 1, 1101, 'UC11 Valid Shop Voucher', 1, 10.00, 50.00, 100, 1, 0, TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, 1, CURRENT_TIMESTAMP), 1),
  (1111, 1, 1, 1102, 1, 1101, 'UC11 Unclaimed Voucher', 1, 5.00, 10.00, 100, 0, 0, TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, 1, CURRENT_TIMESTAMP), 1),
  (1112, 1, 1, 1102, 1, 1101, 'UC11 Threshold Voucher', 1, 10.00, 100.00, 100, 1, 0, TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, 1, CURRENT_TIMESTAMP), 1),
  (1113, 1, 1, 1103, 1, 1103, 'UC11 Other Shop Voucher', 1, 5.00, 10.00, 100, 1, 0, TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, 1, CURRENT_TIMESTAMP), 1);

INSERT INTO `user_voucher` (`id`, `user_id`, `voucher_id`, `status`, `received_time`, `expire_time`)
VALUES
  (1110, 1101, 1110, 1, CURRENT_TIMESTAMP, TIMESTAMPADD(DAY, 1, CURRENT_TIMESTAMP)),
  (1112, 1101, 1112, 1, CURRENT_TIMESTAMP, TIMESTAMPADD(DAY, 1, CURRENT_TIMESTAMP)),
  (1113, 1101, 1113, 1, CURRENT_TIMESTAMP, TIMESTAMPADD(DAY, 1, CURRENT_TIMESTAMP));
