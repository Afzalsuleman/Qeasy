-- call_next_user.lua
-- Atomically mark current user as served and call the next user in queue
-- KEYS[1]: queue:<shopId> (sorted set - queue positions)
-- KEYS[2]: queue:<shopId>:users (hash - user details)
-- KEYS[3]: queue:<shopId>:waiting (sorted set - waiting users by join timestamp)
-- KEYS[4]: queue:<shopId>:current (string - current user being served)
-- KEYS[5]: queue:<shopId>:is_completed (sorted set - users being served, awaiting completion)
-- ARGV[1]: currentTimestamp (milliseconds since epoch)
-- RETURNS: JSON string with next user details or nil if queue is empty

-- Get the next user in queue (lowest score = earliest join time)
local nextUsers = redis.call('ZRANGE', KEYS[3], 0, 0, 'WITHSCORES')
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

-- Remove from waiting set so other users move up in positions
redis.call('ZREM', KEYS[3], nextUserId)

-- Keep in main queue but with position 0 (not removed yet)
-- User stays in queue:users hash and queue:<shopId> sorted set

-- Add to is_completed set with current timestamp as score
redis.call('ZADD', KEYS[5], ARGV[1], nextUserId)

-- Calculate remaining queue size (excludes called user)
local remainingInQueue = redis.call('ZCARD', KEYS[3])

-- Return next user details with position 0
return cjson.encode({
    userId = userDetails.userId,
    userName = userDetails.userName,
    userEmail = userDetails.userEmail,
    joinedAt = userDetails.joinedAt,
    calledAt = userDetails.calledAt,
    status = 'CALLED',
    position = 0,
    remainingInQueue = tonumber(remainingInQueue)
})
