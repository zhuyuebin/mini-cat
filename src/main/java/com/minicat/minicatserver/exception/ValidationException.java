package com.minicat.minicatserver.exception;

/**
 * 参数验证异常
 */
public class ValidationException extends DatabaseException {
    
    public ValidationException(String message) {
        super(message, 400);
    }
    
    public ValidationException(String field, String message) {
        super(field + ": " + message, 400);
    }
}
