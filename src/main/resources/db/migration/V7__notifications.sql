CREATE TABLE IF NOT EXISTS device_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL UNIQUE,
    platform VARCHAR(20) NOT NULL DEFAULT 'android',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_device_tokens_user ON device_tokens (user_id);

CREATE TABLE IF NOT EXISTS user_notifications (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(80) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    data_json TEXT,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    source_event_id VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_notifications_user_read_created
    ON user_notifications (user_id, read_flag, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_notifications_user_created
    ON user_notifications (user_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_notifications_source
    ON user_notifications (user_id, source_event_id)
    WHERE source_event_id IS NOT NULL;
