DELETE FROM `notification`;
DELETE FROM `transaction_record`;
DELETE FROM `order_after_sale_log`;
DELETE FROM `balance`;
DELETE FROM `order_item`;
DELETE FROM `order_info`;
DELETE FROM `product`;
DELETE FROM `shop`;
DELETE FROM `user`;

INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `role`, `status`)
VALUES
  (1, 'uc14_buyer', 'x', 'UC14 Buyer', 'USER', 'NORMAL'),
  (2, 'uc14_admin', 'x', 'UC14 Admin', 'ADMIN', 'NORMAL'),
  (3, 'uc14_other_buyer', 'x', 'UC14 Other Buyer', 'USER', 'NORMAL'),
  (10, 'uc14_seller', 'x', 'UC14 Seller', 'OFFICIAL_SELLER', 'NORMAL'),
  (11, 'uc14_other_seller', 'x', 'UC14 Other Seller', 'OFFICIAL_SELLER', 'NORMAL');

INSERT INTO `shop` (`id`, `owner_user_id`, `name`, `status`)
VALUES
  (501, 10, 'UC14 Seller Shop', 1),
  (502, 11, 'UC14 Other Shop', 1);

INSERT INTO `product` (`id`, `shop_id`, `name`, `price`, `stock`, `status`)
VALUES
  (401, 501, 'UC14 Arrived Product', 60.00, 98, 1),
  (402, 501, 'UC14 Pending Ship Product', 50.00, 99, 1),
  (403, 502, 'UC14 Other Seller Product', 70.00, 99, 1);

INSERT INTO `balance` (`user_id`, `personal_balance`, `business_balance`, `version`)
VALUES
  (1, 0, 0, 0),
  (2, 0, 0, 0),
  (3, 0, 0, 0),
  (10, 0, 0, 0),
  (11, 0, 0, 0);

INSERT INTO `order_info` (`id`, `order_no`, `buyer_user_id`, `total_amount`, `payable_amount`, `pay_status`,
  `order_status`, `refund_status`, `logistics_status`, `logistics_current_index`, `auto_confirm_deadline`,
  `can_refund`, `version`)
VALUES
  (301, 'UC14-ARRIVED-301', 1, 120.00, 120.00, 1, 2, 0, 'ARRIVED', 2,
   TIMESTAMPADD(HOUR, -2, CURRENT_TIMESTAMP), 1, 0),
  (302, 'UC14-PENDING-302', 1, 50.00, 50.00, 1, 1, 0, 'PENDING', 0,
   NULL, 1, 0),
  (304, 'UC14-TRANSIT-304', 1, 60.00, 60.00, 1, 2, 0, 'IN_TRANSIT', 1,
   TIMESTAMPADD(HOUR, 24, CURRENT_TIMESTAMP), 1, 0);

INSERT INTO `order_item` (`id`, `order_id`, `product_type`, `product_id`, `product_name`, `price`, `quantity`, `status`)
VALUES
  (30101, 301, 'NEW', 401, 'UC14 Arrived Product', 60.00, 2, 1),
  (30201, 302, 'NEW', 402, 'UC14 Pending Ship Product', 50.00, 1, 1),
  (30401, 304, 'NEW', 401, 'UC14 In Transit Product', 60.00, 1, 1);
