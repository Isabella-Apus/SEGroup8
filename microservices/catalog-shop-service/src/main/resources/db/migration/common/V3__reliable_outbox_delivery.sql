ALTER TABLE outbox_event ADD COLUMN IF NOT EXISTS destination VARCHAR(40) NOT NULL DEFAULT 'MESSAGING';
ALTER TABLE outbox_event ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMP NULL;
ALTER TABLE outbox_event ADD COLUMN IF NOT EXISTS last_error VARCHAR(500) NULL;
ALTER TABLE outbox_event ADD COLUMN IF NOT EXISTS sent_at TIMESTAMP NULL;
UPDATE outbox_event SET destination='ORDER' WHERE event_type IN ('InventoryReservationExpired.v1','InventoryReservationReleased.v1');
CREATE INDEX idx_outbox_delivery ON outbox_event(status,destination,next_attempt_at,id);
