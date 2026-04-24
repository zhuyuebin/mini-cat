package com.minicat.minicatserver.exception;

/**
 * 数据库操作基础异常类
 */
public class DatabaseException extends RuntimeException {
    
    private Integer errorCode;
    
    public DatabaseException(String message) {
        super(message);
        this.errorCode = 500;
    }
    
    public DatabaseException(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = 500;
    }
    
    public DatabaseException(String message, Integer errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public Integer getErrorCode() {
        return errorCode;
    }
}
