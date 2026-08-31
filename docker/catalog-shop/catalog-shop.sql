INSERT INTO category(id,parent_id,name,sort_order,active) VALUES
  (1,NULL,'数码产品',1,true),(101,1,'手机',1,true)
ON DUPLICATE KEY UPDATE name=VALUES(name),parent_id=VALUES(parent_id),active=VALUES(active);
INSERT INTO shop(id,seller_id,merchant_application_id,name,announcement,status,decoration_template,decoration_json) VALUES
  (1,2,'compose-seller-2','Container Demo Store','Seed shop for full-system E2E','OPEN','GRID','{"components":[]}')
ON DUPLICATE KEY UPDATE name=VALUES(name),status=VALUES(status);
INSERT INTO product(id,seller_id,shop_id,category_id,sub_category_id,name,description,price,stock,reserved_stock,status) VALUES
  (1,2,1,1,101,'Container Demo Keyboard','Seed product for full-system E2E',299.00,80,0,'ON_SALE')
ON DUPLICATE KEY UPDATE name=VALUES(name),price=VALUES(price),stock=VALUES(stock),status=VALUES(status);
