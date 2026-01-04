package com.smartqueue.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Shop entity representing businesses using the queue management system
 * Each shop has one owner and configurable queue settings
 */
@Entity
@Table(name = "shops", indexes = {
        @Index(name = "idx_shops_owner_id", columnList = "owner_id"),
        @Index(name = "idx_shops_is_active", columnList = "is_active"),
        @Index(name = "idx_shops_location", columnList = "latitude,longitude")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_shops_owner_active", columnNames = {"owner_id", "is_active"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shop extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Owner is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_shops_owner"))
    private User owner;

    @NotBlank(message = "Shop name is required")
    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 500)
    private String imageUrl;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Min(value = 1, message = "Average service time must be at least 1 minute")
    @Column(name = "avg_service_time_minutes", nullable = false)
    @Builder.Default
    private Integer avgServiceTimeMinutes = 15;

    @Min(value = 1, message = "Max queue size must be at least 1")
    @Max(value = 1000, message = "Max queue size cannot exceed 1000")
    @Column(name = "max_queue_size", nullable = false)
    @Builder.Default
    private Integer maxQueueSize = 50;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Shop)) return false;
        Shop shop = (Shop) o;
        return id != null && id.equals(shop.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Shop{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
