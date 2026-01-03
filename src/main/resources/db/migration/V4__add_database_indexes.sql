-- Smart Queue: Additional Performance Indexes
-- Description: Optimize common query patterns

-- Shop queries by activity status
CREATE INDEX IF NOT EXISTS idx_shops_active_created
ON shops(is_active, created_at DESC)
WHERE is_active = true;

-- Queue logs for recent activity (last 7 days analytics)
CREATE INDEX IF NOT EXISTS idx_queue_logs_recent
ON queue_logs(shop_id, joined_at DESC)
WHERE joined_at > CURRENT_TIMESTAMP - INTERVAL '7 days';

-- Comments
COMMENT ON INDEX idx_shops_active_created IS 'Optimize queries for active shops list';
COMMENT ON INDEX idx_queue_logs_recent IS 'Optimize queries for recent queue activity (7 days)';
