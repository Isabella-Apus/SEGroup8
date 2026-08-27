DELETE FROM logistics_trace;
DELETE FROM logistics_path_template;
DELETE FROM notification;
DELETE FROM transaction_record;
DELETE FROM balance;
DELETE FROM order_item;
DELETE FROM order_info;
DELETE FROM product;
DELETE FROM shop;
DELETE FROM user;

INSERT INTO user (id, username, role, status, create_time, update_time) VALUES
  (1301, 'uc13_buyer', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1302, 'uc13_seller', 'OFFICIAL_SELLER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1303, 'uc13_other', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO shop (id, owner_user_id, name, status, create_time, update_time)
VALUES (1301, 1302, 'UC13 New Product Shop', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO product (id, shop_id, name, price, stock, status, create_time, update_time)
VALUES (1301, 1301, 'UC13 New Product', 120.00, 50, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO balance (user_id, personal_balance, business_balance, version) VALUES
  (1301, 0.00, 0.00, 0), (1302, 0.00, 0.00, 0);

INSERT INTO order_info (id, order_no, buyer_user_id, total_amount, payable_amount, pay_status,
  order_status, refund_status, receiver_name, receiver_phone, receiver_province,
  receiver_city, receiver_detail_address, version, create_time, update_time) VALUES
  (1301, 'UC13-NEW-FULFILLMENT', 1301, 120.00, 120.00, 1, 1, 0, 'UC13 Buyer', '13800001301', NULL, NULL, 'UC13 Address', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1302, 'UC13-NOT-PAID', 1301, 120.00, 120.00, 0, 0, 0, 'UC13 Buyer', '13800001301', NULL, NULL, 'UC13 Address', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1303, 'UC13-MERGED', 1301, 240.00, 240.00, 1, 1, 0, 'UC13 Buyer', '13800001301', NULL, NULL, 'UC13 Address', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1304, 'UC13-AUTO-RACE', 1301, 120.00, 120.00, 1, 2, 0, 'UC13 Buyer', '13800001301', NULL, NULL, 'UC13 Address', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO order_item (order_id, product_type, product_id, product_name, price, quantity, status, create_time, update_time) VALUES
  (1301, 'NEW', 1301, 'UC13 New Product', 120.00, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1302, 'NEW', 1301, 'UC13 New Product', 120.00, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1303, 'NEW', 1301, 'UC13 New Product', 120.00, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1303, 'NEW', 1301, 'UC13 New Product', 120.00, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1304, 'NEW', 1301, 'UC13 New Product', 120.00, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
UPDATE order_info SET logistics_status='ARRIVED', auto_confirm_deadline=TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP) WHERE id=1304;
