package com.smartqueue.model.enums;

import lombok.Getter;

/**
 * Standardized error codes for the Smart Queue API
 * Provides machine-readable error codes with human-readable messages
 * Categories: AUTH, QUEUE, SHOP, AUTHZ, VAL, SYS
 */
@Getter
public enum ErrorCode {
    // Authentication Errors (AUTH001-AUTH005)
    INVALID_OTP("AUTH001", "Invalid or expired OTP"),
    INVALID_TOKEN("AUTH002", "Invalid or expired JWT token"),
    MISSING_TOKEN("AUTH003", "Missing authentication token"),
    OTP_NOT_FOUND("AUTH004", "OTP not found (expired or never sent)"),
    EMAIL_SERVICE_UNAVAILABLE("AUTH005", "Email service unavailable"),

    // Queue Errors (QUEUE001-QUEUE005)
    QUEUE_FULL("QUEUE001", "Queue has reached maximum capacity"),
    ALREADY_IN_QUEUE("QUEUE002", "User is already in a queue"),
    NOT_IN_QUEUE("QUEUE003", "User is not in this queue"),
    QUEUE_EMPTY("QUEUE004", "Queue is empty"),
    QUEUE_NOT_FOUND("QUEUE005", "Shop queue not found"),

    // Shop Errors (SHOP001-SHOP004)
    SHOP_NOT_FOUND("SHOP001", "Shop not found"),
    SHOP_INACTIVE("SHOP002", "Shop is currently inactive"),
    SHOP_ALREADY_EXISTS("SHOP003", "User already owns a shop"),
    INVALID_SHOP_CONFIG("SHOP004", "Invalid shop configuration"),

    // Authorization Errors (AUTHZ001-AUTHZ003)
    UNAUTHORIZED("AUTHZ001", "Unauthorized access"),
    FORBIDDEN("AUTHZ002", "Access denied"),
    NOT_SHOP_OWNER("AUTHZ003", "User is not the shop owner"),
    INVALID_JWT_CLAIMS("AUTHZ004", "Invalid JWT claims"),

    // Validation Errors (VAL001-VAL004)
    INVALID_EMAIL("VAL001", "Invalid email format"),
    INVALID_REQUEST_BODY("VAL002", "Invalid request body"),
    MISSING_REQUIRED_FIELD("VAL003", "Missing required field"),
    VALUE_OUT_OF_RANGE("VAL004", "Value out of range"),

    // System Errors (SYS001-SYS005)
    SYSTEM_ERROR("SYS001", "System error"),
    INTERNAL_ERROR("SYS002", "Internal server error"),
    DATABASE_ERROR("SYS003", "Database connection failed"),
    REDIS_ERROR("SYS004", "Redis connection failed"),
    EXTERNAL_SERVICE_ERROR("SYS005", "External service unavailable");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String toString() {
        return code + ": " + message;
    }
}
