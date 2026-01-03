package com.smartqueue.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standardized error response format for all API errors
 * As specified in tech-design-v3.md section 5.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * Timestamp when error occurred (ISO 8601 format)
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    /**
     * HTTP status code (400, 401, 403, 404, 409, 500, etc.)
     */
    private Integer status;

    /**
     * HTTP status text (Bad Request, Unauthorized, etc.)
     */
    private String error;

    /**
     * Human-readable error message (safe for display to end users)
     */
    private String message;

    /**
     * Machine-readable error code from ErrorCode enum (e.g., AUTH001, QUEUE001)
     */
    private String code;

    /**
     * Request path where error occurred
     */
    private String path;

    /**
     * Correlation ID for tracing across logs
     */
    private String correlationId;
}
