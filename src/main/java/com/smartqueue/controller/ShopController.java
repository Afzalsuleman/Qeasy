package com.smartqueue.controller;

import com.smartqueue.model.dto.CreateShopRequest;
import com.smartqueue.model.dto.ShopResponse;
import com.smartqueue.model.dto.UpdateShopRequest;
import com.smartqueue.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Shop management controller
 * Handles shop CRUD operations
 */
@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shop Management", description = "Endpoints for managing shops")
@SecurityRequirement(name = "bearerAuth")
public class ShopController {

    private final ShopService shopService;

    @Operation(
            summary = "Create a new shop",
            description = "Create a new shop for the authenticated user. " +
                    "Each user can only have one active shop at a time."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Shop created successfully",
                    content = @Content(schema = @Schema(implementation = ShopResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or user already has an active shop",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<ShopResponse> createShop(
            @Valid @RequestBody CreateShopRequest request,
            Authentication authentication
    ) {
        String ownerEmail = authentication.getName();
        log.info("Creating shop for owner: {}", ownerEmail);
        ShopResponse response = shopService.createShop(request, ownerEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get shop by ID",
            description = "Retrieve shop details by shop ID. Includes current queue size and estimated wait time."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Shop found",
                    content = @Content(schema = @Schema(implementation = ShopResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shop not found",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            )
    })
    @GetMapping("/{shopId}")
    public ResponseEntity<ShopResponse> getShopById(
            @Parameter(description = "Shop UUID", required = true)
            @PathVariable UUID shopId
    ) {
        log.info("Getting shop by ID: {}", shopId);
        ShopResponse response = shopService.getShopById(shopId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all active shops",
            description = "Retrieve a list of all active shops with current queue information"
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of shops retrieved successfully"
    )
    @GetMapping
    public ResponseEntity<List<ShopResponse>> getAllShops() {
        log.info("Getting all active shops");
        List<ShopResponse> response = shopService.getAllShops();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get my shop",
            description = "Get the authenticated user's active shop"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Shop found",
                    content = @Content(schema = @Schema(implementation = ShopResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No active shop found for user",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            )
    })
    @GetMapping("/my-shop")
    public ResponseEntity<ShopResponse> getMyShop(Authentication authentication) {
        String ownerEmail = authentication.getName();
        log.info("Getting shop for owner: {}", ownerEmail);
        ShopResponse response = shopService.getShopByOwner(ownerEmail);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update shop configuration",
            description = "Update shop details and configuration. Only the shop owner can update their shop."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Shop updated successfully",
                    content = @Content(schema = @Schema(implementation = ShopResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized to update this shop",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shop not found",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            )
    })
    @PutMapping("/{shopId}")
    public ResponseEntity<ShopResponse> updateShop(
            @Parameter(description = "Shop UUID", required = true)
            @PathVariable UUID shopId,
            @Valid @RequestBody UpdateShopRequest request,
            Authentication authentication
    ) {
        String ownerEmail = authentication.getName();
        log.info("Updating shop {} by owner: {}", shopId, ownerEmail);
        ShopResponse response = shopService.updateShop(shopId, request, ownerEmail);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete (deactivate) shop",
            description = "Soft delete a shop by setting it to inactive. Only the shop owner can delete their shop."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Shop deactivated successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized to delete this shop",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shop not found",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            )
    })
    @DeleteMapping("/{shopId}")
    public ResponseEntity<Void> deleteShop(
            @Parameter(description = "Shop UUID", required = true)
            @PathVariable UUID shopId,
            Authentication authentication
    ) {
        String ownerEmail = authentication.getName();
        log.info("Deleting shop {} by owner: {}", shopId, ownerEmail);
        shopService.deleteShop(shopId, ownerEmail);
        return ResponseEntity.noContent().build();
    }
}
