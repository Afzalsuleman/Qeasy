package com.smartqueue.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartqueue.exception.*;
import com.smartqueue.model.dto.QueueResponse;
import com.smartqueue.model.entity.QueueLog;
import com.smartqueue.model.entity.Shop;
import com.smartqueue.model.entity.User;
import com.smartqueue.model.enums.QueueStatus;
import com.smartqueue.repository.QueueLogRepository;
import com.smartqueue.repository.ShopRepository;
import com.smartqueue.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Queue service with Redis-backed queue management using Lua scripts
 * Handles atomic queue operations (join, leave, call next)
 */
@Service
@Slf4j
public class QueueService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final QueueLogRepository queueLogRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final WebSocketService webSocketService;

    private DefaultRedisScript<Long> joinQueueScript;
    private DefaultRedisScript<Long> leaveQueueScript;
    private DefaultRedisScript<String> callNextUserScript;
    private DefaultRedisScript<String> completeUserScript;

    @Autowired
    public QueueService(ShopRepository shopRepository,
                       UserRepository userRepository,
                       QueueLogRepository queueLogRepository,
                       RedisTemplate<String, String> redisTemplate,
                       ObjectMapper objectMapper,
                       WebSocketService webSocketService) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.queueLogRepository = queueLogRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.webSocketService = webSocketService;
    }

    /**
     * Load Lua scripts on initialization
     */
    @PostConstruct
    public void loadLuaScripts() {
        log.info("Loading Lua scripts for queue operations");

        // Load join_queue.lua
        joinQueueScript = new DefaultRedisScript<>();
        joinQueueScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("lua/join_queue.lua")));
        joinQueueScript.setResultType(Long.class);

        // Load leave_queue.lua
        leaveQueueScript = new DefaultRedisScript<>();
        leaveQueueScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("lua/leave_queue.lua")));
        leaveQueueScript.setResultType(Long.class);

        // Load call_next_user.lua
        callNextUserScript = new DefaultRedisScript<>();
        callNextUserScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("lua/call_next_user.lua")));
        callNextUserScript.setResultType(String.class);

        // Load complete_user.lua
        completeUserScript = new DefaultRedisScript<>();
        completeUserScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("lua/complete_user.lua")));
        completeUserScript.setResultType(String.class);

        log.info("Lua scripts loaded successfully");
    }

    /**
     * Join queue for a shop
     *
     * @param shopId Shop UUID
     * @param userEmail User's email from JWT
     * @return QueueResponse with position and wait time
     */
    @Transactional
    public QueueResponse joinQueue(UUID shopId, String userEmail) {
        log.info("User {} attempting to join queue for shop {}", userEmail, shopId);

        // Validate shop
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));

        if (!shop.getIsActive()) {
            throw new ShopInactiveException("Shop is currently inactive");
        }

        // Get user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Prepare Redis keys
        String queueKey = "queue:" + shopId;
        String usersKey = "queue:" + shopId + ":users";
        String waitingKey = "queue:" + shopId + ":waiting";

        // Execute Lua script atomically
        Long position = redisTemplate.execute(
                joinQueueScript,
                Arrays.asList(queueKey, usersKey, waitingKey),
                user.getId().toString(),
                user.getName(),
                user.getEmail(),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(shop.getMaxQueueSize())
        );

        if (position == null) {
            throw new SystemException("Failed to join queue - Redis operation failed");
        }

        if (position == -1) {
            throw new QueueFullException("Queue is full. Maximum capacity: " + shop.getMaxQueueSize());
        }

        if (position == -2) {
            throw new AlreadyInQueueException("You are already in this queue");
        }

        // Position is 0-indexed, convert to 1-indexed
        int actualPosition = position.intValue() + 1;

        // Save to database for audit
        QueueLog queueLog = QueueLog.builder()
                .shop(shop)
                .user(user)
                .status(QueueStatus.JOINED)
                .joinedAt(Instant.now())
                .build();
        queueLogRepository.save(queueLog);

        // Calculate estimated wait time
        int estimatedWaitTime = position.intValue() * shop.getAvgServiceTimeMinutes();

        log.info("User {} joined queue at position {}", userEmail, actualPosition);

        QueueResponse response = QueueResponse.builder()
                .shopId(shopId)
                .shopName(shop.getName())
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .position(actualPosition)
                .status("JOINED")
                .peopleAhead(position.intValue())
                .estimatedWaitTimeMinutes(estimatedWaitTime)
                .joinedAt(Instant.now())
                .message("You have successfully joined the queue at position " + actualPosition)
                .build();

        // Broadcast real-time update to all subscribers of this shop's queue
        webSocketService.broadcastQueueUpdate(shopId, response);

        return response;
    }

    /**
     * Leave queue
     *
     * @param shopId Shop UUID
     * @param userEmail User's email from JWT
     */
    @Transactional
    public void leaveQueue(UUID shopId, String userEmail) {
        log.info("User {} attempting to leave queue for shop {}", userEmail, shopId);

        // Validate shop
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));

        // Get user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Prepare Redis keys
        String queueKey = "queue:" + shopId;
        String usersKey = "queue:" + shopId + ":users";
        String waitingKey = "queue:" + shopId + ":waiting";
        String currentKey = "queue:" + shopId + ":current";

        // Execute Lua script atomically
        Long result = redisTemplate.execute(
                leaveQueueScript,
                Arrays.asList(queueKey, usersKey, waitingKey, currentKey),
                user.getId().toString()
        );

        if (result == null || result == 0) {
            throw new UserNotInQueueException("You are not in this queue");
        }

        if (result == -1) {
            throw new SystemException("Cannot leave queue while being served");
        }

        // Update database
        QueueLog queueLog = queueLogRepository
                .findByShopIdAndUserIdAndStatus(shopId, user.getId(), QueueStatus.JOINED)
                .orElseThrow(() -> new UserNotInQueueException("Queue log not found"));

        queueLog.setStatus(QueueStatus.LEFT);
        queueLogRepository.save(queueLog);

        log.info("User {} left queue for shop {}", userEmail, shopId);

        // Broadcast real-time update to all subscribers
        QueueResponse leaveUpdate = QueueResponse.builder()
                .shopId(shopId)
                .shopName(shop.getName())
                .userId(user.getId())
                .userName(user.getName())
                .status("LEFT")
                .message(user.getName() + " left the queue")
                .build();

        webSocketService.broadcastQueueUpdate(shopId, leaveUpdate);
    }

    /**
     * Call next user in queue (shop owner only)
     *
     * @param shopId Shop UUID
     * @param ownerEmail Shop owner's email from JWT
     * @return QueueResponse with next user details
     */
    @Transactional
    public QueueResponse callNextUser(UUID shopId, String ownerEmail) {
        log.info("Owner {} calling next user for shop {}", ownerEmail, shopId);

        // Validate shop and ownership
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!shop.getOwner().getId().equals(owner.getId())) {
            throw new UnauthorizedException("You are not authorized to call users for this shop");
        }

        if (!shop.getIsActive()) {
            throw new ShopInactiveException("Shop is currently inactive");
        }

        // Prepare Redis keys
        String queueKey = "queue:" + shopId;
        String usersKey = "queue:" + shopId + ":users";
        String waitingKey = "queue:" + shopId + ":waiting";
        String currentKey = "queue:" + shopId + ":current";
        String isCompletedKey = "queue:" + shopId + ":is_completed";

        // Execute Lua script atomically
        String resultJson = redisTemplate.execute(
                callNextUserScript,
                Arrays.asList(queueKey, usersKey, waitingKey, currentKey, isCompletedKey),
                String.valueOf(System.currentTimeMillis())
        );

        log.info("callNextUser Lua script result: {}", resultJson);

        if (resultJson == null) {
            throw new QueueEmptyException("Queue is empty - no users to call");
        }

        // Parse result JSON
        try {
            JsonNode result = objectMapper.readTree(resultJson);
            UUID userId = UUID.fromString(result.get("userId").asText());
            String userName = result.get("userName").asText();
            String userEmail = result.get("userEmail").asText();
            long joinedAtMs = result.get("joinedAt").asLong();
            long calledAtMs = result.get("calledAt").asLong();
            int position = result.get("position").asInt();
            int remainingInQueue = result.get("remainingInQueue").asInt();

            // Update database
            QueueLog queueLog = queueLogRepository
                    .findByShopIdAndUserIdAndStatus(shopId, userId, QueueStatus.JOINED)
                    .orElseThrow(() -> new SystemException("Queue log not found"));

            queueLog.setStatus(QueueStatus.CALLED);
            queueLog.setCalledAt(Instant.ofEpochMilli(calledAtMs));
            queueLogRepository.save(queueLog);

            log.info("User {} (ID: {}) called for shop {} - position set to {}, added to is_completed set",
                     userName, userId, shopId, position);

            QueueResponse response = QueueResponse.builder()
                    .shopId(shopId)
                    .shopName(shop.getName())
                    .userId(userId)
                    .userName(userName)
                    .userEmail(userEmail)
                    .position(position)
                    .status("CALLED")
                    .totalInQueue(remainingInQueue)
                    .joinedAt(Instant.ofEpochMilli(joinedAtMs))
                    .calledAt(Instant.ofEpochMilli(calledAtMs))
                    .message("Next user called: " + userName)
                    .build();

            // Broadcast to all subscribers of this shop's queue
            webSocketService.broadcastQueueUpdate(shopId, response);

            // Send personal notification to the called user
            webSocketService.notifyUserCalled(userId, response);

            return response;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Lua script result: {}", e.getMessage());
            throw new SystemException("Failed to parse queue data");
        }
    }

    /**
     * Get user's current position in queue
     *
     * @param shopId Shop UUID
     * @param userEmail User's email from JWT
     * @return QueueResponse with current position
     */
    @Transactional(readOnly = true)
    public QueueResponse getQueuePosition(UUID shopId, String userEmail) {
        log.info("Getting queue position for user {} in shop {}", userEmail, shopId);

        // Validate shop
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));

        // Get user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Check if user is currently being called (in is_completed set)
        String isCompletedKey = "queue:" + shopId + ":is_completed";
        Double calledScore = redisTemplate.opsForZSet().score(isCompletedKey, user.getId().toString());

        log.debug("Checking is_completed set for user {} in shop {}: score = {}", user.getId(), shopId, calledScore);

        if (calledScore != null) {
            // User is currently being CALLED (position 0)
            Long totalSize = redisTemplate.opsForZSet().zCard("queue:" + shopId + ":waiting");
            int total = totalSize != null ? totalSize.intValue() : 0;

            log.info("User {} is in CALLED state with position 0", user.getId());

            return QueueResponse.builder()
                    .shopId(shopId)
                    .shopName(shop.getName())
                    .userId(user.getId())
                    .userName(user.getName())
                    .userEmail(user.getEmail())
                    .position(0)
                    .status("CALLED")
                    .totalInQueue(total)
                    .peopleAhead(0)
                    .estimatedWaitTimeMinutes(0)
                    .message("You are currently being served")
                    .build();
        }

        // Get position from the WAITING set (excludes CALLED users)
        // This ensures accurate position calculation after callNextUser()
        String waitingKey = "queue:" + shopId + ":waiting";
        Long position = redisTemplate.opsForZSet().rank(waitingKey, user.getId().toString());

        if (position == null) {
            throw new UserNotInQueueException("You are not in this queue");
        }

        // Position is 0-indexed, convert to 1-indexed
        int actualPosition = position.intValue() + 1;
        int estimatedWaitTime = position.intValue() * shop.getAvgServiceTimeMinutes();

        // Get total waiting queue size (excludes users currently being CALLED)
        Long totalSize = redisTemplate.opsForZSet().zCard(waitingKey);
        int total = totalSize != null ? totalSize.intValue() : 0;

        return QueueResponse.builder()
                .shopId(shopId)
                .shopName(shop.getName())
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .position(actualPosition)
                .status("JOINED")
                .totalInQueue(total)
                .peopleAhead(position.intValue())
                .estimatedWaitTimeMinutes(estimatedWaitTime)
                .build();
    }

    /**
     * Mark user as served/completed (user self-completes after being served)
     *
     * @param shopId Shop UUID
     * @param userEmail User's email from JWT
     * @return QueueResponse with completed user details
     */
    @Transactional
    public QueueResponse completeUser(UUID shopId, String userEmail) {
        log.info("User {} marking themselves as completed for shop {}", userEmail, shopId);

        // Validate shop
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));

        // Get user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!shop.getIsActive()) {
            throw new ShopInactiveException("Shop is currently inactive");
        }

        // Prepare Redis keys
        String queueKey = "queue:" + shopId;
        String usersKey = "queue:" + shopId + ":users";
        String waitingKey = "queue:" + shopId + ":waiting";
        String currentKey = "queue:" + shopId + ":current";
        String isCompletedKey = "queue:" + shopId + ":is_completed";

        // Execute Lua script atomically (pass userId and timestamp as ARGV)
        String resultJson = redisTemplate.execute(
                completeUserScript,
                Arrays.asList(queueKey, usersKey, waitingKey, currentKey, isCompletedKey),
                user.getId().toString(),
                String.valueOf(System.currentTimeMillis())
        );

        if (resultJson == null) {
            throw new SystemException("Failed to complete user");
        }

        // Parse result JSON
        try {
            JsonNode result = objectMapper.readTree(resultJson);

            // Check for error
            if (result.has("error")) {
                String error = result.get("error").asText();
                String message = result.get("message").asText();
                log.warn("Failed to complete user: {} - {}", error, message);

                // Handle specific error cases
                if ("USER_NOT_CALLED".equals(error)) {
                    throw new UserNotInQueueException(message);
                } else if ("USER_NOT_FOUND".equals(error)) {
                    throw new SystemException(message);
                } else {
                    throw new SystemException(message);
                }
            }

            UUID completedUserId = UUID.fromString(result.get("userId").asText());
            String userName = result.get("userName").asText();
            String completedUserEmail = result.get("userEmail").asText();
            long joinedAtMs = result.get("joinedAt").asLong();
            long calledAtMs = result.get("calledAt").asLong();
            long servedAtMs = result.get("servedAt").asLong();
            int position = result.get("position").asInt();
            int remainingInQueue = result.get("remainingInQueue").asInt();

            // Update database
            QueueLog queueLog = queueLogRepository
                    .findByShopIdAndUserIdAndStatus(shopId, completedUserId, QueueStatus.CALLED)
                    .orElseThrow(() -> new SystemException("Queue log not found for user being served"));

            queueLog.setStatus(QueueStatus.SERVED);
            queueLog.setCompletedAt(Instant.ofEpochMilli(servedAtMs));
            queueLogRepository.save(queueLog);

            log.info("User {} marked as served for shop {}", userName, shopId);

            QueueResponse response = QueueResponse.builder()
                    .shopId(shopId)
                    .shopName(shop.getName())
                    .userId(completedUserId)
                    .userName(userName)
                    .userEmail(completedUserEmail)
                    .position(position)
                    .status("SERVED")
                    .totalInQueue(remainingInQueue)
                    .joinedAt(Instant.ofEpochMilli(joinedAtMs))
                    .calledAt(Instant.ofEpochMilli(calledAtMs))
                    .servedAt(Instant.ofEpochMilli(servedAtMs))
                    .message("You have been served successfully")
                    .build();

            // Broadcast to all subscribers of this shop's queue
            webSocketService.broadcastQueueUpdate(shopId, response);

            // Send personal notification to the served user
            webSocketService.notifyUserServed(completedUserId, response);

            return response;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Lua script result: {}", e.getMessage());
            throw new SystemException("Failed to parse queue data");
        }
    }
}
