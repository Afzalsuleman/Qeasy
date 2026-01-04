package com.smartqueue.model.enums;

/**
 * User role enumeration
 * Defines the different roles a user can have in the system
 */
public enum UserRole {
    /**
     * Regular user - can join queues
     */
    USER,

    /**
     * Shop owner - can manage their shop and access analytics
     * Has access to:
     * - Create/update/delete their shop
     * - Call next user in their shop's queue
     * - View analytics for their shop
     */
    SHOP_OWNER
}
