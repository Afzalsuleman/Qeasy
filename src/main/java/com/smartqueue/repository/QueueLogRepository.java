package com.smartqueue.repository;

import com.smartqueue.model.entity.QueueLog;
import com.smartqueue.model.enums.QueueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for QueueLog entity
 * Provides data access methods for queue audit trail and analytics
 * Retention: 30 days (cleaned by scheduler)
 */
@Repository
public interface QueueLogRepository extends JpaRepository<QueueLog, Long> {

    /**
     * Find queue logs by shop ID and status
     * Used for analytics and queue state queries
     *
     * @param shopId Shop ID
     * @param status Queue status
     * @return List of matching queue logs
     */
    List<QueueLog> findByShopIdAndStatus(UUID shopId, QueueStatus status);

    /**
     * Find recent queue logs for a shop (paginated)
     * Used for shop analytics and history
     *
     * @param shopId Shop ID
     * @param pageable Pagination parameters
     * @return Page of queue logs ordered by joined time descending
     */
    Page<QueueLog> findByShopIdOrderByJoinedAtDesc(UUID shopId, Pageable pageable);

    /**
     * Find recent queue logs for a user (paginated)
     * Used for user history
     *
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Page of queue logs ordered by joined time descending
     */
    Page<QueueLog> findByUserIdOrderByJoinedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find latest queue log for user at specific shop
     * Used to check current queue state
     *
     * @param userId User ID
     * @param shopId Shop ID
     * @return Optional containing latest QueueLog
     */
    @Query("SELECT ql FROM QueueLog ql " +
           "WHERE ql.user.id = :userId AND ql.shop.id = :shopId " +
           "ORDER BY ql.joinedAt DESC LIMIT 1")
    Optional<QueueLog> findLatestByUserIdAndShopId(
            @Param("userId") UUID userId,
            @Param("shopId") UUID shopId
    );

    /**
     * Find queue log by user, shop, and status
     * Used for updating log status (e.g., marking as CALLED, SERVED)
     *
     * @param userId User ID
     * @param shopId Shop ID
     * @param status Current status
     * @return Optional containing QueueLog
     */
    Optional<QueueLog> findFirstByUserIdAndShopIdAndStatusOrderByJoinedAtDesc(
            UUID userId, UUID shopId, QueueStatus status
    );

    /**
     * Find queue log by shop ID, user ID, and status
     * Alias method for QueueService
     *
     * @param shopId Shop ID
     * @param userId User ID
     * @param status Queue status
     * @return Optional containing QueueLog
     */
    default Optional<QueueLog> findByShopIdAndUserIdAndStatus(
            UUID shopId, UUID userId, QueueStatus status) {
        return findFirstByUserIdAndShopIdAndStatusOrderByJoinedAtDesc(userId, shopId, status);
    }

    /**
     * Delete queue logs older than cutoff date
     * Used by cleanup scheduler (30-day retention)
     *
     * @param cutoffDate Delete logs older than this date
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM QueueLog ql WHERE ql.joinedAt < :cutoffDate")
    int deleteByJoinedAtBefore(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Count queue logs by shop and date range
     * Used for analytics
     *
     * @param shopId Shop ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of logs
     */
    @Query("SELECT COUNT(ql) FROM QueueLog ql " +
           "WHERE ql.shop.id = :shopId " +
           "AND ql.joinedAt BETWEEN :startDate AND :endDate")
    long countByShopIdAndDateRange(
            @Param("shopId") UUID shopId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Get average wait time for a shop
     * Used for analytics and wait time estimation
     *
     * @param shopId Shop ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Average wait time in minutes
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(MINUTE, ql.joinedAt, ql.calledAt)) " +
           "FROM QueueLog ql " +
           "WHERE ql.shop.id = :shopId " +
           "AND ql.status = 'CALLED' " +
           "AND ql.joinedAt BETWEEN :startDate AND :endDate")
    Double getAverageWaitTime(
            @Param("shopId") UUID shopId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Find queue logs by status and called before a certain time
     * Used by stale queue cleanup scheduler to detect NO_SHOW users
     *
     * @param status Queue status (usually CALLED)
     * @param calledBefore Cutoff time for called timestamp
     * @return List of queue logs
     */
    List<QueueLog> findByStatusAndCalledAtBefore(QueueStatus status, Instant calledBefore);

    /**
     * Count queue logs by shop, status, and date range
     * Used for NO_SHOW analytics
     *
     * @param shopId Shop ID
     * @param status Queue status
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of logs
     */
    @Query("SELECT COUNT(ql) FROM QueueLog ql " +
           "WHERE ql.shop.id = :shopId " +
           "AND ql.status = :status " +
           "AND ql.joinedAt BETWEEN :startDate AND :endDate")
    long countByShopIdAndStatusAndDateRange(
            @Param("shopId") UUID shopId,
            @Param("status") QueueStatus status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}
