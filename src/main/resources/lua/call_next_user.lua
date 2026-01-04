-- call_next_user.lua
-- Atomically mark current user as served and call the next user in queue
-- KEYS[1]: queue:<shopId> (sorted set - queue positions)
-- KEYS[2]: queue:<shopId>:users (hash - user details)
-- KEYS[3]: queue:<shopId>:waiting (sorted set - waiting users by join timestamp)
-- KEYS[4]: queue:<shopId>:current (string - current user being served)
-- ARGV[1]: currentTimestamp (milliseconds since epoch)
-- RETURNS: JSON string with next user details or nil if queue is empty

-- Mark current user as served (if exists)
local currentUser = redis.call('GET', KEYS[4])
if currentUser then
    -- Remove current user from all structures
    redis.call('ZREM', KEYS[1], currentUser)
    redis.call('HDEL', KEYS[2], currentUser)
    redis.call('ZREM', KEYS[3], currentUser)
    redis.call('DEL', KEYS[4])
end

-- Get the next user in queue (lowest score = earliest join time)
local nextUsers = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')
if #nextUsers == 0 then
    return nil  -- Queue is empty
end

local nextUserId = nextUsers[1]

-- Get user details
local userDetailsJson = redis.call('HGET', KEYS[2], nextUserId)
if not userDetailsJson then
    -- Inconsistent state, remove from queue and try again
    redis.call('ZREM', KEYS[1], nextUserId)
    redis.call('ZREM', KEYS[3], nextUserId)
    return nil
end

-- Update user status to CALLED
local userDetails = cjson.decode(userDetailsJson)
userDetails.status = 'CALLED'
userDetails.calledAt = ARGV[1]
redis.call('HSET', KEYS[2], nextUserId, cjson.encode(userDetails))

-- Set as current user
redis.call('SET', KEYS[4], nextUserId)

-- Remove from waiting set (still in main queue until served)
redis.call('ZREM', KEYS[3], nextUserId)

-- Calculate remaining queue size
local remainingInQueue = redis.call('ZCARD', KEYS[3])

-- Return next user details with queue info
return cjson.encode({
    userId = userDetails.userId,
    userName = userDetails.userName,
    userEmail = userDetails.userEmail,
    joinedAt = userDetails.joinedAt,
    calledAt = userDetails.calledAt,
    status = 'CALLED',
    remainingInQueue = tonumber(remainingInQueue)
})
