package com.smartqueue.model.entity;

import com.smartqueue.model.enums.QueueStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Queue Log entity for audit trail of queue operations
 * Tracks all queue events: JOINED, CALLED, SERVED, LEFT, NO_SHOW, CANCELLED
 * Retention: 30 days
 */
@Entity
@Table(name = "queue_logs", indexes = {
        @Index(name = "idx_queue_logs_shop_id", columnList = "shop_id,joined_at"),
        @Index(name = "idx_queue_logs_user_id", columnList = "user_id,joined_at"),
        @Index(name = "idx_queue_logs_status", columnList = "status"),
        @Index(name = "idx_queue_logs_joined_at", columnList = "joined_at"),
        @Index(name = "idx_queue_logs_shop_status_time", columnList = "shop_id,status,joined_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Shop is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false, foreignKey = @ForeignKey(name = "fk_queue_logs_shop"))
    private Shop shop;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_queue_logs_user"))
    private User user;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueStatus status;

    @Column
    private Integer position;

    @Column(name = "estimated_wait_minutes")
    private Integer estimatedWaitMinutes;

    @NotNull
    @Column(name = "joined_at", nullable = false)
    @Builder.Default
    private Instant joinedAt = Instant.now();

    @Column(name = "called_at")
    private Instant calledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueueLog)) return false;
        QueueLog queueLog = (QueueLog) o;
        return id != null && id.equals(queueLog.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "QueueLog{" +
                "id=" + id +
                ", status=" + status +
                ", position=" + position +
                ", joinedAt=" + joinedAt +
                '}';
    }
}
