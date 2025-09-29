-- V001__init.sql  (MySQL 8+)
-- Engine/charset defaults are recommended at DB level, but we set them here too.

-- ----------------------------
-- uploaded_file: stores the raw uploaded JSON file
-- ----------------------------
CREATE TABLE IF NOT EXISTS uploaded_file (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  original_name   VARCHAR(255) NOT NULL,
  content_type    VARCHAR(100),
  size_bytes      BIGINT NOT NULL,
  sha256          CHAR(64) NOT NULL,
  uploaded_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  raw_json        LONGTEXT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_uploaded_file_sha256 (sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- record: one row per logical record parsed from the file
-- ----------------------------
CREATE TABLE IF NOT EXISTS record (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  file_id         BIGINT UNSIGNED NOT NULL,
  payload_json    JSON NOT NULL,
  status          ENUM('RECEIVED','PROCESSING','PROCESSED','FAILED') NOT NULL DEFAULT 'RECEIVED',
  error_message   TEXT,
  pdf_url         TEXT,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_record_file_id (file_id),
  KEY idx_record_status (status),
  CONSTRAINT fk_record_file
    FOREIGN KEY (file_id) REFERENCES uploaded_file(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- outbox_event (optional): used only if you later add an event stream
-- ----------------------------
CREATE TABLE IF NOT EXISTS outbox_event (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  aggregate_type  VARCHAR(100) NOT NULL,   -- e.g., 'record'
  aggregate_id    BIGINT UNSIGNED NOT NULL,
  event_type      VARCHAR(100) NOT NULL,   -- e.g., 'RecordReceived'
  payload_json    JSON NOT NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at    TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_outbox_created (created_at),
  KEY idx_outbox_processed_at (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
