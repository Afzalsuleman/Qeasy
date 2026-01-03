package com.smartqueue.model.enums;

/**
 * Queue operation status enumeration
 * Represents the lifecycle of a user in a queue
 */
public enum QueueStatus {
    /**
     * User has joined the queue
     */
    JOINED,

    /**
     * User has been called for service
     */
    CALLED,

    /**
     * User has been served and completed
     */
    SERVED,

    /**
     * User voluntarily left the queue
     */
    LEFT,

    /**
     * User did not respond when called (auto-detected after 3 minutes)
     */
    NO_SHOW,

    /**
     * Queue entry was cancelled by shop owner
     */
    CANCELLED
}
