package com.smartqueue.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for shop information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Shop information response")
public class ShopResponse {

    @Schema(description = "Shop unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Shop owner's user ID", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID ownerId;

    @Schema(description = "Shop name", example = "Joe's Coffee Shop")
    private String name;

    @Schema(description = "Shop description", example = "Best coffee in town!")
    private String description;

    @Schema(description = "Shop address", example = "123 Main St, New York, NY 10001")
    private String address;

    @Schema(description = "Shop image URL", example = "https://example.com/shop-image.jpg")
    private String imageUrl;

    @Schema(description = "Shop phone number", example = "+1-555-0123")
    private String phone;

    @Schema(description = "Average service time per customer in minutes", example = "10")
    private Integer avgServiceTimeMinutes;

    @Schema(description = "Maximum number of customers in queue", example = "50")
    private Integer maxQueueSize;

    @Schema(description = "Whether shop is currently active", example = "true")
    private Boolean isActive;

    @Schema(description = "Current number of customers in queue", example = "5")
    private Integer currentQueueSize;

    @Schema(description = "Estimated wait time in minutes", example = "50")
    private Integer estimatedWaitTimeMinutes;

    @Schema(description = "Timestamp when shop was created")
    private Instant createdAt;

    @Schema(description = "Timestamp when shop was last updated")
    private Instant updatedAt;
}
