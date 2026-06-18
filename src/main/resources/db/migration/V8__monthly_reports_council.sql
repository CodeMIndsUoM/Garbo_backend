ALTER TABLE monthly_reports
    ADD COLUMN IF NOT EXISTS council VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_monthly_reports_council_created
    ON monthly_reports (council, created_at DESC);
