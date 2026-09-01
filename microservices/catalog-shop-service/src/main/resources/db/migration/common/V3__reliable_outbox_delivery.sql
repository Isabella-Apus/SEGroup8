ALTER TABLE outbox_event ADD COLUMN destination VARCHAR(40) NOT NULL DEFAULT 'MESSAGING';
ALTER TABLE outbox_event ADD COLUMN next_attempt_at TIMESTAMP NULL;
ALTER TABLE outbox_event ADD COLUMN last_error VARCHAR(500) NULL;
ALTER TABLE outbox_event ADD COLUMN sent_at TIMESTAMP NULL;
UPDATE outbox_event SET destination='ORDER' WHERE event_type IN ('InventoryReservationExpired.v1','InventoryReservationReleased.v1');
CREATE INDEX idx_outbox_delivery ON outbox_event(status,destination,next_attempt_at,id);
