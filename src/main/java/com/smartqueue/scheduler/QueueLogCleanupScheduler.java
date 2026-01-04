package com.smartqueue.scheduler;

import com.smartqueue.repository.QueueLogRepository;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled task to clean up old queue logs
 * Runs daily at 2 AM to delete logs older than 30 days
 * Uses ShedLock for distributed locking in multi-instance deployments
 */
@Component
@Slf4j
public class QueueLogCleanupScheduler {

    private final QueueLogRepository queueLogRepository;

    @Autowired
    public QueueLogCleanupScheduler(QueueLogRepository queueLogRepository) {
        this.queueLogRepository = queueLogRepository;
    }

    /**
     * Clean up queue logs older than 30 days
     * Scheduled to run daily at 2:00 AM
     * ShedLock ensures only one instance runs this task (lock for 10 minutes, at least 5 minutes)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2:00 AM
    @SchedulerLock(
            name = "QueueLogCleanupScheduler_cleanupOldLogs",
            lockAtMostFor = "10m",
            lockAtLeastFor = "5m"
    )
    @Transactional
    public void cleanupOldLogs() {
        log.info("Starting queue log cleanup task");

        try {
            // Calculate cutoff date (30 days ago)
            Instant cutoffDate = Instant.now().minus(30, ChronoUnit.DAYS);

            log.info("Deleting queue logs older than: {}", cutoffDate);

            // Delete old logs
            int deletedCount = queueLogRepository.deleteByJoinedAtBefore(cutoffDate);

            log.info("Queue log cleanup completed. Deleted {} old records", deletedCount);

        } catch (Exception e) {
            log.error("Error during queue log cleanup: {}", e.getMessage(), e);
            throw e; // Re-throw to ensure proper transaction rollback
        }
    }

    /**
     * Alternative method to clean up specific statuses
     * Can be called manually or scheduled separately
     */
    @Transactional
    public int cleanupCompletedLogs(int daysOld) {
        log.info("Cleaning up completed queue logs older than {} days", daysOld);

        Instant cutoffDate = Instant.now().minus(daysOld, ChronoUnit.DAYS);

        try {
            int deletedCount = queueLogRepository.deleteByJoinedAtBefore(cutoffDate);
            log.info("Cleaned up {} completed queue logs", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("Error cleaning up completed logs: {}", e.getMessage(), e);
            throw e;
        }
    }
}
