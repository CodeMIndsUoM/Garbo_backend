CREATE TABLE IF NOT EXISTS bin_suggestions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mentor_id BIGINT NOT NULL,
    mentor_name VARCHAR(255),
    council VARCHAR(100) NOT NULL,
    location VARCHAR(100),
    lat DOUBLE,
    lng DOUBLE,
    category VARCHAR(50),
    notes TEXT,
    image_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING',
    resolution_notes TEXT,
    created_bin_id BIGINT,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE INDEX idx_bin_suggestions_council ON bin_suggestions (council);
CREATE INDEX idx_bin_suggestions_mentor ON bin_suggestions (mentor_id);
CREATE INDEX idx_bin_suggestions_status ON bin_suggestions (status);
