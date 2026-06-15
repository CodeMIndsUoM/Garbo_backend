-- Status discrepancy flag when mentor reports non-empty on an empty/collected bin.
ALTER TABLE bin_reports
    ADD COLUMN IF NOT EXISTS discrepancy BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE bin_reports
    ADD COLUMN IF NOT EXISTS previous_status VARCHAR(20);
