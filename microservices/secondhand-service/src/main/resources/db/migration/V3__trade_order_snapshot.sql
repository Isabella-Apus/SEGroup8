ALTER TABLE trade_order_request ADD COLUMN product_name VARCHAR(200);
ALTER TABLE trade_order_request ADD COLUMN receiver_name VARCHAR(50);
ALTER TABLE trade_order_request ADD COLUMN receiver_phone VARCHAR(20);
ALTER TABLE trade_order_request ADD COLUMN receiver_province VARCHAR(50);
ALTER TABLE trade_order_request ADD COLUMN receiver_city VARCHAR(50);
ALTER TABLE trade_order_request ADD COLUMN receiver_detail_address VARCHAR(255);

UPDATE trade_order_request
SET product_name = (SELECT p.name FROM secondhand_product p WHERE p.id = trade_order_request.product_id)
WHERE product_name IS NULL;
