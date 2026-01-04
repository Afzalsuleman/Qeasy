package com.smartqueue.controller;

import com.smartqueue.model.dto.AuthResponse;
import com.smartqueue.model.dto.GenerateOtpRequest;
import com.smartqueue.model.dto.VerifyOtpRequest;
import com.smartqueue.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller
 * Handles OTP generation and verification for user authentication
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "OTP-based authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Generate OTP",
            description = "Generate and send a 6-digit OTP code to the user's email address. " +
                    "The OTP is valid for 5 minutes. If the user doesn't exist, a new account will be created."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP sent successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or too many OTP attempts",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Daily email limit exceeded",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            )
    })
    @PostMapping("/generate-otp")
    public ResponseEntity<AuthResponse> generateOtp(
            @Valid @RequestBody GenerateOtpRequest request
    ) {
        log.info("OTP generation requested for email: {}", request.getEmail());
        AuthResponse response = authService.generateOtp(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Verify OTP and get JWT token",
            description = "Verify the OTP code and receive a JWT token for authentication. " +
                    "The token should be included in the Authorization header as 'Bearer <token>' " +
                    "for all protected endpoints. Token expires in 24 hours."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP verified successfully, JWT token returned",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired OTP",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication failed",
                    content = @Content(schema = @Schema(implementation = com.smartqueue.model.dto.ErrorResponse.class))
            )
    })
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        log.info("OTP verification requested for email: {}", request.getEmail());
        AuthResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Health check for auth service",
            description = "Simple endpoint to verify the authentication service is running"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Service is healthy"
    )
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
}
