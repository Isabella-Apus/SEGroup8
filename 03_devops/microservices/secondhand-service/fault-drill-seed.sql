SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM outbox_event
WHERE aggregate_id LIKE '991001%' OR aggregate_id LIKE '991002%'
   OR payload LIKE '%991001%' OR payload LIKE '%991002%';
DELETE FROM trade_order_request WHERE product_id IN (991001, 991002);
DELETE FROM secondhand_product WHERE id IN (991001, 991002);

INSERT INTO secondhand_product
  (id, seller_user_id, seller_name_snapshot, name, cover, images, description,
   origin_price, sale_price, category_id, sub_category_id, condition_level,
   is_negotiable, status, risk_status, version, deleted)
VALUES
  (991001,950001,'Fault Drill Seller','Fault Recovery Product','','[]','Order dependency recovery drill',100.00,60.00,8,801,'LIKE_NEW',0,1,'APPROVED',0,0),
  (991002,950001,'Fault Drill Seller','Fault Exhaustion Product','','[]','Order dependency retry exhaustion drill',100.00,55.00,8,801,'LIKE_NEW',0,1,'APPROVED',0,0);

SET FOREIGN_KEY_CHECKS = 1;
