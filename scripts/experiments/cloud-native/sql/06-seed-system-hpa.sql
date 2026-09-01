DROP PROCEDURE IF EXISTS seed_system_hpa_products;
DELIMITER //
CREATE PROCEDURE seed_system_hpa_products()
BEGIN
  DECLARE n INT DEFAULT 1;
  DECLARE selected_seller BIGINT;
  SELECT id INTO selected_seller FROM `user` ORDER BY id LIMIT 1;
  IF selected_seller IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'system HPA seed requires at least one user';
  END IF;
  DELETE FROM secondhand_product WHERE id BETWEEN 9800001 AND 9805000;
  WHILE n <= 5000 DO
    INSERT INTO secondhand_product (
      id, seller_user_id, name, cover, images, description, origin_price,
      sale_price, category_id, sub_category_id, condition_level,
      is_negotiable, status, create_time, update_time
    ) VALUES (
      9800000 + n, selected_seller, CONCAT('System HPA Product ', LPAD(n, 4, '0')),
      '/experiment/system-hpa.png', '[]', CONCAT('Temporary HPA dataset row ', n),
      200.00 + MOD(n, 50), 100.00 + MOD(n, 50), 9, 901, 'GOOD',
      1, IF(MOD(n, 5) = 0, 1, 2), TIMESTAMPADD(SECOND, n, '2026-08-31 00:00:00'),
      TIMESTAMPADD(SECOND, n, '2026-08-31 00:00:00')
    );
    SET n = n + 1;
  END WHILE;
END//
DELIMITER ;
CALL seed_system_hpa_products();
DROP PROCEDURE seed_system_hpa_products;
