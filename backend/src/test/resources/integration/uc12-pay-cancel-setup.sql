DELETE FROM idempotency_record;
DELETE FROM notification;
DELETE FROM transaction_record;
DELETE FROM balance;
DELETE FROM user_voucher;
DELETE FROM voucher;
DELETE FROM order_item;
DELETE FROM order_info;
DELETE FROM product;
DELETE FROM shop;
DELETE FROM user;

INSERT INTO user (id, username, password, role, status, create_time, update_time) VALUES
  (1201, 'uc12_buyer', 'x', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1202, 'uc12_other', 'x', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1203, 'uc12_seller', 'x', 'OFFICIAL_SELLER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO shop (id, owner_user_id, name, status, create_time, update_time)
VALUES (1201, 1203, 'UC12 Shop', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product (id, shop_id, name, price, stock, status, create_time, update_time)
VALUES
  (1201, 1201, 'UC12 Product', 100.00, 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1202, 1201, 'UC12 Product Two', 50.00, 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO balance (user_id, personal_balance, business_balance, version) VALUES
  (1201, 300.00, 0.00, 0),
  (1203, 0.00, 50.00, 0);

INSERT INTO voucher (id, issuer_type, voucher_type, scope_type, shop_id, name, type,
  discount_amount, min_amount, total_count, used_count, status, create_time, update_time)
VALUES (1201, 1, 1, 1, 1201, 'UC12 Voucher', 1, 20.00, 100.00, 10, 0, 1,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO order_info (id, order_no, buyer_user_id, total_amount, payable_amount,
  voucher_id, voucher_discount_amount, seller_bear_amount, platform_bear_amount,
  pay_status, order_status, refund_status, receiver_name, version, create_time, update_time) VALUES
  (1201, 'UC12-PAY-COIN', 1201, 100.00, 80.00, 1201, 20.00, 20.00, 0.00, 0, 0, 0, 'Buyer', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1202, 'UC12-CANCEL', 1201, 100.00, 80.00, 1201, 20.00, 20.00, 0.00, 0, 0, 0, 'Buyer', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1203, 'UC12-INSUFFICIENT', 1201, 500.00, 500.00, NULL, 0.00, 0.00, 0.00, 0, 0, 0, 'Buyer', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1204, 'UC12-PAID', 1201, 100.00, 100.00, NULL, 0.00, 0.00, 0.00, 1, 1, 0, 'Buyer', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1205, 'UC12-COMPLETED', 1201, 100.00, 100.00, NULL, 0.00, 0.00, 0.00, 1, 4, 0, 'Buyer', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1206, 'UC12-IDEMPOTENT-PAY', 1201, 60.00, 60.00, NULL, 0.00, 0.00, 0.00, 0, 0, 0, 'Buyer', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1207, 'UC12-IDEMPOTENT-CANCEL', 1201, 40.00, 40.00, NULL, 0.00, 0.00, 0.00, 0, 0, 0, 'Buyer', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1208, 'UC12-PAY-CANCEL-RACE', 1201, 30.00, 30.00, NULL, 0.00, 0.00, 0.00, 0, 0, 0, 'Buyer', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1209, 'UC12-PAY-SPLIT', 1201, 150.00, 120.00, 1201, 30.00, 20.00, 10.00, 0, 0, 0, 'Buyer', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO order_item (order_id, product_type, product_id, product_name, price, quantity, status, create_time, update_time) VALUES
  (1201, 'NEW', 1201, 'UC12 Product', 100.00, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1202, 'NEW', 1201, 'UC12 Product', 100.00, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1203, 'NEW', 1201, 'UC12 Product', 500.00, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1204, 'NEW', 1201, 'UC12 Product', 100.00, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1205, 'NEW', 1201, 'UC12 Product', 100.00, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1206, 'NEW', 1201, 'UC12 Product', 60.00, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1207, 'NEW', 1201, 'UC12 Product', 40.00, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1208, 'NEW', 1201, 'UC12 Product', 30.00, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1209, 'NEW', 1201, 'UC12 Product', 100.00, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1209, 'NEW', 1202, 'UC12 Product Two', 50.00, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO user_voucher (id, user_id, voucher_id, status, used_order_id, received_time, create_time, update_time) VALUES
  (1201, 1201, 1201, 1, 1201, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1202, 1201, 1201, 1, 1202, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1203, 1201, 1201, 1, 1209, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
