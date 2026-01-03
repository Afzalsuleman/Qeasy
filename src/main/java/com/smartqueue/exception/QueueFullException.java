package com.smartqueue.exception;

import com.smartqueue.model.enums.ErrorCode;

/**
 * Exception thrown when attempting to join a queue that has reached maximum capacity
 */
public class QueueFullException extends SmartQueueException {

    public QueueFullException(String message) {
        super(ErrorCode.QUEUE_FULL, message);
    }

    public QueueFullException() {
        super(ErrorCode.QUEUE_FULL, "Queue has reached maximum capacity");
    }
}
