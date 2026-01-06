package com.smartqueue.controller;

import com.smartqueue.model.dto.CreateShopOwnerRequest;
import com.smartqueue.model.dto.UserResponse;
import com.smartqueue.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin controller for admin-only operations
 * Requires ADMIN role for all endpoints
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Admin-only endpoints for managing shop owners and system analytics")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @Operation(
            summary = "Create a new shop owner",
            description = "Admin endpoint to create a new shop owner and send invitation email with credentials. " +
                    "Shop owner will receive email with login link and must set password before using the system."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Shop owner created successfully and invitation email sent",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or user already exists",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - user is not admin",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            )
    })
    @PostMapping("/shop-owners")
    public ResponseEntity<UserResponse> createShopOwner(
            @Valid @RequestBody CreateShopOwnerRequest request,
            Authentication authentication
    ) {
        log.info("Admin creating shop owner with email: {}", request.getEmail());
        String adminEmail = authentication.getName();
        UserResponse response = adminService.createShopOwner(request, adminEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get all shop owners",
            description = "Retrieve list of all shop owners in the system with their details"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of shop owners retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - user is not admin",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            )
    })
    @GetMapping("/shop-owners")
    public ResponseEntity<List<UserResponse>> getAllShopOwners() {
        log.info("Fetching all shop owners");
        List<UserResponse> shopOwners = adminService.getAllShopOwners();
        return ResponseEntity.ok(shopOwners);
    }

    @Operation(
            summary = "Get admin dashboard",
            description = "Retrieve system-wide dashboard data including total shops, active shops, shop owners count, and user count"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Dashboard data retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AdminDashboardResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - user is not admin",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            )
    })
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard(
            Authentication authentication
    ) {
        log.info("Fetching admin dashboard");
        String adminEmail = authentication.getName();
        AdminService.AdminDashboardData data = adminService.getDashboardData(adminEmail);

        AdminDashboardResponse response = AdminDashboardResponse.builder()
                .totalShops(data.getTotalShops())
                .activeShops(data.getActiveShops())
                .inactiveShops(data.getInactiveShops())
                .totalShopOwners(data.getTotalShopOwners())
                .totalUsers(data.getTotalUsers())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Response DTO for admin dashboard
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @Schema(description = "Admin dashboard data")
    public static class AdminDashboardResponse {
        @Schema(description = "Total number of shops", example = "50")
        private long totalShops;

        @Schema(description = "Number of active shops", example = "45")
        private long activeShops;

        @Schema(description = "Number of inactive shops", example = "5")
        private long inactiveShops;

        @Schema(description = "Total number of shop owners", example = "50")
        private long totalShopOwners;

        @Schema(description = "Total number of users in system", example = "1000")
        private long totalUsers;
    }
}
