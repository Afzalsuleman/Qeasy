package com.smartqueue.exception;

import com.smartqueue.model.enums.ErrorCode;
import lombok.Getter;

/**
 * Base exception class for Smart Queue application
 * All custom exceptions should extend this class
 */
@Getter
public class SmartQueueException extends RuntimeException {

    private final ErrorCode errorCode;

    public SmartQueueException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SmartQueueException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getCode() {
        return errorCode.getCode();
    }
}
