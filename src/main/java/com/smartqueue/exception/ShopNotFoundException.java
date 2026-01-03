package com.smartqueue.exception;

import com.smartqueue.model.enums.ErrorCode;

import java.util.UUID;

/**
 * Exception thrown when a shop is not found by ID
 */
public class ShopNotFoundException extends SmartQueueException {

    public ShopNotFoundException(UUID shopId) {
        super(ErrorCode.SHOP_NOT_FOUND,
              String.format("Shop not found with ID: %s", shopId));
    }

    public ShopNotFoundException(String message) {
        super(ErrorCode.SHOP_NOT_FOUND, message);
    }
}
