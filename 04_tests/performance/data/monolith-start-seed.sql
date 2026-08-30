SET FOREIGN_KEY_CHECKS = 0;

DROP TEMPORARY TABLE IF EXISTS perf_order_ids;
CREATE TEMPORARY TABLE perf_order_ids (`id` BIGINT PRIMARY KEY);
INSERT IGNORE INTO perf_order_ids (`id`)
SELECT DISTINCT `order_id`
FROM `order_item`
WHERE (`product_type` = 'NEW' AND `product_id` = 980001)
   OR (`product_type` = 'SECONDHAND' AND `product_id` BETWEEN 990001 AND 990050);

DELETE FROM `logistics_trace` WHERE `order_id` IN (SELECT `id` FROM perf_order_ids);
DELETE FROM `order_after_sale_log` WHERE `order_id` IN (SELECT `id` FROM perf_order_ids);
DELETE FROM `review` WHERE `order_id` IN (SELECT `id` FROM perf_order_ids);
DELETE FROM `order_item` WHERE `order_id` IN (SELECT `id` FROM perf_order_ids);
DELETE FROM `order_info` WHERE `id` IN (SELECT `id` FROM perf_order_ids);
DROP TEMPORARY TABLE perf_order_ids;

DELETE FROM `auction_log` WHERE `auction_id` BETWEEN 999001 AND 999010;
DELETE FROM `product_auction` WHERE `id` BETWEEN 999001 AND 999010;
DELETE FROM `secondhand_product` WHERE `id` BETWEEN 990001 AND 990050;
DELETE FROM `product` WHERE `id` = 980001;
DELETE FROM `address` WHERE `id` BETWEEN 970001 AND 970002;
DELETE FROM `shop` WHERE `id` = 960001;
DELETE FROM `notification` WHERE `user_id` BETWEEN 950001 AND 950021;
DELETE FROM `transaction_record` WHERE `user_id` BETWEEN 950001 AND 950021;
DELETE FROM `balance` WHERE `user_id` BETWEEN 950001 AND 950021;
DELETE FROM `user` WHERE `id` BETWEEN 950001 AND 950021;

INSERT INTO `user`
  (`id`, `username`, `password`, `nickname`, `avatar`, `phone`, `email`, `role`, `status`, `credit_score`)
VALUES
  (950001, 'perf_seller', 'perf_seller_2026', 'Performance Seller', '', '13950000001', 'perf-seller@example.test', 'OFFICIAL_SELLER', 'NORMAL', 100),
  (950002, 'perf_buyer', 'perf_buyer_2026', 'Performance Buyer', '', '13950000002', 'perf-buyer@example.test', 'USER', 'NORMAL', 100),
  (950003, 'perf_buyer_2', 'perf_buyer_2_2026', 'Performance Buyer 2', '', '13950000003', 'perf-buyer-2@example.test', 'USER', 'NORMAL', 100),
  (950004, 'perf_bidder_03', 'perf_bidder_2026', 'Performance Bidder 03', '', '13950000004', 'perf-bidder-03@example.test', 'USER', 'NORMAL', 100),
  (950005, 'perf_bidder_04', 'perf_bidder_2026', 'Performance Bidder 04', '', '13950000005', 'perf-bidder-04@example.test', 'USER', 'NORMAL', 100),
  (950006, 'perf_bidder_05', 'perf_bidder_2026', 'Performance Bidder 05', '', '13950000006', 'perf-bidder-05@example.test', 'USER', 'NORMAL', 100),
  (950007, 'perf_bidder_06', 'perf_bidder_2026', 'Performance Bidder 06', '', '13950000007', 'perf-bidder-06@example.test', 'USER', 'NORMAL', 100),
  (950008, 'perf_bidder_07', 'perf_bidder_2026', 'Performance Bidder 07', '', '13950000008', 'perf-bidder-07@example.test', 'USER', 'NORMAL', 100),
  (950009, 'perf_bidder_08', 'perf_bidder_2026', 'Performance Bidder 08', '', '13950000009', 'perf-bidder-08@example.test', 'USER', 'NORMAL', 100),
  (950010, 'perf_bidder_09', 'perf_bidder_2026', 'Performance Bidder 09', '', '13950000010', 'perf-bidder-09@example.test', 'USER', 'NORMAL', 100),
  (950011, 'perf_bidder_10', 'perf_bidder_2026', 'Performance Bidder 10', '', '13950000011', 'perf-bidder-10@example.test', 'USER', 'NORMAL', 100),
  (950012, 'perf_bidder_11', 'perf_bidder_2026', 'Performance Bidder 11', '', '13950000012', 'perf-bidder-11@example.test', 'USER', 'NORMAL', 100),
  (950013, 'perf_bidder_12', 'perf_bidder_2026', 'Performance Bidder 12', '', '13950000013', 'perf-bidder-12@example.test', 'USER', 'NORMAL', 100),
  (950014, 'perf_bidder_13', 'perf_bidder_2026', 'Performance Bidder 13', '', '13950000014', 'perf-bidder-13@example.test', 'USER', 'NORMAL', 100),
  (950015, 'perf_bidder_14', 'perf_bidder_2026', 'Performance Bidder 14', '', '13950000015', 'perf-bidder-14@example.test', 'USER', 'NORMAL', 100),
  (950016, 'perf_bidder_15', 'perf_bidder_2026', 'Performance Bidder 15', '', '13950000016', 'perf-bidder-15@example.test', 'USER', 'NORMAL', 100),
  (950017, 'perf_bidder_16', 'perf_bidder_2026', 'Performance Bidder 16', '', '13950000017', 'perf-bidder-16@example.test', 'USER', 'NORMAL', 100),
  (950018, 'perf_bidder_17', 'perf_bidder_2026', 'Performance Bidder 17', '', '13950000018', 'perf-bidder-17@example.test', 'USER', 'NORMAL', 100),
  (950019, 'perf_bidder_18', 'perf_bidder_2026', 'Performance Bidder 18', '', '13950000019', 'perf-bidder-18@example.test', 'USER', 'NORMAL', 100),
  (950020, 'perf_bidder_19', 'perf_bidder_2026', 'Performance Bidder 19', '', '13950000020', 'perf-bidder-19@example.test', 'USER', 'NORMAL', 100),
  (950021, 'perf_bidder_20', 'perf_bidder_2026', 'Performance Bidder 20', '', '13950000021', 'perf-bidder-20@example.test', 'USER', 'NORMAL', 100);

INSERT INTO `balance` (`user_id`, `personal_balance`, `business_balance`, `version`)
VALUES
  (950001, 1000000.00, 1000000.00, 0),
  (950002, 1000000.00, 0.00, 0),
  (950003, 1000000.00, 0.00, 0),
  (950004, 1000000.00, 0.00, 0),
  (950005, 1000000.00, 0.00, 0),
  (950006, 1000000.00, 0.00, 0),
  (950007, 1000000.00, 0.00, 0),
  (950008, 1000000.00, 0.00, 0),
  (950009, 1000000.00, 0.00, 0),
  (950010, 1000000.00, 0.00, 0),
  (950011, 1000000.00, 0.00, 0),
  (950012, 1000000.00, 0.00, 0),
  (950013, 1000000.00, 0.00, 0),
  (950014, 1000000.00, 0.00, 0),
  (950015, 1000000.00, 0.00, 0),
  (950016, 1000000.00, 0.00, 0),
  (950017, 1000000.00, 0.00, 0),
  (950018, 1000000.00, 0.00, 0),
  (950019, 1000000.00, 0.00, 0),
  (950020, 1000000.00, 0.00, 0),
  (950021, 1000000.00, 0.00, 0);

INSERT INTO `shop` (`id`, `owner_user_id`, `name`, `logo`, `description`, `status`)
VALUES (960001, 950001, 'Performance Baseline Shop', '', 'Reserved for repeatable k6 tests', 1);

INSERT INTO `address`
  (`id`, `user_id`, `receiver_name`, `receiver_phone`, `province`, `city`, `detail_address`, `is_default`)
VALUES
  (970001, 950002, 'Performance Buyer', '13950000002', 'Test Province', 'Test City', 'No. 1 Baseline Road', 1),
  (970002, 950003, 'Performance Buyer 2', '13950000003', 'Test Province', 'Test City', 'No. 2 Baseline Road', 1);

INSERT INTO `product`
  (`id`, `shop_id`, `name`, `cover`, `description`, `price`, `category_id`, `stock`, `status`)
VALUES
  (980001, 960001, 'Performance Demo Keyboard', '', 'Reserved for new-order baseline', 299.00, 1, 10000, 1);

INSERT INTO `secondhand_product`
  (`id`, `seller_user_id`, `name`, `cover`, `images`, `description`, `origin_price`, `sale_price`, `category_id`, `sub_category_id`, `condition_level`, `is_negotiable`, `status`)
VALUES
  (990001,950001,'Performance Secondhand 01','','[]','Reserved k6 item',300.00,180.00,8,801,'95%',1,1),
  (990002,950001,'Performance Secondhand 02','','[]','Reserved k6 item',300.00,181.00,8,801,'95%',1,1),
  (990003,950001,'Performance Secondhand 03','','[]','Reserved k6 item',300.00,182.00,8,801,'95%',1,1),
  (990004,950001,'Performance Secondhand 04','','[]','Reserved k6 item',300.00,183.00,8,801,'95%',1,1),
  (990005,950001,'Performance Secondhand 05','','[]','Reserved k6 item',300.00,184.00,8,801,'95%',1,1),
  (990006,950001,'Performance Secondhand 06','','[]','Reserved k6 item',300.00,185.00,8,801,'95%',1,1),
  (990007,950001,'Performance Secondhand 07','','[]','Reserved k6 item',300.00,186.00,8,801,'95%',1,1),
  (990008,950001,'Performance Secondhand 08','','[]','Reserved k6 item',300.00,187.00,8,801,'95%',1,1),
  (990009,950001,'Performance Secondhand 09','','[]','Reserved k6 item',300.00,188.00,8,801,'95%',1,1),
  (990010,950001,'Performance Secondhand 10','','[]','Reserved k6 item',300.00,189.00,8,801,'95%',1,1),
  (990011,950001,'Performance Secondhand 11','','[]','Reserved k6 item',300.00,190.00,8,801,'90%',1,1),
  (990012,950001,'Performance Secondhand 12','','[]','Reserved k6 item',300.00,191.00,8,801,'90%',1,1),
  (990013,950001,'Performance Secondhand 13','','[]','Reserved k6 item',300.00,192.00,8,801,'90%',1,1),
  (990014,950001,'Performance Secondhand 14','','[]','Reserved k6 item',300.00,193.00,8,801,'90%',1,1),
  (990015,950001,'Performance Secondhand 15','','[]','Reserved k6 item',300.00,194.00,8,801,'90%',1,1),
  (990016,950001,'Performance Secondhand 16','','[]','Reserved k6 item',300.00,195.00,8,801,'90%',1,1),
  (990017,950001,'Performance Secondhand 17','','[]','Reserved k6 item',300.00,196.00,8,801,'90%',1,1),
  (990018,950001,'Performance Secondhand 18','','[]','Reserved k6 item',300.00,197.00,8,801,'90%',1,1),
  (990019,950001,'Performance Secondhand 19','','[]','Reserved k6 item',300.00,198.00,8,801,'90%',1,1),
  (990020,950001,'Performance Secondhand 20','','[]','Reserved k6 item',300.00,199.00,8,801,'90%',1,1),
  (990021,950001,'Performance Secondhand 21','','[]','Reserved k6 item',300.00,200.00,8,801,'85%',1,1),
  (990022,950001,'Performance Secondhand 22','','[]','Reserved k6 item',300.00,201.00,8,801,'85%',1,1),
  (990023,950001,'Performance Secondhand 23','','[]','Reserved k6 item',300.00,202.00,8,801,'85%',1,1),
  (990024,950001,'Performance Secondhand 24','','[]','Reserved k6 item',300.00,203.00,8,801,'85%',1,1),
  (990025,950001,'Performance Secondhand 25','','[]','Reserved k6 item',300.00,204.00,8,801,'85%',1,1),
  (990026,950001,'Performance Secondhand 26','','[]','Reserved k6 item',300.00,205.00,8,801,'85%',1,1),
  (990027,950001,'Performance Secondhand 27','','[]','Reserved k6 item',300.00,206.00,8,801,'85%',1,1),
  (990028,950001,'Performance Secondhand 28','','[]','Reserved k6 item',300.00,207.00,8,801,'85%',1,1),
  (990029,950001,'Performance Secondhand 29','','[]','Reserved k6 item',300.00,208.00,8,801,'85%',1,1),
  (990030,950001,'Performance Secondhand 30','','[]','Reserved k6 item',300.00,209.00,8,801,'85%',1,1),
  (990031,950001,'Performance Secondhand 31','','[]','Reserved k6 item',300.00,210.00,8,801,'80%',1,1),
  (990032,950001,'Performance Secondhand 32','','[]','Reserved k6 item',300.00,211.00,8,801,'80%',1,1),
  (990033,950001,'Performance Secondhand 33','','[]','Reserved k6 item',300.00,212.00,8,801,'80%',1,1),
  (990034,950001,'Performance Secondhand 34','','[]','Reserved k6 item',300.00,213.00,8,801,'80%',1,1),
  (990035,950001,'Performance Secondhand 35','','[]','Reserved k6 item',300.00,214.00,8,801,'80%',1,1),
  (990036,950001,'Performance Secondhand 36','','[]','Reserved k6 item',300.00,215.00,8,801,'80%',1,1),
  (990037,950001,'Performance Secondhand 37','','[]','Reserved k6 item',300.00,216.00,8,801,'80%',1,1),
  (990038,950001,'Performance Secondhand 38','','[]','Reserved k6 item',300.00,217.00,8,801,'80%',1,1),
  (990039,950001,'Performance Secondhand 39','','[]','Reserved k6 item',300.00,218.00,8,801,'80%',1,1),
  (990040,950001,'Performance Secondhand 40','','[]','Reserved k6 item',300.00,219.00,8,801,'80%',1,1),
  (990041,950001,'Performance Auction Item 01','','[]','Reserved k6 auction item',500.00,300.00,8,801,'95%',0,1),
  (990042,950001,'Performance Auction Item 02','','[]','Reserved k6 auction item',500.00,300.00,8,801,'95%',0,1),
  (990043,950001,'Performance Auction Item 03','','[]','Reserved k6 auction item',500.00,300.00,8,801,'95%',0,1),
  (990044,950001,'Performance Auction Item 04','','[]','Reserved k6 auction item',500.00,300.00,8,801,'95%',0,1),
  (990045,950001,'Performance Auction Item 05','','[]','Reserved k6 auction item',500.00,300.00,8,801,'95%',0,1),
  (990046,950001,'Performance Auction Item 06','','[]','Reserved k6 auction item',500.00,300.00,8,801,'95%',0,1),
  (990047,950001,'Performance Auction Item 07','','[]','Reserved k6 auction item',500.00,300.00,8,801,'95%',0,1),
  (990048,950001,'Performance Auction Item 08','','[]','Reserved k6 auction item',500.00,300.00,8,801,'95%',0,1),
  (990049,950001,'Performance Auction Item 09','','[]','Reserved k6 auction item',500.00,300.00,8,801,'95%',0,1),
  (990050,950001,'Performance Auction Item 10','','[]','Reserved k6 auction item',500.00,300.00,8,801,'95%',0,1);

INSERT INTO `product_auction`
  (`id`, `product_id`, `seller_user_id`, `start_price`, `increment_amount`, `current_price`, `current_bidder_user_id`, `start_time`, `end_time`, `status`, `settled_order_id`, `version`)
VALUES
  (999001, 990041, 950001, 300.00, 5.00, 300.00, NULL, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'ONGOING', NULL, 0),
  (999002, 990042, 950001, 300.00, 5.00, 300.00, NULL, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'ONGOING', NULL, 0),
  (999003, 990043, 950001, 300.00, 5.00, 300.00, NULL, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'ONGOING', NULL, 0),
  (999004, 990044, 950001, 300.00, 5.00, 300.00, NULL, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'ONGOING', NULL, 0),
  (999005, 990045, 950001, 300.00, 5.00, 300.00, NULL, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'ONGOING', NULL, 0),
  (999006, 990046, 950001, 300.00, 5.00, 300.00, NULL, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'ONGOING', NULL, 0),
  (999007, 990047, 950001, 300.00, 5.00, 300.00, NULL, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'ONGOING', NULL, 0),
  (999008, 990048, 950001, 300.00, 5.00, 300.00, NULL, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'ONGOING', NULL, 0),
  (999009, 990049, 950001, 300.00, 5.00, 300.00, NULL, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'ONGOING', NULL, 0),
  (999010, 990050, 950001, 300.00, 5.00, 300.00, NULL, DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'ONGOING', NULL, 0);

SET FOREIGN_KEY_CHECKS = 1;
