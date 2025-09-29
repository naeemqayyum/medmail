-- V002__extra_indexes.sql  (MySQL 8.x)
-- Create the index only if it doesn't already exist.

SET @idx_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'record'
    AND INDEX_NAME = 'idx_record_created_at'
);

SET @sql := IF(@idx_exists = 0,
               'CREATE INDEX idx_record_created_at ON record (created_at)',
               'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
