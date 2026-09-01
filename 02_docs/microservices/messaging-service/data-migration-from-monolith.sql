-- Operator-run only. Requires a maintenance window and a temporary cross-schema migration principal.
-- Preconditions: Flyway V1 applied; target business tables empty. No source table is modified.
SELECT 'chat_conversation' table_name, COUNT(*) row_count, MAX(id) max_id FROM segroup8_platform.chat_conversation
UNION ALL SELECT 'chat_message', COUNT(*), MAX(id) FROM segroup8_platform.chat_message
UNION ALL SELECT 'notification', COUNT(*), MAX(id) FROM segroup8_platform.notification;
SELECT 'chat_conversation' table_name, COUNT(*) row_count, MAX(id) max_id FROM messaging_db.chat_conversation
UNION ALL SELECT 'chat_message', COUNT(*), MAX(id) FROM messaging_db.chat_message
UNION ALL SELECT 'notification', COUNT(*), MAX(id) FROM messaging_db.notification;

START TRANSACTION;

INSERT INTO messaging_db.user_access_projection
  (user_id, access_status, role, display_name, avatar_url, source_version, updated_at)
SELECT id, status, role, COALESCE(NULLIF(nickname, ''), username), avatar,
       UNIX_TIMESTAMP(update_time), update_time
FROM segroup8_platform.`user`;

INSERT INTO messaging_db.chat_conversation
  (id, buyer_user_id, seller_user_id, buyer_display_name, buyer_avatar_url, buyer_role,
   seller_display_name, seller_avatar_url, seller_role, source_type, source_id, source_title,
   last_message_content, last_message_time, create_time, update_time)
SELECT c.id, c.buyer_user_id, c.seller_user_id,
       COALESCE(NULLIF(b.nickname, ''), b.username), b.avatar, b.role,
       COALESCE(NULLIF(s.nickname, ''), s.username), s.avatar, s.role,
       c.source_type, c.source_id, c.source_title, c.last_message_content, c.last_message_time,
       c.create_time, c.update_time
FROM segroup8_platform.chat_conversation c
JOIN segroup8_platform.`user` b ON b.id = c.buyer_user_id
JOIN segroup8_platform.`user` s ON s.id = c.seller_user_id;

INSERT INTO messaging_db.chat_message
  (id, conversation_id, sender_user_id, receiver_user_id, content, is_read, create_time)
SELECT id, conversation_id, sender_user_id, receiver_user_id, content, is_read, create_time
FROM segroup8_platform.chat_message;

INSERT INTO messaging_db.notification
  (id, user_id, title, content, target_path, scope, is_read, create_time)
SELECT id, user_id, title, content, target_path,
       CASE WHEN target_path LIKE '/merchant/%' THEN 'seller' ELSE 'buyer' END,
       is_read, create_time
FROM segroup8_platform.notification;

INSERT INTO messaging_db.user_block_projection
  (blocker_user_id, blocked_user_id, active, source_version, updated_at)
SELECT blocker_id, blocked_id, 1, UNIX_TIMESTAMP(create_time), create_time
FROM segroup8_platform.user_block;

INSERT IGNORE INTO messaging_db.user_block_projection
  (blocker_user_id, blocked_user_id, active, source_version, updated_at)
SELECT buyer_user_id, seller_user_id, 0, 0, CURRENT_TIMESTAMP
FROM segroup8_platform.chat_conversation;
INSERT IGNORE INTO messaging_db.user_block_projection
  (blocker_user_id, blocked_user_id, active, source_version, updated_at)
SELECT seller_user_id, buyer_user_id, 0, 0, CURRENT_TIMESTAMP
FROM segroup8_platform.chat_conversation;

COMMIT;

SELECT 'chat_conversation' table_name, COUNT(*) row_count, MAX(id) max_id FROM messaging_db.chat_conversation
UNION ALL SELECT 'chat_message', COUNT(*), MAX(id) FROM messaging_db.chat_message
UNION ALL SELECT 'notification', COUNT(*), MAX(id) FROM messaging_db.notification;
SELECT COUNT(*) orphan_messages FROM messaging_db.chat_message m
LEFT JOIN messaging_db.chat_conversation c ON c.id=m.conversation_id WHERE c.id IS NULL;
SELECT COUNT(*) invalid_conversations FROM messaging_db.chat_conversation
WHERE buyer_user_id IS NULL OR seller_user_id IS NULL OR buyer_user_id=seller_user_id;
SELECT COUNT(*) invalid_messages FROM messaging_db.chat_message
WHERE sender_user_id IS NULL OR receiver_user_id IS NULL OR conversation_id IS NULL;
