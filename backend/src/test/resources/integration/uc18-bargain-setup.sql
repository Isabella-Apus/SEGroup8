DELETE FROM chat_message;
DELETE FROM chat_conversation;
DELETE FROM notification;
DELETE FROM product_negotiation;
DELETE FROM order_item;
DELETE FROM order_info;
DELETE FROM secondhand_product;
DELETE FROM user_block;
DELETE FROM user;

INSERT INTO user (id, username, password, nickname, role, status, create_time, update_time) VALUES
  (1801, 'uc18_seller', 'x', 'UC18 Seller', 'OFFICIAL_SELLER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1802, 'uc18_buyer', 'x', 'UC18 Buyer', 'USER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1803, 'uc18_other', 'x', 'UC18 Other', 'OFFICIAL_SELLER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO secondhand_product
  (id, seller_user_id, name, description, origin_price, sale_price, category_id, sub_category_id,
   condition_level, is_negotiable, status, create_time, update_time) VALUES
  (18101, 1801, 'UC18 bargain application', 'valid bargain', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (18102, 1801, 'UC18 fixed price', 'not negotiable', 180.00, 100.00, 8, 801, '90%', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (18103, 1802, 'UC18 buyer owned', 'self bargain', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (18104, 1801, 'UC18 confirmed deal', 'confirmation', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (18105, 1801, 'UC18 rejected deal', 'rejection', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (18106, 1801, 'UC18 concurrent decision', 'confirm reject race', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (18107, 1801, 'UC18 side effect isolation', 'chat and notification failure', 180.00, 100.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO product_negotiation
  (id, product_id, buyer_user_id, seller_user_id, proposed_price, status, create_time, update_time) VALUES
  (18301, 18104, 1802, 1801, 70.00, 'APPLIED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (18302, 18105, 1802, 1801, 65.00, 'APPLIED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (18303, 18106, 1802, 1801, 60.00, 'APPLIED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
