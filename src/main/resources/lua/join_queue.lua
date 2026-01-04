-- join_queue.lua
-- Atomically add a user to the shop's queue
-- KEYS[1]: queue:<shopId> (sorted set - queue positions)
-- KEYS[2]: queue:<shopId>:users (hash - user details)
-- KEYS[3]: queue:<shopId>:waiting (sorted set - waiting users by join timestamp)
-- ARGV[1]: userId (UUID string)
-- ARGV[2]: userName (string)
-- ARGV[3]: userEmail (string)
-- ARGV[4]: currentTimestamp (milliseconds since epoch)
-- ARGV[5]: maxQueueSize (integer)
-- RETURNS: position in queue (integer) or -1 if queue is full, -2 if already in queue

-- Check if user is already in queue
local existingPosition = redis.call('ZSCORE', KEYS[1], ARGV[1])
if existingPosition then
    return -2  -- User already in queue
end

-- Check current queue size
local currentSize = redis.call('ZCARD', KEYS[1])
if tonumber(currentSize) >= tonumber(ARGV[5]) then
    return -1  -- Queue is full
end

-- Add user to queue with timestamp as score
redis.call('ZADD', KEYS[1], ARGV[4], ARGV[1])

-- Store user details in hash
redis.call('HSET', KEYS[2], ARGV[1], cjson.encode({
    userId = ARGV[1],
    userName = ARGV[2],
    userEmail = ARGV[3],
    joinedAt = ARGV[4],
    status = 'JOINED'
}))

-- Add to waiting sorted set (sorted by join timestamp)
redis.call('ZADD', KEYS[3], ARGV[4], ARGV[1])

-- Calculate position (0-indexed, we'll add 1 in Java)
local position = redis.call('ZRANK', KEYS[1], ARGV[1])

return position
