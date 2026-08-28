DELETE FROM chat_message;
DELETE FROM chat_conversation;
DELETE FROM notification;
DELETE FROM order_after_sale_log;
DELETE FROM transaction_record;
DELETE FROM logistics_trace;
DELETE FROM logistics_path_template;
DELETE FROM balance;
DELETE FROM product_negotiation;
DELETE FROM auction_log;
DELETE FROM product_auction;
DELETE FROM order_item;
DELETE FROM order_info;
DELETE FROM secondhand_product;
DELETE FROM user;

INSERT INTO user (id, username, password, nickname, role, status, create_time, update_time) VALUES
  (2001, 'uc20_seller', 'x', 'UC20 Seller', 'USER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2002, 'uc20_buyer', 'x', 'UC20 Buyer', 'USER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2003, 'uc20_outsider', 'x', 'UC20 Outsider', 'USER', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO balance (user_id, personal_balance, business_balance, version, create_time, update_time) VALUES
  (2001, 10.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2002, 500.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2003, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO secondhand_product
  (id, seller_user_id, name, description, origin_price, sale_price, category_id, sub_category_id,
   condition_level, is_negotiable, status, create_time, update_time) VALUES
  (20201, 2001, 'UC20 repeat shipment', 'paid pending shipment', 120.00, 80.00, 8, 801, '90%', 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20202, 2001, 'UC20 unpaid shipment', 'unpaid pending shipment', 120.00, 75.00, 8, 801, '90%', 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20203, 2001, 'UC20 wrong shipment state', 'paid pending payment state', 120.00, 70.00, 8, 801, '90%', 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20204, 2001, 'UC20 receipt settlement', 'shipped order', 130.00, 90.00, 8, 801, '95%', 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20205, 2001, 'UC20 shipment notification failure', 'notification isolation', 110.00, 65.00, 8, 801, '90%', 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20206, 2001, 'UC20 receipt notification failure', 'notification isolation', 105.00, 60.00, 8, 801, '90%', 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20207, 2001, 'UC20 settlement retry', 'settlement rollback', 100.00, 55.00, 8, 801, '90%', 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20208, 2001, 'UC20 bargain compensation', 'order creation rollback', 100.00, 50.00, 8, 801, '90%', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO logistics_path_template
  (id, origin_region, dest_region, path_nodes, create_time, update_time) VALUES
  (20401, '华南', '华北', '["广东省分拨中心","武汉中转站","Beijing分拨中心"]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO order_info
  (id, order_no, buyer_user_id, total_amount, payable_amount, pay_status, order_status, refund_status,
   receiver_name, receiver_phone, receiver_province, receiver_city, receiver_detail_address,
   pay_method, paid_time, shipped_time, delivery_time, logistics_template_id, logistics_status,
   logistics_current_index, version, create_time, update_time) VALUES
  (20301, 'ORD_UC20_REPEAT_SHIP', 2002, 80.00, 80.00, 1, 1, 0,
   'UC20 Buyer', '13800002002', 'Beijing', 'Beijing', 'UC20 Road 1',
   '微信支付', CURRENT_TIMESTAMP, NULL, NULL, NULL, 'PENDING', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20302, 'ORD_UC20_UNPAID', 2002, 75.00, 75.00, 0, 1, 0,
   'UC20 Buyer', '13800002002', 'Beijing', 'Beijing', 'UC20 Road 2',
   NULL, NULL, NULL, NULL, NULL, 'PENDING', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20303, 'ORD_UC20_WRONG_STATE', 2002, 70.00, 70.00, 1, 0, 0,
   'UC20 Buyer', '13800002002', 'Beijing', 'Beijing', 'UC20 Road 3',
   '微信支付', CURRENT_TIMESTAMP, NULL, NULL, NULL, 'PENDING', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20304, 'ORD_UC20_RECEIPT', 2002, 90.00, 90.00, 1, 2, 0,
   'UC20 Buyer', '13800002002', 'Beijing', 'Beijing', 'UC20 Road 4',
   '微信支付', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 20401, 'IN_TRANSIT', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20305, 'ORD_UC20_SHIP_NOTIFY', 2002, 65.00, 65.00, 1, 1, 0,
   'UC20 Buyer', '13800002002', 'Beijing', 'Beijing', 'UC20 Road 5',
   '微信支付', CURRENT_TIMESTAMP, NULL, NULL, NULL, 'PENDING', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20306, 'ORD_UC20_RECEIVE_NOTIFY', 2002, 60.00, 60.00, 1, 2, 0,
   'UC20 Buyer', '13800002002', 'Beijing', 'Beijing', 'UC20 Road 6',
   '微信支付', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 20401, 'IN_TRANSIT', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20307, 'ORD_UC20_SETTLEMENT_RETRY', 2002, 55.00, 55.00, 1, 2, 0,
   'UC20 Buyer', '13800002002', 'Beijing', 'Beijing', 'UC20 Road 7',
   '微信支付', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 20401, 'IN_TRANSIT', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO order_item
  (order_id, product_type, product_id, product_name, price, quantity, status, create_time, update_time) VALUES
  (20301, 'SECONDHAND', 20201, 'UC20 repeat shipment', 80.00, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20302, 'SECONDHAND', 20202, 'UC20 unpaid shipment', 75.00, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20303, 'SECONDHAND', 20203, 'UC20 wrong shipment state', 70.00, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20304, 'SECONDHAND', 20204, 'UC20 receipt settlement', 90.00, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20305, 'SECONDHAND', 20205, 'UC20 shipment notification failure', 65.00, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20306, 'SECONDHAND', 20206, 'UC20 receipt notification failure', 60.00, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (20307, 'SECONDHAND', 20207, 'UC20 settlement retry', 55.00, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO logistics_trace (order_id, node_name, status_desc, create_time) VALUES
  (20304, '广东省分拨中心', '包裹已揽收', CURRENT_TIMESTAMP),
  (20306, '广东省分拨中心', '包裹已揽收', CURRENT_TIMESTAMP),
  (20307, '广东省分拨中心', '包裹已揽收', CURRENT_TIMESTAMP);

INSERT INTO product_negotiation
  (id, product_id, buyer_user_id, seller_user_id, proposed_price, status, create_time, update_time) VALUES
  (20501, 20208, 2002, 2001, 45.00, 'APPLIED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
