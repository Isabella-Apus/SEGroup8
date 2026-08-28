DELETE FROM idempotency_record;
DELETE FROM notification;
DELETE FROM transaction_record;
DELETE FROM product_negotiation;
DELETE FROM auction_log;
DELETE FROM product_auction;
DELETE FROM user_block;
DELETE FROM order_item;
DELETE FROM order_info;
DELETE FROM address;
DELETE FROM secondhand_product;
DELETE FROM user;

INSERT INTO user (id, username, password, role, status, create_time, update_time) VALUES
  (1701, 'uc17_seller', 'x', 'USER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1702, 'uc17_buyer_a', 'x', 'USER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1703, 'uc17_buyer_b', 'x', 'USER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1704, 'uc17_other', 'x', 'USER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO address
  (id, user_id, receiver_name, receiver_phone, province, city, detail_address, is_default,
   create_time, update_time) VALUES
  (17201, 1702, 'Buyer A', '13800001702', 'Guangdong', 'Guangzhou', 'UC17 Road A', 1,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (17202, 1703, 'Buyer B', '13800001703', 'Guangdong', 'Shenzhen', 'UC17 Road B', 1,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (17203, 1704, 'Other', '13800001704', 'Beijing', 'Beijing', 'Foreign Address', 1,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO secondhand_product
  (id, seller_user_id, name, description, origin_price, sale_price, category_id, sub_category_id,
   condition_level, is_negotiable, status, create_time, update_time) VALUES
  (17101, 1701, 'UC17 direct purchase', 'available', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (17102, 1701, 'UC17 off shelf', 'off shelf', 180.00, 100.00, 8, 801, '90%', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (17103, 1701, 'UC17 sold', 'sold', 180.00, 100.00, 8, 801, '90%', 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (17104, 1702, 'UC17 own product', 'self purchase', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (17105, 1701, 'UC17 concurrent purchase', 'race', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (17106, 1701, 'UC17 rollback purchase', 'rollback', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (17107, 1701, 'UC17 cancel purchase', 'cancel', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (17108, 1701, 'UC17 pay purchase', 'pay', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (17109, 1701, 'UC17 negotiated purchase', 'negotiated', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (17110, 1701, 'UC17 invalid negotiated price', 'invalid price', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (17111, 1701, 'UC17 duplicate purchase', 'duplicate', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (17112, 1701, 'UC17 future negotiated price', 'future price', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_negotiation
  (id, product_id, buyer_user_id, seller_user_id, proposed_price, confirmed_price, status,
   effective_from, effective_until, create_time, update_time) VALUES
  (17301, 17109, 1702, 1701, 55.00, 60.00, 'CONFIRMED',
   '2020-01-01 00:00:00', '2099-12-31 23:59:59',
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (17302, 17110, 1702, 1701, 110.00, 120.00, 'CONFIRMED',
   '2020-01-01 00:00:00', '2099-12-31 23:59:59',
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (17303, 17112, 1702, 1701, 45.00, 50.00, 'CONFIRMED',
   '2090-01-01 00:00:00', '2099-12-31 23:59:59',
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
