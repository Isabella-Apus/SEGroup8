-- Applied after Flyway in the disposable E2E database.
INSERT INTO user_access_projection
    (user_id, access_status, role, display_name, avatar_url, source_version)
VALUES
    (2, 'ACTIVE', 'OFFICIAL_SELLER', 'Demo Seller', '', 0),
    (3, 'ACTIVE', 'USER', 'Demo User', '', 0),
    (4, 'ACTIVE', 'USER', 'Third Party User', '', 0),
    (1001, 'ACTIVE', 'USER', 'Audit Buyer', '', 0),
    (1002, 'ACTIVE', 'USER', 'Audit Seller', '', 0),
    (1003, 'ACTIVE', 'USER', 'Audit Outsider', '', 0)
ON DUPLICATE KEY UPDATE
    access_status = VALUES(access_status), role = VALUES(role),
    display_name = VALUES(display_name), source_version = VALUES(source_version);

-- Version 0 deliberately exercises the identity-governance internal fallback.
INSERT INTO user_block_projection
    (blocker_user_id, blocked_user_id, active, source_version)
VALUES
    (2, 3, 0, 0), (3, 2, 0, 0),
    (1001, 1002, 0, 0), (1002, 1001, 0, 0)
ON DUPLICATE KEY UPDATE active = VALUES(active), source_version = VALUES(source_version);

INSERT INTO chat_conversation
    (id, buyer_user_id, seller_user_id, buyer_display_name, seller_display_name,
     buyer_role, seller_role, source_type, source_id, source_title)
VALUES
    (9001, 1001, 1002, 'Audit Buyer', 'Audit Seller', 'USER', 'USER', 'DIRECT', 1, 'Audit conversation')
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO notification
    (id, user_id, title, content, target_path, scope, is_read, notification_type,
     business_type, business_id, dedupe_key, trace_id)
VALUES
    (9001, 1001, 'Audit notification', 'Seed notification', '/chat', 'buyer', 0,
     'AUDIT', 'TEST', 'seed-9001', 'seed-9001')
ON DUPLICATE KEY UPDATE is_read = 0;
