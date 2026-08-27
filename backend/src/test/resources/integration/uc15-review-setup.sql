DELETE FROM review;
DELETE FROM order_item WHERE order_id = 1501;
DELETE FROM order_info WHERE id = 1501;
DELETE FROM product WHERE id IN (1501, 1502);
DELETE FROM shop WHERE id = 1501;
DELETE FROM user WHERE id IN (1501, 1502, 1503);

INSERT INTO user (id, username, password, nickname, role, status, create_time, update_time)
VALUES
  (1501, 'uc15buyer', 'x', 'UC15 Buyer', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1502, 'uc15seller', 'x', 'UC15 Seller', 'OFFICIAL_SELLER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1503, 'uc15other', 'x', 'UC15 Other', 'OFFICIAL_SELLER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO shop (id, owner_user_id, name, status, create_time, update_time)
VALUES (1501, 1502, 'UC15 Review Shop', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product (id, shop_id, name, price, stock, status, create_time, update_time)
VALUES
  (1501, 1501, 'UC15 Product One', 30.00, 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1502, 1501, 'UC15 Product Two', 40.00, 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO order_info (id, order_no, buyer_user_id, total_amount, pay_status, order_status, refund_status,
  can_refund, version, create_time, update_time)
VALUES (1501, 'UC15-REVIEW-FLOW', 1501, 70.00, 1, 3, 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO order_item (id, order_id, product_type, product_id, product_name, price, quantity, status, create_time, update_time)
VALUES
  (150101, 1501, 'NEW', 1501, 'UC15 Product One', 30.00, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (150102, 1501, 'NEW', 1502, 'UC15 Product Two', 40.00, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
