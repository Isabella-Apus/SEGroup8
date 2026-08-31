ALTER TABLE outbox_event
  ADD COLUMN last_error VARCHAR(500) NULL;
