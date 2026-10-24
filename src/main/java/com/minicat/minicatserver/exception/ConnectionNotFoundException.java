package com.minicat.minicatserver.exception;

/**
 * 数据库连接不存在异常（HTTP 404）。
 */
public class ConnectionNotFoundException extends DatabaseException {

    public ConnectionNotFoundException(String connectionId) {
        super(ErrorCode.CONNECTION_NOT_FOUND, "连接不存在: " + connectionId);
    }

    public ConnectionNotFoundException(String message, String connectionId) {
        super(ErrorCode.CONNECTION_NOT_FOUND, message + ": " + connectionId);
    }
}
