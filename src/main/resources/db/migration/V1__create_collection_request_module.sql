CREATE TABLE IF NOT EXISTS collection_requests (
    id BIGSERIAL PRIMARY KEY,
    citizen_id BIGINT NOT NULL,
    waste_type VARCHAR(20) NOT NULL,
    quantity_label VARCHAR(50) NOT NULL,
    quantity_kg_estimate DOUBLE PRECISION,
    address_line VARCHAR(500) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    preferred_date DATE NOT NULL,
    preferred_slot VARCHAR(20) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    notes TEXT,
    photo_url VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    accepted_offer_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS collection_offers (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    collector_id BIGINT NOT NULL,
    price_per_unit DOUBLE PRECISION NOT NULL,
    price_unit VARCHAR(20) NOT NULL,
    proposed_pickup_at TIMESTAMP WITH TIME ZONE NOT NULL,
    message_to_citizen VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    cancellation_reason VARCHAR(30),
    cancellation_note VARCHAR(500),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    completion_photo_url VARCHAR(500),
    completion_weight_kg DOUBLE PRECISION,
    completion_lat DOUBLE PRECISION,
    completion_lng DOUBLE PRECISION,
    completion_notes TEXT,
    citizen_rating INTEGER,
    citizen_feedback TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cr_status_created
    ON collection_requests (status, created_at);

CREATE INDEX IF NOT EXISTS idx_cr_citizen_status
    ON collection_requests (citizen_id, status);

CREATE INDEX IF NOT EXISTS idx_co_request
    ON collection_offers (request_id);

CREATE INDEX IF NOT EXISTS idx_co_collector_status
    ON collection_offers (collector_id, status);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_collection_requests_citizen'
    ) THEN
        ALTER TABLE collection_requests
            ADD CONSTRAINT fk_collection_requests_citizen
            FOREIGN KEY (citizen_id) REFERENCES citizens(emp_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_collection_offers_request'
    ) THEN
        ALTER TABLE collection_offers
            ADD CONSTRAINT fk_collection_offers_request
            FOREIGN KEY (request_id) REFERENCES collection_requests(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_collection_offers_collector'
    ) THEN
        ALTER TABLE collection_offers
            ADD CONSTRAINT fk_collection_offers_collector
            FOREIGN KEY (collector_id) REFERENCES third_party_collectors(emp_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_collection_requests_accepted_offer'
    ) THEN
        ALTER TABLE collection_requests
            ADD CONSTRAINT fk_collection_requests_accepted_offer
            FOREIGN KEY (accepted_offer_id) REFERENCES collection_offers(id);
    END IF;
END $$;
