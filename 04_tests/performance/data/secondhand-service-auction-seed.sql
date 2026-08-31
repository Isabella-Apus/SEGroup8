SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `auction_log` WHERE `auction_id` BETWEEN 999001 AND 999010;
DELETE FROM `product_auction` WHERE `id` BETWEEN 999001 AND 999010;
DELETE FROM `outbox_event`
WHERE `aggregate_type` = 'AUCTION' AND CAST(`aggregate_id` AS UNSIGNED) BETWEEN 999001 AND 999010;
DELETE FROM `secondhand_product` WHERE `id` BETWEEN 990041 AND 990050;

INSERT INTO `secondhand_product`
  (`id`, `seller_user_id`, `seller_name_snapshot`, `name`, `cover`, `images`, `description`,
   `origin_price`, `sale_price`, `category_id`, `sub_category_id`, `condition_level`,
   `is_negotiable`, `status`, `risk_status`, `version`, `deleted`)
VALUES
  (990041,950001,'Performance Seller','Performance Auction Item 01','','[]','Reserved formal benchmark item',500.00,300.00,8,801,'LIKE_NEW',0,1,'APPROVED',0,0),
  (990042,950001,'Performance Seller','Performance Auction Item 02','','[]','Reserved formal benchmark item',500.00,300.00,8,801,'LIKE_NEW',0,1,'APPROVED',0,0),
  (990043,950001,'Performance Seller','Performance Auction Item 03','','[]','Reserved formal benchmark item',500.00,300.00,8,801,'LIKE_NEW',0,1,'APPROVED',0,0),
  (990044,950001,'Performance Seller','Performance Auction Item 04','','[]','Reserved formal benchmark item',500.00,300.00,8,801,'LIKE_NEW',0,1,'APPROVED',0,0),
  (990045,950001,'Performance Seller','Performance Auction Item 05','','[]','Reserved formal benchmark item',500.00,300.00,8,801,'LIKE_NEW',0,1,'APPROVED',0,0),
  (990046,950001,'Performance Seller','Performance Auction Item 06','','[]','Reserved formal benchmark item',500.00,300.00,8,801,'LIKE_NEW',0,1,'APPROVED',0,0),
  (990047,950001,'Performance Seller','Performance Auction Item 07','','[]','Reserved formal benchmark item',500.00,300.00,8,801,'LIKE_NEW',0,1,'APPROVED',0,0),
  (990048,950001,'Performance Seller','Performance Auction Item 08','','[]','Reserved formal benchmark item',500.00,300.00,8,801,'LIKE_NEW',0,1,'APPROVED',0,0),
  (990049,950001,'Performance Seller','Performance Auction Item 09','','[]','Reserved formal benchmark item',500.00,300.00,8,801,'LIKE_NEW',0,1,'APPROVED',0,0),
  (990050,950001,'Performance Seller','Performance Auction Item 10','','[]','Reserved formal benchmark item',500.00,300.00,8,801,'LIKE_NEW',0,1,'APPROVED',0,0);

INSERT INTO `product_auction`
  (`id`, `product_id`, `seller_user_id`, `start_price`, `increment_amount`, `current_price`,
   `current_bidder_user_id`, `start_time`, `end_time`, `status`, `settled_order_id`, `version`)
VALUES
  (999001,990041,950001,300.00,5.00,300.00,NULL,DATE_SUB(NOW(),INTERVAL 1 MINUTE),DATE_ADD(NOW(),INTERVAL 2 HOUR),'ONGOING',NULL,0),
  (999002,990042,950001,300.00,5.00,300.00,NULL,DATE_SUB(NOW(),INTERVAL 1 MINUTE),DATE_ADD(NOW(),INTERVAL 2 HOUR),'ONGOING',NULL,0),
  (999003,990043,950001,300.00,5.00,300.00,NULL,DATE_SUB(NOW(),INTERVAL 1 MINUTE),DATE_ADD(NOW(),INTERVAL 2 HOUR),'ONGOING',NULL,0),
  (999004,990044,950001,300.00,5.00,300.00,NULL,DATE_SUB(NOW(),INTERVAL 1 MINUTE),DATE_ADD(NOW(),INTERVAL 2 HOUR),'ONGOING',NULL,0),
  (999005,990045,950001,300.00,5.00,300.00,NULL,DATE_SUB(NOW(),INTERVAL 1 MINUTE),DATE_ADD(NOW(),INTERVAL 2 HOUR),'ONGOING',NULL,0),
  (999006,990046,950001,300.00,5.00,300.00,NULL,DATE_SUB(NOW(),INTERVAL 1 MINUTE),DATE_ADD(NOW(),INTERVAL 2 HOUR),'ONGOING',NULL,0),
  (999007,990047,950001,300.00,5.00,300.00,NULL,DATE_SUB(NOW(),INTERVAL 1 MINUTE),DATE_ADD(NOW(),INTERVAL 2 HOUR),'ONGOING',NULL,0),
  (999008,990048,950001,300.00,5.00,300.00,NULL,DATE_SUB(NOW(),INTERVAL 1 MINUTE),DATE_ADD(NOW(),INTERVAL 2 HOUR),'ONGOING',NULL,0),
  (999009,990049,950001,300.00,5.00,300.00,NULL,DATE_SUB(NOW(),INTERVAL 1 MINUTE),DATE_ADD(NOW(),INTERVAL 2 HOUR),'ONGOING',NULL,0),
  (999010,990050,950001,300.00,5.00,300.00,NULL,DATE_SUB(NOW(),INTERVAL 1 MINUTE),DATE_ADD(NOW(),INTERVAL 2 HOUR),'ONGOING',NULL,0);

SET FOREIGN_KEY_CHECKS = 1;
