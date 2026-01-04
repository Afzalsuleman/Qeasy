package com.smartqueue.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Analytics response DTO
 * Contains queue metrics and statistics for a shop
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Analytics and statistics for a shop's queue")
public class AnalyticsResponse {

    @Schema(description = "Shop ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID shopId;

    @Schema(description = "Shop name", example = "Joe's Coffee Shop")
    private String shopName;

    @Schema(description = "Current number of people in queue", example = "5")
    private Integer currentQueueSize;

    @Schema(description = "Total visitors in analyzed period", example = "150")
    private Long totalVisitors;

    @Schema(description = "Number of users served", example = "120")
    private Long servedCount;

    @Schema(description = "Number of no-shows", example = "10")
    private Long noShowCount;

    @Schema(description = "Average wait time in minutes", example = "15")
    private Integer averageWaitTimeMinutes;

    @Schema(description = "Estimated wait time for last person in current queue", example = "45")
    private Integer estimatedWaitTimeMinutes;

    @Schema(description = "Completion rate percentage", example = "80.0")
    private Double completionRate;

    @Schema(description = "No-show rate percentage", example = "6.67")
    private Double noShowRate;

    @Schema(description = "Maximum queue capacity", example = "50")
    private Integer maxQueueSize;

    @Schema(description = "Average service time per user in minutes", example = "10")
    private Integer avgServiceTimeMinutes;

    @Schema(description = "Number of days analyzed", example = "7")
    private Integer analyzedDays;

    @Schema(description = "Start date of analysis period")
    private Instant startDate;

    @Schema(description = "End date of analysis period")
    private Instant endDate;
}
