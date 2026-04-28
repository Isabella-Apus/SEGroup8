SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `order_after_sale_log`;
DELETE FROM `logistics_trace`;
DELETE FROM `order_item`;
DELETE FROM `review`;
DELETE FROM `report`;
DELETE FROM `auction_log`;
DELETE FROM `product_auction`;
DELETE FROM `product_negotiation`;
DELETE FROM `order_info`;
DELETE FROM `browse_history`;
DELETE FROM `user_search_history`;
DELETE FROM `search_keyword_stat`;
DELETE FROM `chat_message`;
DELETE FROM `chat_conversation`;
DELETE FROM `notification`;
DELETE FROM `merchant_application`;
DELETE FROM `admin_audit_log`;
DELETE FROM `idempotency_record`;
DELETE FROM `transaction_record`;
DELETE FROM `balance`;
DELETE FROM `voucher`;
DELETE FROM `product`;
DELETE FROM `secondhand_product`;
DELETE FROM `shop`;
DELETE FROM `address`;
DELETE FROM `credit_score_log`;
DELETE FROM `user_report`;
DELETE FROM `user_block`;
DELETE FROM `user`;

SET @uv_exists = (
  SELECT COUNT(*)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_voucher'
);
SET @uv_sql = IF(@uv_exists = 1, 'DELETE FROM `user_voucher`', 'SELECT 1');
PREPARE stmt_uv FROM @uv_sql;
EXECUTE stmt_uv;
DEALLOCATE PREPARE stmt_uv;

SET FOREIGN_KEY_CHECKS = 1;
