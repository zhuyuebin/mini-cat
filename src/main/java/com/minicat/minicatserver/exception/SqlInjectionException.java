package com.minicat.minicatserver.exception;

/**
 * SQL注入检测异常
 */
public class SqlInjectionException extends DatabaseException {
    
    public SqlInjectionException(String message) {
        super(message, 403);
    }
    
    public SqlInjectionException() {
        super("Potential SQL injection detected", 403);
    }
}
