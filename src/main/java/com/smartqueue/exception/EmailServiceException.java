package com.smartqueue.exception;

import com.smartqueue.model.enums.ErrorCode;

/**
 * Exception thrown when email service fails
 * Could be due to: SMTP connection issues, authentication failure, network problems
 * Circuit breaker will handle repeated failures
 */
public class EmailServiceException extends SmartQueueException {

    public EmailServiceException(String message) {
        super(ErrorCode.EMAIL_SERVICE_UNAVAILABLE, message);
    }

    public EmailServiceException(String message, Throwable cause) {
        super(ErrorCode.EMAIL_SERVICE_UNAVAILABLE, message, cause);
    }
}
