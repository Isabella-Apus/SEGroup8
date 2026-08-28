DELETE FROM notification;
DELETE FROM transaction_record;
DELETE FROM balance;
DELETE FROM auction_log;
DELETE FROM product_auction;
DELETE FROM order_item;
DELETE FROM order_info;
DELETE FROM secondhand_product;
DELETE FROM user;

INSERT INTO user (id, username, password, nickname, role, status, create_time, update_time) VALUES
  (1901, 'uc19_seller', 'x', 'UC19 Seller', 'USER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1902, 'uc19_bidder_a', 'x', 'UC19 Bidder A', 'USER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1903, 'uc19_bidder_b', 'x', 'UC19 Bidder B', 'USER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1904, 'uc19_outsider', 'x', 'UC19 Outsider', 'USER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO balance (user_id, personal_balance, business_balance, version, create_time, update_time) VALUES
  (1901, 500.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1902, 500.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1903, 500.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1904, 500.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO secondhand_product
  (id, seller_user_id, name, description, origin_price, sale_price, category_id, sub_category_id,
   condition_level, is_negotiable, status, create_time, update_time) VALUES
  (19101, 1901, 'UC19 balance auction', 'fund hold and refund', 260.00, 180.00, 8, 801, '95%', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (19102, 1901, 'UC19 concurrent auction', 'optimistic bid race', 260.00, 180.00, 8, 801, '95%', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (19103, 1901, 'UC19 flow auction', 'no bidder flow', 260.00, 180.00, 8, 801, '95%', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (19104, 1901, 'UC19 settled auction', 'successful settlement', 260.00, 180.00, 8, 801, '95%', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (19105, 1901, 'UC19 retry auction', 'retry after order failure', 260.00, 180.00, 8, 801, '95%', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (19106, 1901, 'UC19 future auction', 'not started', 260.00, 180.00, 8, 801, '95%', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (19107, 1901, 'UC19 closed auction', 'already closed', 260.00, 180.00, 8, 801, '95%', 0, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (19108, 1901, 'UC19 self auction', 'seller cannot bid', 260.00, 180.00, 8, 801, '95%', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (19109, 1901, 'UC19 expired auction', 'already ended', 260.00, 180.00, 8, 801, '95%', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (19110, 1901, 'UC19 relisted auction', 'historical auction allows a new auction', 260.00, 180.00, 8, 801, '95%', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_auction
  (id, product_id, seller_user_id, start_price, increment_amount, current_price,
   current_bidder_user_id, start_time, end_time, status, settled_order_id, version, create_time, update_time) VALUES
  (19301, 19101, 1901, 100.00, 10.00, 100.00, NULL, '2000-01-01 00:00:00', '2099-12-31 23:59:59', 'ONGOING', NULL, 0, NOW(), NOW()),
  (19302, 19102, 1901, 100.00, 10.00, 100.00, NULL, '2000-01-01 00:00:00', '2099-12-31 23:59:59', 'ONGOING', NULL, 0, NOW(), NOW()),
  (19303, 19103, 1901, 100.00, 10.00, 100.00, NULL, '2000-01-01 00:00:00', '2099-12-31 23:59:59', 'ONGOING', NULL, 0, NOW(), NOW()),
  (19304, 19104, 1901, 100.00, 10.00, 100.00, NULL, '2000-01-01 00:00:00', '2099-12-31 23:59:59', 'ONGOING', NULL, 0, NOW(), NOW()),
  (19305, 19105, 1901, 100.00, 10.00, 100.00, NULL, '2000-01-01 00:00:00', '2099-12-31 23:59:59', 'ONGOING', NULL, 0, NOW(), NOW()),
  (19306, 19106, 1901, 100.00, 10.00, 100.00, NULL, '2099-01-01 00:00:00', '2099-01-02 00:00:00', 'ONGOING', NULL, 0, NOW(), NOW()),
  (19307, 19107, 1901, 100.00, 10.00, 100.00, NULL, '2000-01-01 00:00:00', '2099-12-31 23:59:59', 'FLOW', NULL, 1, NOW(), NOW()),
  (19308, 19108, 1901, 100.00, 10.00, 100.00, NULL, '2000-01-01 00:00:00', '2099-12-31 23:59:59', 'ONGOING', NULL, 0, NOW(), NOW()),
  (19309, 19109, 1901, 100.00, 10.00, 100.00, NULL, '2000-01-01 00:00:00', '2000-01-02 00:00:00', 'ONGOING', NULL, 0, NOW(), NOW()),
  (19310, 19110, 1901, 80.00, 5.00, 80.00, NULL, '2000-01-01 00:00:00', '2000-01-02 00:00:00', 'FLOW', NULL, 1, NOW(), NOW());
