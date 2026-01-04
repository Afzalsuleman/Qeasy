package com.smartqueue.repository;

import com.smartqueue.model.entity.FailedEmail;
import com.smartqueue.model.enums.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for FailedEmail entity
 * Provides data access methods for failed email retry mechanism
 */
@Repository
public interface FailedEmailRepository extends JpaRepository<FailedEmail, Long> {

    /**
     * Find failed emails ready for retry
     * Used by manual retry scheduler (runs every hour)
     *
     * @param status Email status (usually PENDING)
     * @param now Current timestamp
     * @return List of failed emails ready for retry
     */
    @Query("SELECT fe FROM FailedEmail fe " +
           "WHERE fe.status = :status " +
           "AND (fe.retryAfter IS NULL OR fe.retryAfter <= :now) " +
           "ORDER BY fe.createdAt ASC")
    List<FailedEmail> findByStatusAndRetryAfterBefore(
            @Param("status") EmailStatus status,
            @Param("now") Instant now
    );

    /**
     * Find failed emails by status (paginated)
     * Used for admin dashboard and monitoring
     *
     * @param status Email status
     * @param pageable Pagination parameters
     * @return Page of failed emails
     */
    Page<FailedEmail> findByStatusOrderByCreatedAtDesc(EmailStatus status, Pageable pageable);

    /**
     * Count failed emails by status
     * Used for monitoring dashboard
     *
     * @param status Email status
     * @return Count of emails with this status
     */
    long countByStatus(EmailStatus status);

    /**
     * Find failed emails by status list (for retry scheduler)
     * Used to find emails in PENDING or RETRYING status
     *
     * @param statuses List of email statuses
     * @return List of failed emails ordered by failed date
     */
    List<FailedEmail> findByStatusInOrderByFailedAtAsc(List<EmailStatus> statuses);

    /**
     * Find emails that have exceeded max retry attempts
     * Used to identify permanently failed emails
     *
     * @param maxAttempts Maximum allowed attempts (e.g., 10)
     * @return List of emails that failed after max attempts
     */
    @Query("SELECT fe FROM FailedEmail fe " +
           "WHERE fe.status = 'PENDING' " +
           "AND fe.attemptCount >= :maxAttempts " +
           "ORDER BY fe.createdAt ASC")
    List<FailedEmail> findEmailsExceedingMaxAttempts(@Param("maxAttempts") int maxAttempts);

    /**
     * Find oldest failed emails for cleanup
     * Used for cleanup scheduler (optional - remove very old failed emails)
     *
     * @param cutoffDate Delete emails older than this date
     * @param status Only delete emails with this status (e.g., SUCCESS or FAILED)
     * @return List of failed emails to delete
     */
    @Query("SELECT fe FROM FailedEmail fe " +
           "WHERE fe.createdAt < :cutoffDate " +
           "AND fe.status = :status")
    List<FailedEmail> findOldFailedEmails(
            @Param("cutoffDate") Instant cutoffDate,
            @Param("status") EmailStatus status
    );
}
