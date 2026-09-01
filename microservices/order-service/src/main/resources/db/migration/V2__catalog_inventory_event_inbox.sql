CREATE TABLE inbox_event (
  event_id VARCHAR(96) PRIMARY KEY,
  event_type VARCHAR(80) NOT NULL,
  producer VARCHAR(80) NOT NULL,
  payload TEXT NOT NULL,
  received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_inbox_event_type_received
  ON inbox_event(event_type, received_at);
