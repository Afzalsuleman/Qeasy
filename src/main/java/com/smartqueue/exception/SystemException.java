package com.smartqueue.exception;

import com.smartqueue.model.enums.ErrorCode;

/**
 * Exception thrown for system-level errors
 * HTTP Status: 500 INTERNAL_SERVER_ERROR
 * Error Codes: SYS001, SYS002, SYS003, SYS004
 */
public class SystemException extends SmartQueueException {

    public SystemException(String message) {
        super(ErrorCode.SYSTEM_ERROR, message);
    }

    public SystemException(String message, Throwable cause) {
        super(ErrorCode.SYSTEM_ERROR, message, cause);
    }
}
