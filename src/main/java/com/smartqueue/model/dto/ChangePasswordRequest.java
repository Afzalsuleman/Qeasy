package com.smartqueue.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for changing user password
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to change user password")
public class ChangePasswordRequest {

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
    @Schema(
            description = "New password (minimum 8 characters, should contain uppercase, lowercase, digits, and special characters)",
            example = "SecurePass123!"
    )
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    @Schema(
            description = "Confirm new password (must match newPassword)",
            example = "SecurePass123!"
    )
    private String confirmPassword;
}
