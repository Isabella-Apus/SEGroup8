CREATE TABLE `user` (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar VARCHAR(255),
    phone VARCHAR(20),
    email VARCHAR(100),
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    credit_score INT NOT NULL DEFAULT 100,
    buyer_credit_score INT NOT NULL DEFAULT 100,
    seller_credit_score INT NOT NULL DEFAULT 100,
    shop_name VARCHAR(80),
    access_version BIGINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (username)
);

CREATE TABLE address (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    receiver_name VARCHAR(50) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    province VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    detail_address VARCHAR(255) NOT NULL,
    is_default TINYINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES `user`(id)
);
CREATE INDEX idx_address_user ON address(user_id);

CREATE TABLE merchant_application (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    store_name VARCHAR(80) NOT NULL,
    category_id BIGINT NOT NULL,
    id_card_no VARCHAR(30) NOT NULL,
    bank_card_no VARCHAR(50) NOT NULL,
    license_img VARCHAR(255) NOT NULL,
    warehouse_addr VARCHAR(255) NOT NULL,
    warehouse_province VARCHAR(50) NOT NULL,
    warehouse_city VARCHAR(50) NOT NULL,
    warehouse_detail VARCHAR(255) NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    reject_reason VARCHAR(255),
    apply_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES `user`(id)
);
CREATE INDEX idx_merchant_application_status ON merchant_application(status);

CREATE TABLE report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporter_user_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE user_report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL,
    reported_id BIGINT NOT NULL,
    reporter_role VARCHAR(30) NOT NULL,
    trade_context VARCHAR(20) NOT NULL DEFAULT 'SHOP',
    reason_type VARCHAR(50) NOT NULL,
    reason_desc VARCHAR(500),
    evidence_urls VARCHAR(1000),
    status TINYINT NOT NULL DEFAULT 0,
    admin_id BIGINT,
    admin_remark VARCHAR(500),
    audit_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX idx_user_report_reporter ON user_report(reporter_id);
CREATE INDEX idx_user_report_reported ON user_report(reported_id);
CREATE INDEX idx_user_report_status ON user_report(status);

CREATE TABLE user_block (
    id BIGINT NOT NULL AUTO_INCREMENT,
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (blocker_id, blocked_id)
);
CREATE INDEX idx_user_block_blocked ON user_block(blocked_id);

CREATE TABLE credit_score_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    delta INT NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    reason_desc VARCHAR(255),
    ref_id BIGINT,
    operator_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX idx_credit_score_log_user ON credit_score_log(user_id);

CREATE TABLE admin_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_user_id BIGINT,
    admin_username VARCHAR(50) NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT,
    detail VARCHAR(500),
    source_service VARCHAR(80) NOT NULL DEFAULT 'identity-governance-service',
    source_event_id VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (source_service, source_event_id)
);

CREATE TABLE idempotency_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    caller_id VARCHAR(80) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    response_body TEXT,
    expire_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (caller_id, request_path, idempotency_key)
);

CREATE TABLE outbox_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_attempt_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_time TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (event_id)
);
CREATE INDEX idx_outbox_status ON outbox_event(status, next_attempt_time);
