package com.smartqueue.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating shop configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update shop configuration")
public class UpdateShopRequest {

    @Size(min = 2, max = 100, message = "Shop name must be between 2 and 100 characters")
    @Schema(description = "Shop name", example = "Joe's Coffee Shop")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Schema(description = "Shop description", example = "Best coffee in town!")
    private String description;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    @Schema(description = "Shop address", example = "123 Main St, New York, NY 10001")
    private String address;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    @Schema(description = "Shop image URL", example = "https://example.com/shop-image.jpg")
    private String imageUrl;

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    @Schema(description = "Shop phone number", example = "+1-555-0123")
    private String phone;

    @Min(value = 1, message = "Average service time must be at least 1 minute")
    @Schema(description = "Average service time per customer in minutes", example = "10")
    private Integer avgServiceTimeMinutes;

    @Min(value = 1, message = "Maximum queue size must be at least 1")
    @Schema(description = "Maximum number of customers in queue", example = "50")
    private Integer maxQueueSize;

    @Schema(description = "Whether shop is currently active", example = "true")
    private Boolean isActive;
}
