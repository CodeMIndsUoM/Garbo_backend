CREATE TABLE IF NOT EXISTS collection_request_waste_types (
    request_id BIGINT NOT NULL,
    waste_type VARCHAR(20) NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (request_id, waste_type),
    CONSTRAINT fk_cr_waste_types_request
        FOREIGN KEY (request_id) REFERENCES collection_requests(id) ON DELETE CASCADE
);

INSERT INTO collection_request_waste_types (request_id, waste_type, sort_order)
SELECT id, waste_type, 0
FROM collection_requests
ON CONFLICT DO NOTHING;
