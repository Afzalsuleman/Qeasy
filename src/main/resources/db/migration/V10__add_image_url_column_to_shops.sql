-- Smart Queue: Add Image URL Column to Shops
-- Description: Add image_url column to shops table for shop branding

-- Add image_url column to shops table
ALTER TABLE shops
ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);

-- Create index for future queries
CREATE INDEX IF NOT EXISTS idx_shops_image_url ON shops(image_url);

-- Comments
COMMENT ON COLUMN shops.image_url IS 'URL to shop profile image for branding purposes';
