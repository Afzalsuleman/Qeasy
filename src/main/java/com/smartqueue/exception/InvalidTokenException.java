package com.smartqueue.exception;

import com.smartqueue.model.enums.ErrorCode;

/**
 * Exception thrown when JWT token validation fails
 * Could be due to: malformed token, expired token, invalid signature
 */
public class InvalidTokenException extends SmartQueueException {

    public InvalidTokenException(String message) {
        super(ErrorCode.INVALID_TOKEN, message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(ErrorCode.INVALID_TOKEN, message, cause);
    }

    public InvalidTokenException() {
        super(ErrorCode.INVALID_TOKEN, "Invalid or expired JWT token");
    }
}
