-- leave_queue.lua
-- Atomically remove a user from the shop's queue
-- KEYS[1]: queue:<shopId> (sorted set - queue positions)
-- KEYS[2]: queue:<shopId>:users (hash - user details)
-- KEYS[3]: queue:<shopId>:waiting (sorted set - waiting users by join timestamp)
-- KEYS[4]: queue:<shopId>:current (string - current user being served)
-- ARGV[1]: userId (UUID string)
-- RETURNS: 1 if removed successfully, 0 if user not in queue, -1 if user is currently being served

-- Check if user is the current user being served
local currentUser = redis.call('GET', KEYS[4])
if currentUser == ARGV[1] then
    return -1  -- Cannot leave while being served
end

-- Check if user is in queue
local existingPosition = redis.call('ZSCORE', KEYS[1], ARGV[1])
if not existingPosition then
    return 0  -- User not in queue
end

-- Remove user from all queue structures
redis.call('ZREM', KEYS[1], ARGV[1])
redis.call('HDEL', KEYS[2], ARGV[1])
redis.call('ZREM', KEYS[3], ARGV[1])

return 1  -- Successfully removed
