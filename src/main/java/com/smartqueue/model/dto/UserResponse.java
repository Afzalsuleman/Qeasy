package com.smartqueue.model.dto;

import com.smartqueue.model.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User information response")
public class UserResponse {

    @Schema(description = "User unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "User email address", example = "user@example.com")
    private String email;

    @Schema(description = "User full name", example = "John Doe")
    private String name;

    @Schema(description = "User phone number", example = "+1-555-0123")
    private String phone;

    @Schema(description = "User role in the system", example = "SHOP_OWNER")
    private UserRole role;

    @Schema(description = "Whether user has set their password", example = "true")
    private Boolean passwordSet;

    @Schema(description = "Timestamp when user was created")
    private Instant createdAt;

    @Schema(description = "Timestamp when user was last updated")
    private Instant updatedAt;
}
