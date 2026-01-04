package com.smartqueue.scheduler;

import com.smartqueue.model.dto.QueueResponse;
import com.smartqueue.model.entity.QueueLog;
import com.smartqueue.model.enums.QueueStatus;
import com.smartqueue.repository.QueueLogRepository;
import com.smartqueue.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Scheduled task to detect and remove stale queue entries
 * Auto-marks users as NO_SHOW if they don't respond within 15 minutes after being CALLED
 * Runs every hour to check for stale entries
 * Uses ShedLock for distributed locking in multi-instance deployments
 */
@Component
@Slf4j
public class StaleQueueCleanupScheduler {

    private static final int NO_SHOW_TIMEOUT_MINUTES = 15;

    private final QueueLogRepository queueLogRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final WebSocketService webSocketService;

    @Autowired
    public StaleQueueCleanupScheduler(
            QueueLogRepository queueLogRepository,
            RedisTemplate<String, String> redisTemplate,
            WebSocketService webSocketService) {
        this.queueLogRepository = queueLogRepository;
        this.redisTemplate = redisTemplate;
        this.webSocketService = webSocketService;
    }

    /**
     * Detect and mark stale queue entries as NO_SHOW
     * Scheduled to run every hour
     * ShedLock ensures only one instance runs this task
     */
    @Scheduled(cron = "0 0 * * * ?") // Every hour at minute 0
    @SchedulerLock(
            name = "StaleQueueCleanupScheduler_detectNoShows",
            lockAtMostFor = "55m",
            lockAtLeastFor = "5m"
    )
    @Transactional
    public void detectNoShows() {
        log.info("Starting stale queue cleanup task (NO_SHOW detection)");

        try {
            // Calculate cutoff time (15 minutes ago)
            Instant cutoffTime = Instant.now().minus(NO_SHOW_TIMEOUT_MINUTES, ChronoUnit.MINUTES);

            // Find all users who were CALLED but haven't been marked as SERVED
            List<QueueLog> calledUsers = queueLogRepository.findByStatusAndCalledAtBefore(
                    QueueStatus.CALLED,
                    cutoffTime
            );

            if (calledUsers.isEmpty()) {
                log.debug("No stale queue entries found");
                return;
            }

            log.info("Found {} users who may be NO_SHOW", calledUsers.size());

            int noShowCount = 0;

            for (QueueLog queueLog : calledUsers) {
                try {
                    // Mark as NO_SHOW in database
                    queueLog.setStatus(QueueStatus.NO_SHOW);
                    queueLogRepository.save(queueLog);

                    // Remove from Redis queue structures
                    removeFromRedis(queueLog.getShop().getId(), queueLog.getUser().getId());

                    // Broadcast NO_SHOW notification
                    QueueResponse noShowNotification = QueueResponse.builder()
                            .shopId(queueLog.getShop().getId())
                            .shopName(queueLog.getShop().getName())
                            .userId(queueLog.getUser().getId())
                            .userName(queueLog.getUser().getName())
                            .status("NO_SHOW")
                            .message("User did not respond and was marked as NO_SHOW")
                            .build();

                    // Broadcast to shop queue subscribers
                    webSocketService.broadcastQueueUpdate(
                            queueLog.getShop().getId(),
                            noShowNotification
                    );

                    noShowCount++;

                    log.info("Marked user {} as NO_SHOW for shop {} (called at: {}, timeout: {} minutes)",
                            queueLog.getUser().getName(),
                            queueLog.getShop().getName(),
                            queueLog.getCalledAt(),
                            NO_SHOW_TIMEOUT_MINUTES);

                } catch (Exception e) {
                    log.error("Error processing NO_SHOW for queue log {}: {}",
                            queueLog.getId(), e.getMessage(), e);
                    // Continue processing other entries even if one fails
                }
            }

            log.info("Stale queue cleanup completed. Marked {} users as NO_SHOW", noShowCount);

        } catch (Exception e) {
            log.error("Error during stale queue cleanup task: {}", e.getMessage(), e);
            throw e; // Re-throw to ensure proper transaction rollback
        }
    }

    /**
     * Remove user from Redis queue structures
     * Cleans up sorted set, hash, and current user tracking
     *
     * @param shopId Shop UUID
     * @param userId User UUID
     */
    private void removeFromRedis(UUID shopId, UUID userId) {
        String queueKey = "queue:" + shopId;
        String usersKey = "queue:" + shopId + ":users";
        String waitingKey = "queue:" + shopId + ":waiting";
        String currentKey = "queue:" + shopId + ":current";

        try {
            // Remove from sorted set (queue)
            redisTemplate.opsForZSet().remove(queueKey, userId.toString());

            // Remove user details from hash
            redisTemplate.opsForHash().delete(usersKey, userId.toString());

            // Remove from waiting set
            redisTemplate.opsForSet().remove(waitingKey, userId.toString());

            // Clear current user if it matches
            String currentUser = redisTemplate.opsForValue().get(currentKey);
            if (currentUser != null && currentUser.equals(userId.toString())) {
                redisTemplate.delete(currentKey);
            }

            log.debug("Removed user {} from Redis queue structures for shop {}", userId, shopId);

        } catch (Exception e) {
            log.error("Error removing user {} from Redis for shop {}: {}",
                    userId, shopId, e.getMessage());
            // Don't throw - cleanup is best effort
        }
    }

    /**
     * Manual method to mark specific user as NO_SHOW
     * Can be called from admin API or shop owner action
     *
     * @param shopId Shop UUID
     * @param userId User UUID
     * @return true if successful
     */
    @Transactional
    public boolean markUserAsNoShow(UUID shopId, UUID userId) {
        log.info("Manual NO_SHOW requested for user {} in shop {}", userId, shopId);

        try {
            // Find the CALLED queue log
            QueueLog queueLog = queueLogRepository
                    .findByShopIdAndUserIdAndStatus(shopId, userId, QueueStatus.CALLED)
                    .orElse(null);

            if (queueLog == null) {
                log.warn("Cannot mark user {} as NO_SHOW - not in CALLED status", userId);
                return false;
            }

            // Mark as NO_SHOW
            queueLog.setStatus(QueueStatus.NO_SHOW);
            queueLogRepository.save(queueLog);

            // Remove from Redis
            removeFromRedis(shopId, userId);

            // Broadcast notification
            QueueResponse noShowNotification = QueueResponse.builder()
                    .shopId(shopId)
                    .shopName(queueLog.getShop().getName())
                    .userId(userId)
                    .userName(queueLog.getUser().getName())
                    .status("NO_SHOW")
                    .message("User marked as NO_SHOW")
                    .build();

            webSocketService.broadcastQueueUpdate(shopId, noShowNotification);

            log.info("Successfully marked user {} as NO_SHOW for shop {}", userId, shopId);
            return true;

        } catch (Exception e) {
            log.error("Error marking user {} as NO_SHOW: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get count of NO_SHOW users for a shop in a date range
     * Used for analytics
     *
     * @param shopId Shop UUID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of NO_SHOW users
     */
    public long getNoShowCount(UUID shopId, Instant startDate, Instant endDate) {
        return queueLogRepository.countByShopIdAndStatusAndDateRange(
                shopId, QueueStatus.NO_SHOW, startDate, endDate
        );
    }
}
