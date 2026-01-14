-- complete_user.lua
-- Atomically mark user as served/completed and remove from queue
-- User must be in is_completed set to complete (i.e., must have been CALLED)
-- KEYS[1]: queue:<shopId> (sorted set - queue positions)
-- KEYS[2]: queue:<shopId>:users (hash - user details)
-- KEYS[3]: queue:<shopId>:waiting (sorted set - waiting users by join timestamp)
-- KEYS[4]: queue:<shopId>:current (string - current user being served)
-- KEYS[5]: queue:<shopId>:is_completed (sorted set - users being served, awaiting completion)
-- ARGV[1]: userId (user marking themselves as completed)
-- ARGV[2]: currentTimestamp (milliseconds since epoch)
-- RETURNS: JSON string with completed user details or error

-- Verify user is in the is_completed set (was CALLED)
local score = redis.call('ZSCORE', KEYS[5], ARGV[1])
if not score then
    return cjson.encode({
        error = 'USER_NOT_CALLED',
        message = 'You have not been called yet or already completed'
    })
end

-- Get user details
local userDetailsJson = redis.call('HGET', KEYS[2], ARGV[1])
if not userDetailsJson then
    -- Clean up inconsistent state
    redis.call('ZREM', KEYS[5], ARGV[1])
    return cjson.encode({
        error = 'USER_NOT_FOUND',
        message = 'User details not found in queue'
    })
end

-- Parse user details
local userDetails = cjson.decode(userDetailsJson)

-- Update user status to SERVED
userDetails.status = 'SERVED'
userDetails.servedAt = ARGV[2]

-- Remove user from all queue structures including is_completed
redis.call('ZREM', KEYS[1], ARGV[1])
redis.call('HDEL', KEYS[2], ARGV[1])
redis.call('ZREM', KEYS[3], ARGV[1])
redis.call('ZREM', KEYS[5], ARGV[1])

-- Clear current user if this was the current user
local currentUser = redis.call('GET', KEYS[4])
if currentUser == ARGV[1] then
    redis.call('DEL', KEYS[4])
end

-- Calculate remaining queue size
local remainingInQueue = redis.call('ZCARD', KEYS[3])

-- Return completed user details with position 0
return cjson.encode({
    userId = userDetails.userId,
    userName = userDetails.userName,
    userEmail = userDetails.userEmail,
    joinedAt = userDetails.joinedAt,
    calledAt = userDetails.calledAt,
    servedAt = userDetails.servedAt,
    status = 'SERVED',
    position = 0,
    remainingInQueue = tonumber(remainingInQueue)
})
