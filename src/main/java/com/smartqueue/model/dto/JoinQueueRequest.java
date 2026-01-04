package com.smartqueue.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for joining a queue
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to join a shop's queue")
public class JoinQueueRequest {

    @NotNull(message = "Shop ID is required")
    @Schema(description = "Shop unique identifier", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    private UUID shopId;
}
