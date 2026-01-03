package com.smartqueue.exception;

import com.smartqueue.model.enums.ErrorCode;

import java.util.UUID;

/**
 * Exception thrown when a user attempts to join a queue while already in another queue
 */
public class AlreadyInQueueException extends SmartQueueException {

    public AlreadyInQueueException(UUID shopId) {
        super(ErrorCode.ALREADY_IN_QUEUE,
              String.format("User is already in a queue at shop: %s", shopId));
    }

    public AlreadyInQueueException(String message) {
        super(ErrorCode.ALREADY_IN_QUEUE, message);
    }
}
