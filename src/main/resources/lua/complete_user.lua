-- complete_user.lua
-- Atomically mark current user as served/completed and remove from queue
-- KEYS[1]: queue:<shopId> (sorted set - queue positions)
-- KEYS[2]: queue:<shopId>:users (hash - user details)
-- KEYS[3]: queue:<shopId>:waiting (sorted set - waiting users by join timestamp)
-- KEYS[4]: queue:<shopId>:current (string - current user being served)
-- ARGV[1]: currentTimestamp (milliseconds since epoch)
-- RETURNS: JSON string with completed user details or error

-- Get current user being served
local currentUser = redis.call('GET', KEYS[4])
if not currentUser then
    return cjson.encode({
        error = 'NO_CURRENT_USER',
        message = 'No user is currently being served'
    })
end

-- Get user details
local userDetailsJson = redis.call('HGET', KEYS[2], currentUser)
if not userDetailsJson then
    -- Clean up inconsistent state
    redis.call('DEL', KEYS[4])
    return cjson.encode({
        error = 'USER_NOT_FOUND',
        message = 'User details not found in queue'
    })
end

-- Parse user details
local userDetails = cjson.decode(userDetailsJson)

-- Update user status to SERVED
userDetails.status = 'SERVED'
userDetails.servedAt = ARGV[1]

-- Remove user from all queue structures
redis.call('ZREM', KEYS[1], currentUser)
redis.call('HDEL', KEYS[2], currentUser)
redis.call('ZREM', KEYS[3], currentUser)
redis.call('DEL', KEYS[4])

-- Calculate remaining queue size
local remainingInQueue = redis.call('ZCARD', KEYS[3])

-- Return completed user details
return cjson.encode({
    userId = userDetails.userId,
    userName = userDetails.userName,
    userEmail = userDetails.userEmail,
    joinedAt = userDetails.joinedAt,
    calledAt = userDetails.calledAt,
    servedAt = userDetails.servedAt,
    status = 'SERVED',
    remainingInQueue = tonumber(remainingInQueue)
})
