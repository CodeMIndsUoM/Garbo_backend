CREATE TABLE IF NOT EXISTS bin_suggestions (
    id BIGSERIAL PRIMARY KEY,
    mentor_id BIGINT NOT NULL,
    mentor_name VARCHAR(255),
    council VARCHAR(100) NOT NULL,
    location VARCHAR(100),
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    category VARCHAR(50),
    notes TEXT,
    image_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING',
    resolution_notes TEXT,
    created_bin_id BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_bin_suggestions_council ON bin_suggestions (council);
CREATE INDEX IF NOT EXISTS idx_bin_suggestions_mentor ON bin_suggestions (mentor_id);
CREATE INDEX IF NOT EXISTS idx_bin_suggestions_status ON bin_suggestions (status);
