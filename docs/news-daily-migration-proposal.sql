-- PROPOSAL v2 ONLY: not automatically applied and never executed by this task.
-- User reports NEWS_DAILY was recreated with category/order/content and audit columns.
-- Verify actual schema, InnoDB engine, column charset, strict mode and >= READ COMMITTED first.
-- Both tables must use the same datasource as news.NEWS_DAILY.
-- Prior proposal v1 was not applied by this task. If it already exists elsewhere, review
-- an explicit upgrade/backfill separately; do not run these CREATE statements over it.
CREATE TABLE news.NEWS_DAILY_SEQUENCE (
    YYYYMMDD VARCHAR(8) NOT NULL PRIMARY KEY,
    LAST_SEQ INT NOT NULL DEFAULT 0
) ENGINE=InnoDB COLLATE=utf8mb4_unicode_ci;

CREATE TABLE news.NEWS_DAILY_BATCH (
    YYYYMMDD VARCHAR(8) NOT NULL,
    SEQ INT NOT NULL,
    REQUEST_ID VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PAYLOAD_HASH CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    SOURCE_GENERATED_AT VARCHAR(19) NOT NULL,
    ARTICLE_COUNT INT NOT NULL DEFAULT 0,
    REG_NO BIGINT NOT NULL,
    STATUS VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDING',
    FAILURE_CODE VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NULL,
    REG_DT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FINISHED_DT DATETIME NULL,
    PRIMARY KEY (YYYYMMDD, SEQ),
    UNIQUE KEY UK_NEWS_DAILY_BATCH_REQUEST (YYYYMMDD, REQUEST_ID),
    KEY IDX_NEWS_DAILY_BATCH_LATEST (YYYYMMDD, STATUS, SEQ),
    CONSTRAINT CHK_NEWS_DAILY_BATCH_STATE CHECK (STATUS IN ('PENDING','COMPLETED','FAILED')),
    CONSTRAINT CHK_NEWS_DAILY_BATCH_RESULT CHECK (
        (STATUS='PENDING' AND ARTICLE_COUNT=0 AND PAYLOAD_HASH IS NULL AND FAILURE_CODE IS NULL AND FINISHED_DT IS NULL)
        OR (STATUS='COMPLETED' AND ARTICLE_COUNT BETWEEN 20 AND 25 AND PAYLOAD_HASH IS NOT NULL AND FAILURE_CODE IS NULL AND FINISHED_DT IS NOT NULL)
        OR (STATUS='FAILED' AND ARTICLE_COUNT=0 AND PAYLOAD_HASH IS NULL AND FAILURE_CODE IS NOT NULL AND FINISHED_DT IS NOT NULL)
    )
) ENGINE=InnoDB COLLATE=utf8mb4_unicode_ci;
-- Reservation and LAST_SEQ commit in a short REQUIRES_NEW transaction.
-- Article INSERTs and transition to COMPLETED commit together in another transaction.
-- Confirmed rollback is followed by a separate conditional PENDING -> FAILED transaction.
-- No automatic timeout failure; interrupted/unknown outcomes remain PENDING until resolved.
-- Keep attempt rows and counter history; deleting them destroys idempotency and attempt numbering.
-- Existing summary rows are not automatically certified complete.
-- MySQL 8.0 NVARCHAR may imply utf8mb3 even with a utf8mb4 table default.
-- Inspect NEWS_CONTENT/CATEGORY_NM column character sets before activation.

