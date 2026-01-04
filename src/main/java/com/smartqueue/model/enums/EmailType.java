package com.smartqueue.model.enums;

/**
 * Email type enum for categorizing different email types
 * Used to determine retry behavior for failed emails
 */
public enum EmailType {
    /**
     * OTP (One-Time Password) authentication emails
     * Should NOT be retried automatically since OTP expires in 5 minutes
     */
    OTP,

    /**
     * General notification emails
     * Can be retried automatically
     */
    NOTIFICATION,

    /**
     * Queue update emails
     * Can be retried automatically
     */
    QUEUE_UPDATE,

    /**
     * System alert emails
     * Can be retried automatically
     */
    SYSTEM_ALERT
}
