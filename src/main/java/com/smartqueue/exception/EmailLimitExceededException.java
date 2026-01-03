package com.smartqueue.exception;

import com.smartqueue.model.enums.ErrorCode;

/**
 * Exception thrown when Gmail daily email limit is exceeded (500 emails/day)
 * This is a graceful degradation - email service should not block queue operations
 */
public class EmailLimitExceededException extends SmartQueueException {

    public EmailLimitExceededException(int currentCount, int limit) {
        super(ErrorCode.EMAIL_SERVICE_UNAVAILABLE,
              String.format("Daily email limit exceeded: %d/%d emails sent", currentCount, limit));
    }

    public EmailLimitExceededException(String message) {
        super(ErrorCode.EMAIL_SERVICE_UNAVAILABLE, message);
    }
}
