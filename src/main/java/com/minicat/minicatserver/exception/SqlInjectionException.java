package com.minicat.minicatserver.exception;

/**
 * SQL 安全校验拦截异常（HTTP 403）。
 */
public class SqlInjectionException extends DatabaseException {

    public SqlInjectionException(String message) {
        super(ErrorCode.SQL_BLOCKED, message);
    }

    public SqlInjectionException() {
        super(ErrorCode.SQL_BLOCKED);
    }
}
