ALTER TABLE collection_offers
    ADD COLUMN
IF NOT EXISTS collector_hidden BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN
IF NOT EXISTS collector_hidden_at TIMESTAMP
WITH TIME ZONE;

CREATE INDEX
IF NOT EXISTS idx_co_collector_hidden
    ON collection_offers
(collector_id, collector_hidden, status, created_at);