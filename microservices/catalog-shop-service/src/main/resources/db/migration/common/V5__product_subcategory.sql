ALTER TABLE product ADD COLUMN IF NOT EXISTS sub_category_id BIGINT NULL;
UPDATE product SET sub_category_id=category_id WHERE sub_category_id IS NULL;
