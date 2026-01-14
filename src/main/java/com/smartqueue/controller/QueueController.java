package com.smartqueue.controller;

import com.smartqueue.model.dto.JoinQueueRequest;
import com.smartqueue.model.dto.QueueResponse;
import com.smartqueue.service.QueueService;
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

import java.util.UUID;

/**
 * Queue management controller
 * Handles queue operations (join, leave, call next)
 */
@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Queue Management", description = "Endpoints for managing queue operations")
@SecurityRequirement(name = "bearerAuth")
public class QueueController {

    private final QueueService queueService;

    @Operation(
            summary = "Join a shop's queue",
            description = "Join the queue for a specific shop. You will receive your position in the queue " +
                    "and estimated wait time. Cannot join if queue is full or if already in queue."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully joined queue",
                    content = @Content(schema = @Schema(implementation = QueueResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Queue full, already in queue, or shop inactive",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shop not found",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            )
    })
    @PostMapping("/join")
    public ResponseEntity<QueueResponse> joinQueue(
            @Valid @RequestBody JoinQueueRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        log.info("User {} joining queue for shop {}", userEmail, request.getShopId());
        QueueResponse response = queueService.joinQueue(request.getShopId(), userEmail);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Leave a shop's queue",
            description = "Remove yourself from the queue. Cannot leave if you're currently being served."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Successfully left queue"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Not in queue or currently being served",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shop not found",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            )
    })
    @DeleteMapping("/leave/{shopId}")
    public ResponseEntity<Void> leaveQueue(
            @Parameter(description = "Shop UUID", required = true)
            @PathVariable UUID shopId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        log.info("User {} leaving queue for shop {}", userEmail, shopId);
        queueService.leaveQueue(shopId, userEmail);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Call next user in queue (Shop owner only)",
            description = "Mark the current user as served and call the next user in queue. " +
                    "Only the shop owner can call users. Returns details of the next user to be served."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Next user called successfully",
                    content = @Content(schema = @Schema(implementation = QueueResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Queue is empty or shop inactive",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized - only shop owner can call users",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shop not found",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            )
    })
    @PostMapping("/call-next/{shopId}")
    public ResponseEntity<QueueResponse> callNextUser(
            @Parameter(description = "Shop UUID", required = true)
            @PathVariable UUID shopId,
            Authentication authentication
    ) {
        String ownerEmail = authentication.getName();
        log.info("Owner {} calling next user for shop {}", ownerEmail, shopId);
        QueueResponse response = queueService.callNextUser(shopId, ownerEmail);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get my position in queue",
            description = "Get your current position in the queue for a specific shop, " +
                    "including estimated wait time and people ahead."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Position retrieved successfully",
                    content = @Content(schema = @Schema(implementation = QueueResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Not in queue",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shop not found",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            )
    })
    @GetMapping("/position/{shopId}")
    public ResponseEntity<QueueResponse> getQueuePosition(
            @Parameter(description = "Shop UUID", required = true)
            @PathVariable UUID shopId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        log.info("User {} checking position for shop {}", userEmail, shopId);
        QueueResponse response = queueService.getQueuePosition(shopId, userEmail);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Mark yourself as completed after being served",
            description = "Mark yourself as completed after being called and served. " +
                    "Users can only complete themselves after they have been called by the shop owner. " +
                    "This removes you from the queue and marks your service as complete."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully marked as served",
                    content = @Content(schema = @Schema(implementation = QueueResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "You have not been called yet, already completed, or shop inactive",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shop not found",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            )
    })
    @PostMapping("/complete/{shopId}")
    public ResponseEntity<QueueResponse> completeUser(
            @Parameter(description = "Shop UUID", required = true)
            @PathVariable UUID shopId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        log.info("User {} marking themselves as completed for shop {}", userEmail, shopId);
        QueueResponse response = queueService.completeUser(shopId, userEmail);
        return ResponseEntity.ok(response);
    }
}
