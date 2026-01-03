package com.smartqueue.exception;

import com.smartqueue.model.enums.ErrorCode;

import java.util.UUID;

/**
 * Exception thrown when attempting to interact with an inactive shop
 */
public class ShopInactiveException extends SmartQueueException {

    public ShopInactiveException(UUID shopId) {
        super(ErrorCode.SHOP_INACTIVE,
              String.format("Shop is currently inactive: %s", shopId));
    }

    public ShopInactiveException(String message) {
        super(ErrorCode.SHOP_INACTIVE, message);
    }
}
