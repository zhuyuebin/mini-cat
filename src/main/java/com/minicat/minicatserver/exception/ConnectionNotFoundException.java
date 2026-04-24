package com.minicat.minicatserver.exception;

/**
 * 数据库连接未找到异常
 */
public class ConnectionNotFoundException extends DatabaseException {
    
    public ConnectionNotFoundException(String connectionId) {
        super("Database connection not found: " + connectionId, 404);
    }
    
    public ConnectionNotFoundException(String message, String connectionId) {
        super(message + ": " + connectionId, 404);
    }
}
