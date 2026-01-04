-- Smart Queue: Add User Role Column
-- Description: Add role column to users table for role-based access control

-- Add role column to users table
ALTER TABLE users
ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Add check constraint to ensure valid roles
ALTER TABLE users
ADD CONSTRAINT role_valid CHECK (role IN ('USER', 'SHOP_OWNER'));

-- Update existing shop owners to SHOP_OWNER role
-- Users who have a shop in the shops table should be SHOP_OWNER
UPDATE users
SET role = 'SHOP_OWNER'
WHERE id IN (
    SELECT DISTINCT owner_id
    FROM shops
    WHERE is_active = true
);

-- Create index on role column for faster queries
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- Comments
COMMENT ON COLUMN users.role IS 'User role (USER or SHOP_OWNER) for access control';
