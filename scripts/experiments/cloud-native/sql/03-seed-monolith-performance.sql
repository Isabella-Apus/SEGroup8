USE monolith_db;
DROP PROCEDURE IF EXISTS seed_experiment_monolith;
DELIMITER //
CREATE PROCEDURE seed_experiment_monolith()
BEGIN
  DECLARE n INT DEFAULT 1;
  DELETE FROM secondhand_product WHERE id BETWEEN 900001 AND 900500;
  WHILE n <= 500 DO
    INSERT INTO secondhand_product (
      id, seller_user_id, name, cover, images, description, origin_price,
      sale_price, category_id, sub_category_id, condition_level,
      is_negotiable, status, create_time, update_time
    ) VALUES (
      900000 + n, 2, CONCAT('Experiment Product ', LPAD(n, 3, '0')),
      '/experiment/cover.png', '[]', CONCAT('Fixed performance dataset row ', n),
      200.00 + MOD(n, 50), 100.00 + MOD(n, 50), 9, 901, 'GOOD',
      1, 1, TIMESTAMPADD(SECOND, n, '2026-08-30 00:00:00'),
      TIMESTAMPADD(SECOND, n, '2026-08-30 00:00:00')
    );
    SET n = n + 1;
  END WHILE;
END//
DELIMITER ;
CALL seed_experiment_monolith();
DROP PROCEDURE seed_experiment_monolith;
