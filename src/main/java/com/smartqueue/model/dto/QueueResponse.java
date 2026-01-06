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
 * Response DTO for queue operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Queue operation response")
public class QueueResponse {

    @Schema(description = "Shop unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID shopId;

    @Schema(description = "Shop name", example = "Joe's Coffee Shop")
    private String shopName;

    @Schema(description = "User's unique identifier", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID userId;

    @Schema(description = "User's name", example = "John Doe")
    private String userName;

    @Schema(description = "User's email", example = "user@example.com")
    private String userEmail;

    @Schema(description = "Position in queue (1-indexed)", example = "5")
    private Integer position;

    @Schema(description = "Queue status", example = "JOINED")
    private String status;

    @Schema(description = "Total number of people in queue", example = "15")
    private Integer totalInQueue;

    @Schema(description = "People ahead in queue", example = "4")
    private Integer peopleAhead;

    @Schema(description = "Estimated wait time in minutes", example = "40")
    private Integer estimatedWaitTimeMinutes;

    @Schema(description = "Timestamp when user joined queue")
    private Instant joinedAt;

    @Schema(description = "Timestamp when user was called")
    private Instant calledAt;

    @Schema(description = "Timestamp when user was served/completed")
    private Instant servedAt;

    @Schema(description = "Success message", example = "You have successfully joined the queue")
    private String message;
}
