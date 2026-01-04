-- Smart Queue: Add Email Type Column to Failed Emails
-- Description: Add email_type column to distinguish OTP emails from other email types
-- OTP emails should not be retried automatically since OTP expires in 5 minutes

-- Add email_type column to failed_emails table
ALTER TABLE failed_emails
ADD COLUMN IF NOT EXISTS email_type VARCHAR(20) NOT NULL DEFAULT 'NOTIFICATION';

-- Add check constraint to ensure valid email types
ALTER TABLE failed_emails
ADD CONSTRAINT email_type_valid CHECK (email_type IN ('OTP', 'NOTIFICATION', 'QUEUE_UPDATE', 'SYSTEM_ALERT'));

-- Create index on email_type column for faster queries
CREATE INDEX IF NOT EXISTS idx_failed_emails_email_type ON failed_emails(email_type);

-- Create composite index for scheduler query optimization (status + email_type)
CREATE INDEX IF NOT EXISTS idx_failed_emails_status_type ON failed_emails(status, email_type);

-- Comments
COMMENT ON COLUMN failed_emails.email_type IS 'Email type (OTP, NOTIFICATION, QUEUE_UPDATE, SYSTEM_ALERT) for retry behavior control';
