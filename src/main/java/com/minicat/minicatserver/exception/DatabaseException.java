package com.minicat.minicatserver.exception;

/**
 * 数据库操作基础异常类。
 *
 * <p>所有业务异常都携带一个 {@link ErrorCode}，全局异常处理器据此映射
 * HTTP 状态码与响应体中的业务 code。
 */
public class DatabaseException extends RuntimeException {

    private final ErrorCode errorCode;

    public DatabaseException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public DatabaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DatabaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getBusinessCode() {
        return errorCode.getCode();
    }
}
