-- Smart Queue: Additional Performance Indexes
-- Description: Optimize common query patterns

-- Shop queries by activity status
CREATE INDEX IF NOT EXISTS idx_shops_active_created
ON shops(is_active, created_at DESC)
WHERE is_active = true;

-- Queue logs for recent activity - removed time-based WHERE clause (not immutable)
-- Note: Time-based filtering should be done in application queries
CREATE INDEX IF NOT EXISTS idx_queue_logs_recent
ON queue_logs(shop_id, joined_at DESC);

-- Comments
COMMENT ON INDEX idx_shops_active_created IS 'Optimize queries for active shops list';
COMMENT ON INDEX idx_queue_logs_recent IS 'Optimize queries for queue activity by shop';
