-- One-time MySQL import. Run after renaming the prototype tables into the target schema
-- as legacy_products, legacy_shops, legacy_risk_audits, legacy_search_history and legacy_keyword_stats.
-- The guarded procedure permits clean installations where no prototype tables exist.
DELIMITER $$
CREATE PROCEDURE migrate_catalog_shop_prototypes()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='legacy_shops') THEN
    INSERT IGNORE INTO shop(id,seller_id,name,announcement,status,decoration_template,decoration_json,updated_at)
      SELECT id,seller_id,name,announcement,status,decoration_template,decoration_json,updated_at FROM legacy_shops;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='legacy_products') THEN
    INSERT IGNORE INTO product(id,seller_id,shop_id,category_id,name,description,price,stock,reserved_stock,status,updated_at)
      SELECT id,seller_id,shop_id,1,name,description,price,stock,0,status,updated_at FROM legacy_products;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='legacy_risk_audits') THEN
    INSERT IGNORE INTO product_risk_audit SELECT * FROM legacy_risk_audits;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='legacy_search_history') THEN
    INSERT IGNORE INTO user_search_history SELECT * FROM legacy_search_history;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='legacy_keyword_stats') THEN
    INSERT IGNORE INTO search_keyword_stat SELECT * FROM legacy_keyword_stats;
  END IF;
END$$
CALL migrate_catalog_shop_prototypes()$$
DROP PROCEDURE migrate_catalog_shop_prototypes$$
DELIMITER ;
