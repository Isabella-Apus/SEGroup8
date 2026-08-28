DELETE FROM `chat_message`
WHERE `conversation_id` IN (
  SELECT `id` FROM `chat_conversation`
  WHERE `buyer_user_id` IN (2401, 2402, 2403)
     OR `seller_user_id` IN (2401, 2402, 2403)
);
DELETE FROM `chat_conversation`
WHERE `buyer_user_id` IN (2401, 2402, 2403)
   OR `seller_user_id` IN (2401, 2402, 2403);
DELETE FROM `notification` WHERE `user_id` IN (2401, 2402, 2403);
DELETE FROM `user_block`
WHERE `blocker_id` IN (2401, 2402, 2403)
   OR `blocked_id` IN (2401, 2402, 2403);
DELETE FROM `product` WHERE `id` = 2401;
DELETE FROM `shop` WHERE `id` = 2401;
DELETE FROM `user` WHERE `id` IN (2401, 2402, 2403);

INSERT INTO `user`
  (`id`, `username`, `password`, `nickname`, `role`, `status`, `create_time`, `update_time`)
VALUES
  (2401, 'uc24_buyer', 'x', 'UC24 Buyer', 'USER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2402, 'uc24_seller', 'x', 'UC24 Seller', 'OFFICIAL_SELLER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2403, 'uc24_outsider', 'x', 'UC24 Outsider', 'USER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `shop` (`id`, `owner_user_id`, `name`, `status`, `create_time`, `update_time`)
VALUES (2401, 2402, 'UC24 Shop', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `product`
  (`id`, `shop_id`, `name`, `price`, `stock`, `status`, `create_time`, `update_time`)
VALUES (2401, 2401, 'UC24 Product', 24.00, 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
