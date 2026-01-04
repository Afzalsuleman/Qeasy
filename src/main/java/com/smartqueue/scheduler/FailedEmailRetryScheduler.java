package com.smartqueue.scheduler;

import com.smartqueue.model.entity.FailedEmail;
import com.smartqueue.model.enums.EmailStatus;
import com.smartqueue.model.enums.EmailType;
import com.smartqueue.repository.FailedEmailRepository;
import com.smartqueue.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled task to retry failed email deliveries
 * Runs every 5 minutes to process pending and retrying emails
 * Uses exponential backoff and max retry limit (5 attempts)
 * Uses ShedLock for distributed locking in multi-instance deployments
 */
@Component
@Slf4j
public class FailedEmailRetryScheduler {

    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 50; // Process 50 emails at a time

    private final FailedEmailRepository failedEmailRepository;
    private final EmailService emailService;

    @Autowired
    public FailedEmailRetryScheduler(
            FailedEmailRepository failedEmailRepository,
            EmailService emailService) {
        this.failedEmailRepository = failedEmailRepository;
        this.emailService = emailService;
    }

    /**
     * Retry failed emails with exponential backoff
     * Scheduled to run every 5 minutes
     * ShedLock ensures only one instance runs this task
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes (300000ms)
    @SchedulerLock(
            name = "FailedEmailRetryScheduler_retryFailedEmails",
            lockAtMostFor = "4m",
            lockAtLeastFor = "1m"
    )
    @Transactional
    public void retryFailedEmails() {
        log.debug("Starting failed email retry task");

        try {
            // Find emails eligible for retry (PENDING or RETRYING status)
            List<FailedEmail> failedEmails = failedEmailRepository
                    .findByStatusInOrderByFailedAtAsc(
                            List.of(EmailStatus.PENDING, EmailStatus.RETRYING)
                    )
                    .stream()
                    .limit(BATCH_SIZE)
                    .toList();

            if (failedEmails.isEmpty()) {
                log.debug("No failed emails to retry");
                return;
            }

            log.info("Found {} failed emails to retry", failedEmails.size());

            int successCount = 0;
            int failureCount = 0;
            int permanentFailureCount = 0;

            int skippedOtpCount = 0;

            for (FailedEmail failedEmail : failedEmails) {
                // Skip OTP emails - they should not be retried automatically
                // OTP expires in 5 minutes, so retrying is pointless and confusing
                if (failedEmail.getEmailType() == EmailType.OTP) {
                    log.info("Skipping OTP email {} - OTP emails are not auto-retried (user must request new OTP)",
                            failedEmail.getId());
                    skippedOtpCount++;
                    continue;
                }

                // Check if email is ready for retry (exponential backoff)
                if (!isReadyForRetry(failedEmail)) {
                    log.debug("Email {} not yet ready for retry", failedEmail.getId());
                    continue;
                }

                // Check if max retry attempts reached
                if (failedEmail.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
                    log.warn("Email {} reached max retry attempts ({})",
                            failedEmail.getId(), MAX_RETRY_ATTEMPTS);

                    failedEmail.setStatus(EmailStatus.FAILED);
                    failedEmail.setFailureReason(
                            "Max retry attempts reached: " + failedEmail.getFailureReason()
                    );
                    failedEmailRepository.save(failedEmail);
                    permanentFailureCount++;
                    continue;
                }

                // Attempt to resend email
                try {
                    log.info("Retrying email {} (attempt {}/{})",
                            failedEmail.getId(),
                            failedEmail.getRetryCount() + 1,
                            MAX_RETRY_ATTEMPTS);

                    // Resend the email
                    emailService.sendOtpEmail(
                            failedEmail.getRecipient(),
                            extractOtpFromBody(failedEmail.getEmailBody()),
                            extractNameFromBody(failedEmail.getEmailBody())
                    );

                    // Mark as sent
                    failedEmail.setStatus(EmailStatus.SENT);
                    failedEmail.setRetriedAt(Instant.now());
                    failedEmailRepository.save(failedEmail);

                    successCount++;
                    log.info("Successfully retried email {}", failedEmail.getId());

                } catch (Exception e) {
                    // Retry failed, increment retry count
                    failedEmail.setRetryCount(failedEmail.getRetryCount() + 1);
                    failedEmail.setStatus(EmailStatus.RETRYING);
                    failedEmail.setFailureReason(
                            failedEmail.getFailureReason() + " | Retry " +
                            failedEmail.getRetryCount() + " failed: " + e.getMessage()
                    );
                    failedEmail.setRetriedAt(Instant.now());
                    failedEmailRepository.save(failedEmail);

                    failureCount++;
                    log.error("Failed to retry email {}: {}",
                            failedEmail.getId(), e.getMessage());
                }
            }

            log.info("Email retry task completed. Success: {}, Failed: {}, Permanent failures: {}, Skipped OTP emails: {}",
                    successCount, failureCount, permanentFailureCount, skippedOtpCount);

        } catch (Exception e) {
            log.error("Error during failed email retry task: {}", e.getMessage(), e);
            throw e; // Re-throw to ensure proper transaction rollback
        }
    }

    /**
     * Check if email is ready for retry using exponential backoff
     * Backoff delay: 2^retryCount minutes
     * - 1st retry: 2 minutes
     * - 2nd retry: 4 minutes
     * - 3rd retry: 8 minutes
     * - 4th retry: 16 minutes
     * - 5th retry: 32 minutes
     *
     * @param failedEmail Failed email record
     * @return true if ready for retry
     */
    private boolean isReadyForRetry(FailedEmail failedEmail) {
        Instant lastAttempt = failedEmail.getRetriedAt() != null
                ? failedEmail.getRetriedAt()
                : failedEmail.getFailedAt();

        // Calculate backoff delay in minutes: 2^retryCount
        long backoffMinutes = (long) Math.pow(2, failedEmail.getRetryCount());

        Instant nextRetryTime = lastAttempt.plus(backoffMinutes, ChronoUnit.MINUTES);

        return Instant.now().isAfter(nextRetryTime);
    }

    /**
     * Extract OTP from email body
     * Email body format: "OTP: 123456"
     */
    private String extractOtpFromBody(String emailBody) {
        if (emailBody == null || emailBody.isEmpty()) {
            return "";
        }

        // Extract OTP from format "OTP: 123456"
        if (emailBody.contains("OTP: ")) {
            String[] parts = emailBody.split("OTP: ");
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }

        return "";
    }

    /**
     * Extract recipient name from email body
     * Email body format: "OTP: 123456 for user: John Doe"
     */
    private String extractNameFromBody(String emailBody) {
        if (emailBody == null || emailBody.isEmpty()) {
            return "User";
        }

        // Extract name from format "... for user: John Doe"
        if (emailBody.contains("for user: ")) {
            String[] parts = emailBody.split("for user: ");
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }

        return "User";
    }

    /**
     * Manual method to process specific failed email
     * Can be called from admin API
     *
     * @param emailId Failed email ID
     * @return true if retry was successful
     */
    @Transactional
    public boolean retrySpecificEmail(Long emailId) {
        log.info("Manual retry requested for email {}", emailId);

        FailedEmail failedEmail = failedEmailRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Email not found: " + emailId));

        if (failedEmail.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
            log.warn("Cannot retry email {} - max attempts reached", emailId);
            return false;
        }

        try {
            emailService.sendOtpEmail(
                    failedEmail.getRecipient(),
                    extractOtpFromBody(failedEmail.getEmailBody()),
                    extractNameFromBody(failedEmail.getEmailBody())
            );

            failedEmail.setStatus(EmailStatus.SENT);
            failedEmail.setRetriedAt(Instant.now());
            failedEmailRepository.save(failedEmail);

            log.info("Manual retry successful for email {}", emailId);
            return true;

        } catch (Exception e) {
            failedEmail.setRetryCount(failedEmail.getRetryCount() + 1);
            failedEmail.setStatus(EmailStatus.RETRYING);
            failedEmail.setRetriedAt(Instant.now());
            failedEmailRepository.save(failedEmail);

            log.error("Manual retry failed for email {}: {}", emailId, e.getMessage());
            return false;
        }
    }
}
