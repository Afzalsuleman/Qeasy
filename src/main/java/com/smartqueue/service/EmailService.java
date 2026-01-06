package com.smartqueue.service;

import com.smartqueue.exception.EmailLimitExceededException;
import com.smartqueue.exception.EmailServiceException;
import com.smartqueue.model.entity.FailedEmail;
import com.smartqueue.model.entity.User;
import com.smartqueue.model.enums.EmailStatus;
import com.smartqueue.model.enums.EmailType;
import com.smartqueue.repository.FailedEmailRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Email service with Gmail SMTP integration and circuit breaker
 * Handles OTP emails with daily rate limiting and retry mechanism
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final RedisTemplate<String, String> redisTemplate;
    private final FailedEmailRepository failedEmailRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.daily-limit:100}")
    private int dailyEmailLimit;

    @Value("${app.email.sender-name:Smart Queue}")
    private String senderName;

    private static final String REDIS_EMAIL_COUNTER_KEY = "email:daily:count";
    private static final String REDIS_EMAIL_LOCK_PREFIX = "email:lock:";

    /**
     * Send OTP email with circuit breaker protection
     *
     * @param toEmail Recipient email address
     * @param otp 6-digit OTP code
     * @param userName Recipient name
     * @throws EmailServiceException if email sending fails
     * @throws EmailLimitExceededException if daily limit exceeded
     */
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendOtpEmailFallback")
    @Async
    public void sendOtpEmail(String toEmail, String otp, String userName) {
        log.info("Attempting to send OTP email to: {}", toEmail);

        // Check daily limit
        checkDailyLimit();

        // Check if email is locked (too many recent attempts)
        String lockKey = REDIS_EMAIL_LOCK_PREFIX + toEmail;
        Boolean isLocked = redisTemplate.hasKey(lockKey);
        if (Boolean.TRUE.equals(isLocked)) {
            log.warn("Email {} is temporarily locked due to too many attempts", toEmail);
            throw new EmailLimitExceededException("Too many email requests. Please try again later.");
        }

        try {
            // Create email message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, senderName);
            helper.setTo(toEmail);
            helper.setSubject("Your Smart Queue OTP Code");

            // Create Thymeleaf context
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("otp", otp);
            context.setVariable("validityMinutes", 5);

            // Process template
            String htmlContent = templateEngine.process("otp-email", context);
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);

            // Increment daily counter
            incrementDailyCounter();

            log.info("OTP email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            saveFailedEmail(toEmail, otp, userName, e.getMessage());
            throw new EmailServiceException("Failed to send OTP email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending OTP email to {}: {}", toEmail, e.getMessage());
            saveFailedEmail(toEmail, otp, userName, e.getMessage());
            throw new EmailServiceException("Unexpected error sending OTP email", e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    private void sendOtpEmailFallback(String toEmail, String otp, String userName, Exception e) {
        log.error("Circuit breaker OPEN - Email service unavailable. Saving failed email for retry.", e);
        saveFailedEmail(toEmail, otp, userName, "Circuit breaker open: " + e.getMessage());
        throw new EmailServiceException("Email service is temporarily unavailable. Your request has been queued for retry.", e);
    }

    /**
     * Check if daily email limit has been exceeded
     */
    private void checkDailyLimit() {
        String counterValue = redisTemplate.opsForValue().get(REDIS_EMAIL_COUNTER_KEY);
        int currentCount = counterValue != null ? Integer.parseInt(counterValue) : 0;

        if (currentCount >= dailyEmailLimit) {
            log.warn("Daily email limit exceeded: {} / {}", currentCount, dailyEmailLimit);
            throw new EmailLimitExceededException(
                    String.format("Daily email limit exceeded (%d/%d). Please try again tomorrow.",
                    currentCount, dailyEmailLimit)
            );
        }
    }

    /**
     * Increment daily email counter with 24-hour expiry
     */
    private void incrementDailyCounter() {
        Long newCount = redisTemplate.opsForValue().increment(REDIS_EMAIL_COUNTER_KEY);
        if (newCount != null && newCount == 1) {
            // Set expiry on first increment of the day
            redisTemplate.expire(REDIS_EMAIL_COUNTER_KEY, 24, TimeUnit.HOURS);
        }
    }

    /**
     * Save failed email for retry mechanism
     */
    private void saveFailedEmail(String toEmail, String otp, String userName, String errorMessage) {
        try {
            FailedEmail failedEmail = FailedEmail.builder()
                    .recipient(toEmail)
                    .subject("Your Smart Queue OTP Code")
                    .body(String.format("OTP: %s for user: %s", otp, userName))
                    .emailBody(String.format("OTP: %s", otp))
                    .emailType(EmailType.OTP)  // Mark as OTP email - will not be auto-retried
                    .status(EmailStatus.PENDING)
                    .attemptCount(1)
                    .retryCount(0)
                    .failedAt(Instant.now())
                    .retryAfter(Instant.now().plus(5, ChronoUnit.MINUTES))
                    .failureReason(errorMessage)
                    .build();

            failedEmailRepository.save(failedEmail);
            log.info("Failed OTP email saved (will NOT be auto-retried): {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to save failed email record: {}", e.getMessage());
        }
    }

    /**
     * Get current daily email count
     */
    public int getDailyEmailCount() {
        String counterValue = redisTemplate.opsForValue().get(REDIS_EMAIL_COUNTER_KEY);
        return counterValue != null ? Integer.parseInt(counterValue) : 0;
    }

    /**
     * Lock an email address temporarily (5 minutes)
     */
    public void lockEmail(String email) {
        String lockKey = REDIS_EMAIL_LOCK_PREFIX + email;
        redisTemplate.opsForValue().set(lockKey, "locked", 5, TimeUnit.MINUTES);
        log.info("Email {} locked for 5 minutes", email);
    }

    /**
     * Send shop owner invitation email with login credentials
     * Invitation tells shop owner to set their password and login
     *
     * @param shopOwner Shop owner user entity
     * @throws EmailServiceException if email sending fails
     */
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendShopOwnerInvitationFallback")
    @Async
    public void sendShopOwnerInvitation(User shopOwner) {
        log.info("Attempting to send shop owner invitation to: {}", shopOwner.getEmail());

        // Check daily limit
        checkDailyLimit();

        try {
            // Create email message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, senderName);
            helper.setTo(shopOwner.getEmail());
            helper.setSubject("Smart Queue Shop Owner Account Created - Set Your Password");

            // Create Thymeleaf context
            Context context = new Context();
            context.setVariable("ownerName", shopOwner.getName() != null ? shopOwner.getName() : "Shop Owner");
            context.setVariable("ownerEmail", shopOwner.getEmail());
            context.setVariable("appName", "Smart Queue");
            context.setVariable("loginUrl", "http://localhost:3000/login"); // TODO: Make configurable

            // Process template
            String htmlContent = templateEngine.process("shop-owner-invitation", context);
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);

            // Increment daily counter
            incrementDailyCounter();

            log.info("Shop owner invitation email sent successfully to: {}", shopOwner.getEmail());

        } catch (MessagingException e) {
            log.error("Failed to send shop owner invitation to {}: {}", shopOwner.getEmail(), e.getMessage());
            saveShopOwnerInvitationFailedEmail(shopOwner, e.getMessage());
            throw new EmailServiceException("Failed to send shop owner invitation email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending shop owner invitation to {}: {}", shopOwner.getEmail(), e.getMessage());
            saveShopOwnerInvitationFailedEmail(shopOwner, e.getMessage());
            throw new EmailServiceException("Unexpected error sending shop owner invitation email", e);
        }
    }

    /**
     * Fallback method for shop owner invitation when circuit breaker is open
     */
    private void sendShopOwnerInvitationFallback(User shopOwner, Exception e) {
        log.error("Circuit breaker OPEN - Email service unavailable. Saving shop owner invitation for retry.", e);
        saveShopOwnerInvitationFailedEmail(shopOwner, "Circuit breaker open: " + e.getMessage());
        throw new EmailServiceException("Email service is temporarily unavailable. Invitation will be resent.", e);
    }

    /**
     * Send shop owner invitation email with temporary password
     *
     * @param shopOwner Shop owner user entity
     * @param temporaryPassword Temporary password to be sent in email
     * @throws EmailServiceException if email sending fails
     */
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendShopOwnerInvitationWithPasswordFallback")
    @Async
    public void sendShopOwnerInvitationWithPassword(User shopOwner, String temporaryPassword) {
        log.info("Attempting to send shop owner invitation with password to: {}", shopOwner.getEmail());

        // Check daily limit
        checkDailyLimit();

        try {
            // Create email message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, senderName);
            helper.setTo(shopOwner.getEmail());
            helper.setSubject("Smart Queue Shop Owner Account Created - Your Temporary Password");

            // Create Thymeleaf context
            Context context = new Context();
            context.setVariable("ownerName", shopOwner.getName() != null ? shopOwner.getName() : "Shop Owner");
            context.setVariable("ownerEmail", shopOwner.getEmail());
            context.setVariable("temporaryPassword", temporaryPassword);
            context.setVariable("appName", "Smart Queue");
            context.setVariable("loginUrl", "http://localhost:3000/login"); // TODO: Make configurable

            // Process template
            String htmlContent = templateEngine.process("shop-owner-invitation-password", context);
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);

            // Increment daily counter
            incrementDailyCounter();

            log.info("Shop owner invitation email with password sent successfully to: {}", shopOwner.getEmail());

        } catch (MessagingException e) {
            log.error("Failed to send shop owner invitation with password to {}: {}", shopOwner.getEmail(), e.getMessage());
            saveShopOwnerInvitationWithPasswordFailedEmail(shopOwner, temporaryPassword, e.getMessage());
            throw new EmailServiceException("Failed to send shop owner invitation email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending shop owner invitation with password to {}: {}", shopOwner.getEmail(), e.getMessage());
            saveShopOwnerInvitationWithPasswordFailedEmail(shopOwner, temporaryPassword, e.getMessage());
            throw new EmailServiceException("Unexpected error sending shop owner invitation email", e);
        }
    }

    /**
     * Fallback method for shop owner invitation with password when circuit breaker is open
     */
    private void sendShopOwnerInvitationWithPasswordFallback(User shopOwner, String temporaryPassword, Exception e) {
        log.error("Circuit breaker OPEN - Email service unavailable. Saving shop owner invitation for retry.", e);
        saveShopOwnerInvitationWithPasswordFailedEmail(shopOwner, temporaryPassword, "Circuit breaker open: " + e.getMessage());
        throw new EmailServiceException("Email service is temporarily unavailable. Invitation will be resent.", e);
    }

    /**
     * Save failed shop owner invitation with password for retry
     */
    private void saveShopOwnerInvitationWithPasswordFailedEmail(User shopOwner, String temporaryPassword, String errorMessage) {
        try {
            FailedEmail failedEmail = FailedEmail.builder()
                    .recipient(shopOwner.getEmail())
                    .subject("Smart Queue Shop Owner Account Created - Your Temporary Password")
                    .body("Shop owner invitation for: " + shopOwner.getEmail())
                    .emailBody("Temporary password: " + temporaryPassword)
                    .emailType(EmailType.NOTIFICATION)
                    .status(EmailStatus.PENDING)
                    .attemptCount(1)
                    .retryCount(0)
                    .failedAt(Instant.now())
                    .retryAfter(Instant.now().plus(2, ChronoUnit.MINUTES))
                    .failureReason(errorMessage)
                    .build();

            failedEmailRepository.save(failedEmail);
            log.info("Failed shop owner invitation email saved for retry: {}", shopOwner.getEmail());
        } catch (Exception e) {
            log.error("Failed to save shop owner invitation failed email record: {}", e.getMessage());
        }
    }

    /**
     * Save failed shop owner invitation for retry
     */
    private void saveShopOwnerInvitationFailedEmail(User shopOwner, String errorMessage) {
        try {
            FailedEmail failedEmail = FailedEmail.builder()
                    .recipient(shopOwner.getEmail())
                    .subject("Smart Queue Shop Owner Account Created - Set Your Password")
                    .body("Shop owner invitation for: " + shopOwner.getEmail())
                    .emailBody("Shop owner account created. Please set password and login.")
                    .emailType(EmailType.NOTIFICATION)
                    .status(EmailStatus.PENDING)
                    .attemptCount(1)
                    .retryCount(0)
                    .failedAt(Instant.now())
                    .retryAfter(Instant.now().plus(2, ChronoUnit.MINUTES))
                    .failureReason(errorMessage)
                    .build();

            failedEmailRepository.save(failedEmail);
            log.info("Failed shop owner invitation email saved for retry: {}", shopOwner.getEmail());
        } catch (Exception e) {
            log.error("Failed to save shop owner invitation failed email record: {}", e.getMessage());
        }
    }
}
