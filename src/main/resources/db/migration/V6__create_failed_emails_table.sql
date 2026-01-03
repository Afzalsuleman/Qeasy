-- Smart Queue: Failed Emails Table
-- Description: Store failed email attempts for manual retry
-- Rollback: DROP TABLE failed_emails CASCADE;

CREATE TABLE failed_emails (
    id BIGSERIAL PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    failure_reason TEXT,
    attempt_count INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP,
    retry_after TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT status_valid CHECK (status IN ('PENDING', 'RETRYING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX idx_failed_emails_status ON failed_emails(status, retry_after);
CREATE INDEX idx_failed_emails_created_at ON failed_emails(created_at DESC)
WHERE status IN ('PENDING', 'RETRYING');

COMMENT ON TABLE failed_emails IS 'Failed email attempts for manual retry';
COMMENT ON COLUMN failed_emails.retry_after IS 'Timestamp for next retry attempt (exponential backoff)';
