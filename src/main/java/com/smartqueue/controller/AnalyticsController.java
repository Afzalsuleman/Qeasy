package com.smartqueue.controller;

import com.smartqueue.model.dto.AnalyticsResponse;
import com.smartqueue.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Analytics controller for queue metrics and statistics
 * Provides insights into queue performance
 */
@RestController
@RequestMapping("/api/v1/analytics")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Analytics", description = "Queue analytics and statistics endpoints")
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Autowired
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Get comprehensive analytics for a shop
     * Only shop owners can access analytics for their shops
     * Authorization is enforced via SecurityConfig (hasAnyRole SHOP_OWNER)
     *
     * @param shopId Shop UUID
     * @param days Number of days to analyze (default: 7)
     * @return AnalyticsResponse with metrics
     */
    @GetMapping("/shop/{shopId}")
    @Operation(
            summary = "Get shop analytics (Shop Owner only)",
            description = "Get comprehensive analytics for your shop including visitors, wait times, and completion rates. Only accessible by shop owners."
    )
    public ResponseEntity<AnalyticsResponse> getShopAnalytics(
            @Parameter(description = "Shop ID", required = true)
            @PathVariable UUID shopId,
            @Parameter(description = "Number of days to analyze", example = "7")
            @RequestParam(defaultValue = "7") int days) {

        log.info("Analytics request for shop {} (last {} days)", shopId, days);

        // Validate days parameter
        if (days < 1 || days > 365) {
            days = 7; // Default to 7 days if invalid
        }

        AnalyticsResponse analytics = analyticsService.getShopAnalytics(shopId, days);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get current queue statistics
     * Only shop owners can access analytics for their shops
     * Authorization is enforced via SecurityConfig (hasAnyRole SHOP_OWNER)
     *
     * @param shopId Shop UUID
     * @return AnalyticsResponse with current queue metrics
     */
    @GetMapping("/shop/{shopId}/current")
    @Operation(
            summary = "Get current queue stats (Shop Owner only)",
            description = "Get real-time queue statistics including current size and estimated wait time. Only accessible by shop owners."
    )
    public ResponseEntity<AnalyticsResponse> getCurrentQueueStats(
            @Parameter(description = "Shop ID", required = true)
            @PathVariable UUID shopId) {

        log.info("Current queue stats request for shop {}", shopId);

        AnalyticsResponse stats = analyticsService.getCurrentQueueStats(shopId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get today's statistics for a shop
     * Only shop owners can access analytics for their shops
     * Authorization is enforced via SecurityConfig (hasAnyRole SHOP_OWNER)
     *
     * @param shopId Shop UUID
     * @return AnalyticsResponse with today's metrics
     */
    @GetMapping("/shop/{shopId}/today")
    @Operation(
            summary = "Get today's stats (Shop Owner only)",
            description = "Get today's statistics for your shop including visitors and served count. Only accessible by shop owners."
    )
    public ResponseEntity<AnalyticsResponse> getTodayStats(
            @Parameter(description = "Shop ID", required = true)
            @PathVariable UUID shopId) {

        log.info("Today's stats request for shop {}", shopId);

        AnalyticsResponse stats = analyticsService.getTodayStats(shopId);
        return ResponseEntity.ok(stats);
    }
}
