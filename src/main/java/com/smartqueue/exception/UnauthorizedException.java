package com.smartqueue.exception;

import com.smartqueue.model.enums.ErrorCode;

/**
 * Exception thrown when a user is not authorized
 * HTTP Status: 401 UNAUTHORIZED
 * Error Codes: AUTHZ001, AUTHZ002, AUTHZ003
 */
public class UnauthorizedException extends SmartQueueException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(ErrorCode.UNAUTHORIZED, message, cause);
    }
}
