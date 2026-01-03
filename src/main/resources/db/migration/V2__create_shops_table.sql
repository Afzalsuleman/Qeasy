-- Smart Queue: Shops Table
-- Description: Stores shop information and configuration
-- Rollback: DROP TABLE shops CASCADE;

CREATE TABLE shops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    avg_service_time_minutes INTEGER NOT NULL DEFAULT 15,
    max_queue_size INTEGER NOT NULL DEFAULT 50,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT avg_service_time_positive CHECK (avg_service_time_minutes > 0),
    CONSTRAINT max_queue_size_positive CHECK (max_queue_size > 0 AND max_queue_size <= 1000)
);

-- Indexes
CREATE INDEX idx_shops_owner_id ON shops(owner_id);
CREATE INDEX idx_shops_is_active ON shops(is_active);

-- Unique constraint: One active shop per owner (MVP requirement)
CREATE UNIQUE INDEX idx_shops_owner_id_unique
ON shops(owner_id)
WHERE is_active = true;

-- Comments
COMMENT ON TABLE shops IS 'Shop profiles and configuration';
COMMENT ON COLUMN shops.avg_service_time_minutes IS 'Average time to serve one customer (for wait time estimation)';
COMMENT ON COLUMN shops.max_queue_size IS 'Maximum number of users allowed in queue';
