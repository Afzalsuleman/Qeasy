package com.smartqueue.exception;

import com.smartqueue.model.enums.ErrorCode;

/**
 * Exception thrown when OTP validation fails
 * Could be due to: wrong OTP, expired OTP, or OTP not found
 */
public class InvalidOtpException extends SmartQueueException {

    public InvalidOtpException(String message) {
        super(ErrorCode.INVALID_OTP, message);
    }

    public InvalidOtpException() {
        super(ErrorCode.INVALID_OTP, "Invalid or expired OTP");
    }
}
