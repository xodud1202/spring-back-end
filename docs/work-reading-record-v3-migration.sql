-- Run once on the existing MySQL reading table before deploying the v3 API.
-- Existing rows receive reading round 1 and reread false; no records are removed.
ALTER TABLE eshop.WORK_READING_RECORD
    ADD COLUMN READING_ROUND INT NOT NULL DEFAULT 1,
    ADD COLUMN REREAD BOOLEAN NOT NULL DEFAULT FALSE;
