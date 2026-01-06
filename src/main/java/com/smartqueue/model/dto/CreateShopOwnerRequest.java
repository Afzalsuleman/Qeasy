package com.smartqueue.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for admin to create a new shop owner
 * Admin invites shop owner via email with temporary credentials
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new shop owner (admin only)")
public class CreateShopOwnerRequest {

    @NotBlank(message = "Shop owner email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Shop owner's email address", example = "owner@example.com")
    private String email;

    @Size(min = 2, max = 100, message = "Shop owner name must be between 2 and 100 characters")
    @Schema(description = "Shop owner's full name", example = "John Doe")
    private String name;

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    @Schema(description = "Shop owner's phone number", example = "+1-555-0123")
    private String phone;
}
