-- Smart Queue: Add password fields to users table
-- Description: Add password and passwordSet columns to support shop owner authentication

-- Add password column to users table
ALTER TABLE users
ADD COLUMN IF NOT EXISTS password VARCHAR(255);

-- Add passwordSet column with default value false
ALTER TABLE users
ADD COLUMN IF NOT EXISTS password_set BOOLEAN NOT NULL DEFAULT false;

-- Create index for future queries
CREATE INDEX IF NOT EXISTS idx_users_password_set ON users(password_set);

-- Comments
COMMENT ON COLUMN users.password IS 'Bcrypt-hashed password for shop owners and admins';
COMMENT ON COLUMN users.password_set IS 'Flag indicating if user has set their password (shop owners invited by admin)';
