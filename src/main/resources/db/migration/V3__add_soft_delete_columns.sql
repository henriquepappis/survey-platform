-- Soft delete support
-- MySQL variant without IF NOT EXISTS for wider compatibility
ALTER TABLE surveys ADD COLUMN deleted_at DATETIME NULL;
ALTER TABLE questions ADD COLUMN deleted_at DATETIME NULL;
ALTER TABLE options ADD COLUMN deleted_at DATETIME NULL;
ALTER TABLE users ADD COLUMN deleted_at DATETIME NULL;
