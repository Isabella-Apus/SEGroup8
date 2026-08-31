ALTER TABLE inventory_reservation ADD COLUMN IF NOT EXISTS request_hash VARCHAR(64) NULL;
