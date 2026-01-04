package com.smartqueue.model.entity;

import com.smartqueue.model.enums.EmailStatus;
import com.smartqueue.model.enums.EmailType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

/**
 * Failed Email entity for tracking email delivery failures
 * Enables manual retry with exponential backoff
 * Part of the email service resilience strategy
 */
@Entity
@Table(name = "failed_emails", indexes = {
        @Index(name = "idx_failed_emails_status", columnList = "status,retry_after"),
        @Index(name = "idx_failed_emails_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Recipient is required")
    @Column(nullable = false, length = 255)
    private String recipient;

    @NotBlank(message = "Subject is required")
    @Column(nullable = false, length = 500)
    private String subject;

    @NotBlank(message = "Body is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "email_body", columnDefinition = "TEXT")
    private String emailBody;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @NotNull
    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 1;

    @NotNull
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "retried_at")
    private Instant retriedAt;

    @Column(name = "retry_after")
    private Instant retryAfter;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EmailStatus status = EmailStatus.PENDING;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false, length = 20)
    @Builder.Default
    private EmailType emailType = EmailType.NOTIFICATION;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FailedEmail)) return false;
        FailedEmail that = (FailedEmail) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "FailedEmail{" +
                "id=" + id +
                ", recipient='" + recipient + '\'' +
                ", status=" + status +
                ", attemptCount=" + attemptCount +
                '}';
    }
}
