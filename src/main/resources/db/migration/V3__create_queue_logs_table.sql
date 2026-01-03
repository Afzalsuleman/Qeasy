-- Smart Queue: Queue Logs Table
-- Description: Audit trail of all queue operations
-- Rollback: DROP TABLE queue_logs CASCADE;

CREATE TABLE queue_logs (
    id BIGSERIAL PRIMARY KEY,
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    position INTEGER,
    estimated_wait_minutes INTEGER,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    called_at TIMESTAMP,
    completed_at TIMESTAMP,
    notes TEXT,
    CONSTRAINT status_valid CHECK (status IN ('JOINED', 'CALLED', 'SERVED', 'LEFT', 'NO_SHOW', 'CANCELLED'))
);

-- Indexes for common queries
CREATE INDEX idx_queue_logs_shop_id ON queue_logs(shop_id, joined_at DESC);
CREATE INDEX idx_queue_logs_user_id ON queue_logs(user_id, joined_at DESC);
CREATE INDEX idx_queue_logs_status ON queue_logs(status);
CREATE INDEX idx_queue_logs_joined_at ON queue_logs(joined_at);

-- Composite index for analytics queries
CREATE INDEX idx_queue_logs_shop_status_time
ON queue_logs(shop_id, status, joined_at DESC);

-- Comments
COMMENT ON TABLE queue_logs IS 'Audit trail of queue operations (30-day retention)';
COMMENT ON COLUMN queue_logs.status IS 'Queue operation status: JOINED, CALLED, SERVED, LEFT, NO_SHOW, CANCELLED';
