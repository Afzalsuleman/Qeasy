-- Smart Queue: Update Failed Emails Table
-- Description: Add new columns for retry mechanism and email tracking

-- Add new columns to failed_emails table
ALTER TABLE failed_emails
ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS retried_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS email_body TEXT,
ADD COLUMN IF NOT EXISTS failed_at TIMESTAMP;

-- Update existing records to set failed_at from created_at if null
UPDATE failed_emails
SET failed_at = created_at
WHERE failed_at IS NULL;

-- Comments
COMMENT ON COLUMN failed_emails.retry_count IS 'Number of retry attempts';
COMMENT ON COLUMN failed_emails.retried_at IS 'Timestamp of last retry attempt';
COMMENT ON COLUMN failed_emails.email_body IS 'Content of the failed email';
COMMENT ON COLUMN failed_emails.failed_at IS 'Timestamp when email failed';
