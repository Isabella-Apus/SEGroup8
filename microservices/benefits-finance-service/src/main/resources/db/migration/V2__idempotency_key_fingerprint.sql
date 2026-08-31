ALTER TABLE idempotency_record ADD COLUMN request_fingerprint CHAR(64) NULL;
ALTER TABLE idempotency_record ADD COLUMN response_type VARCHAR(255) NULL;
