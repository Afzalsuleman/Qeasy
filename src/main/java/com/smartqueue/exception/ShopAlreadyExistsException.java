package com.smartqueue.exception;

import com.smartqueue.model.enums.ErrorCode;

import java.util.UUID;

/**
 * Exception thrown when a user attempts to create a shop but already owns one
 * MVP restriction: One active shop per owner
 */
public class ShopAlreadyExistsException extends SmartQueueException {

    public ShopAlreadyExistsException(UUID userId) {
        super(ErrorCode.SHOP_ALREADY_EXISTS,
              String.format("User %s already owns an active shop", userId));
    }

    public ShopAlreadyExistsException(String message) {
        super(ErrorCode.SHOP_ALREADY_EXISTS, message);
    }
}
