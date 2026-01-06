-- Smart Queue: Update Role Constraint to Support ADMIN Role
-- Description: Update role_valid constraint to include ADMIN role for system administration

-- Drop existing check constraint
ALTER TABLE users
DROP CONSTRAINT role_valid;

-- Add new check constraint with ADMIN role included
ALTER TABLE users
ADD CONSTRAINT role_valid CHECK (role IN ('USER', 'SHOP_OWNER', 'ADMIN'));

-- Comment
COMMENT ON CONSTRAINT role_valid ON users IS 'Valid roles: USER (regular customer), SHOP_OWNER (shop administrator), ADMIN (system administrator)';
