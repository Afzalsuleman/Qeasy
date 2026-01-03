package com.smartqueue.model.enums;

/**
 * Email delivery status enumeration
 * Tracks the state of failed email retry attempts
 */
public enum EmailStatus {
    /**
     * Email is pending retry
     */
    PENDING,

    /**
     * Email retry is in progress
     */
    RETRYING,

    /**
     * Email was successfully sent after retry
     */
    SUCCESS,

    /**
     * Email failed permanently after max retry attempts
     */
    FAILED
}
