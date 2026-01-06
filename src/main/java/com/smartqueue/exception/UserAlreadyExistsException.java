package com.smartqueue.exception;

import com.smartqueue.model.enums.ErrorCode;

/**
 * Exception thrown when attempting to create a user that already exists
 */
public class UserAlreadyExistsException extends SmartQueueException {

    public UserAlreadyExistsException(String message) {
        super(ErrorCode.SHOP_ALREADY_EXISTS, message);
    }

    public UserAlreadyExistsException(String message, Throwable cause) {
        super(ErrorCode.SHOP_ALREADY_EXISTS, message, cause);
    }
}
