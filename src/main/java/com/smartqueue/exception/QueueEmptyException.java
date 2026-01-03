package com.smartqueue.exception;

import com.smartqueue.model.enums.ErrorCode;

import java.util.UUID;

/**
 * Exception thrown when attempting to call next user from an empty queue
 */
public class QueueEmptyException extends SmartQueueException {

    public QueueEmptyException(UUID shopId) {
        super(ErrorCode.QUEUE_EMPTY,
              String.format("Queue is empty at shop: %s", shopId));
    }

    public QueueEmptyException(String message) {
        super(ErrorCode.QUEUE_EMPTY, message);
    }
}
