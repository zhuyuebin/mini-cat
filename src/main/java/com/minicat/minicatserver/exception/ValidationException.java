package com.minicat.minicatserver.exception;

/**
 * 参数验证异常（HTTP 400）。
 */
public class ValidationException extends DatabaseException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
    }

    public ValidationException(String field, String message) {
        super(ErrorCode.VALIDATION_FAILED, field + ": " + message);
    }
}
