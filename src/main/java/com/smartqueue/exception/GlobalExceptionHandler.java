package com.smartqueue.exception;

import com.smartqueue.model.dto.ErrorResponse;
import com.smartqueue.model.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler for standardized error responses
 * Maps all exceptions to ErrorResponse DTO with appropriate HTTP status codes
 * As specified in tech-design-v3.md section 5.2
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ========== Queue Exceptions ==========

    @ExceptionHandler(QueueFullException.class)
    public ResponseEntity<ErrorResponse> handleQueueFull(
            QueueFullException ex, HttpServletRequest request) {
        log.warn("[{}] Queue full: {}", getCorrelationId(), ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(AlreadyInQueueException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyInQueue(
            AlreadyInQueueException ex, HttpServletRequest request) {
        log.warn("[{}] User already in queue: {}", getCorrelationId(), ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(UserNotInQueueException.class)
    public ResponseEntity<ErrorResponse> handleUserNotInQueue(
            UserNotInQueueException ex, HttpServletRequest request) {
        log.warn("[{}] User not in queue: {}", getCorrelationId(), ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(QueueEmptyException.class)
    public ResponseEntity<ErrorResponse> handleQueueEmpty(
            QueueEmptyException ex, HttpServletRequest request) {
        log.warn("[{}] Queue empty: {}", getCorrelationId(), ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    // ========== Shop Exceptions ==========

    @ExceptionHandler(ShopNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleShopNotFound(
            ShopNotFoundException ex, HttpServletRequest request) {
        log.warn("[{}] Shop not found: {}", getCorrelationId(), ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(ShopInactiveException.class)
    public ResponseEntity<ErrorResponse> handleShopInactive(
            ShopInactiveException ex, HttpServletRequest request) {
        log.warn("[{}] Shop inactive: {}", getCorrelationId(), ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ShopAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleShopAlreadyExists(
            ShopAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("[{}] Shop already exists: {}", getCorrelationId(), ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    // ========== Authentication Exceptions ==========

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOtp(
            InvalidOtpException ex, HttpServletRequest request) {
        log.warn("[{}] Invalid OTP: {}", getCorrelationId(), ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException ex, HttpServletRequest request) {
        log.warn("[{}] Invalid token: {}", getCorrelationId(), ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, request);
    }

    // ========== Email Service Exceptions ==========

    @ExceptionHandler(EmailLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleEmailLimitExceeded(
            EmailLimitExceededException ex, HttpServletRequest request) {
        log.error("[{}] Email limit exceeded: {}", getCorrelationId(), ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    @ExceptionHandler(EmailServiceException.class)
    public ResponseEntity<ErrorResponse> handleEmailServiceException(
            EmailServiceException ex, HttpServletRequest request) {
        log.error("[{}] Email service error: {}", getCorrelationId(), ex.getMessage(), ex);
        return buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<ErrorResponse> handleMailException(
            MailException ex, HttpServletRequest request) {
        log.error("[{}] Mail exception: {}", getCorrelationId(), ex.getMessage(), ex);
        return buildErrorResponse(
                ErrorCode.EMAIL_SERVICE_UNAVAILABLE,
                "Email service is temporarily unavailable",
                HttpStatus.SERVICE_UNAVAILABLE,
                request
        );
    }

    // ========== Authorization Exceptions ==========

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("[{}] Access denied: {}", getCorrelationId(), ex.getMessage());
        return buildErrorResponse(
                ErrorCode.FORBIDDEN,
                "Access denied",
                HttpStatus.FORBIDDEN,
                request
        );
    }

    // ========== Validation Exceptions ==========

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("[{}] Validation error: {}", getCorrelationId(), errors);

        return buildErrorResponse(
                ErrorCode.INVALID_REQUEST_BODY,
                "Validation failed: " + errors,
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("[{}] Data integrity violation: {}", getCorrelationId(), ex.getMessage());
        return buildErrorResponse(
                ErrorCode.VALUE_OUT_OF_RANGE,
                "Data integrity violation - check constraints and unique values",
                HttpStatus.CONFLICT,
                request
        );
    }

    // ========== Generic SmartQueueException ==========

    @ExceptionHandler(SmartQueueException.class)
    public ResponseEntity<ErrorResponse> handleSmartQueueException(
            SmartQueueException ex, HttpServletRequest request) {
        log.warn("[{}] SmartQueue exception: {} - {}", getCorrelationId(), ex.getCode(), ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    // ========== Generic Exception (Catch-all) ==========

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("[{}] Unhandled exception: {}", getCorrelationId(), ex.getMessage(), ex);
        return buildErrorResponse(
                ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );
    }

    // ========== Helper Methods ==========

    /**
     * Build error response from SmartQueueException
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            SmartQueueException ex,
            HttpStatus status,
            HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .code(ex.getCode())
                .path(request.getRequestURI())
                .correlationId(getCorrelationId())
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Build error response with custom ErrorCode
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            ErrorCode errorCode,
            String message,
            HttpStatus status,
            HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .code(errorCode.getCode())
                .path(request.getRequestURI())
                .correlationId(getCorrelationId())
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Get correlation ID from MDC (set by CorrelationIdFilter)
     */
    private String getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : "N/A";
    }
}
