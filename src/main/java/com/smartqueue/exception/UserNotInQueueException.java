package com.smartqueue.exception;

import com.smartqueue.model.enums.ErrorCode;

import java.util.UUID;

/**
 * Exception thrown when attempting to perform an operation on a user not in the queue
 */
public class UserNotInQueueException extends SmartQueueException {

    public UserNotInQueueException(UUID userId, UUID shopId) {
        super(ErrorCode.NOT_IN_QUEUE,
              String.format("User %s is not in queue at shop %s", userId, shopId));
    }

    public UserNotInQueueException(String message) {
        super(ErrorCode.NOT_IN_QUEUE, message);
    }
}
