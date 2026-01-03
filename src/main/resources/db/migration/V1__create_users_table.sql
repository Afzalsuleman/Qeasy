-- Smart Queue: Users Table
-- Description: Stores user accounts (email-based authentication)
-- Rollback: DROP TABLE users CASCADE;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

-- Indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Comments for documentation
COMMENT ON TABLE users IS 'User accounts for Smart Queue system';
COMMENT ON COLUMN users.email IS 'User email address (used for OTP authentication)';
COMMENT ON COLUMN users.phone IS 'Optional phone number (for future SMS feature)';
