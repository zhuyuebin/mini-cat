package com.minicat.minicatserver.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码。
 *
 * <p>约定：{@code 4xxxx} 表示客户端/请求侧问题，{@code 5xxxx} 表示服务端或数据库侧问题；
 * HTTP 状态码由 {@link #getHttpStatus()} 决定，响应体中的 {@code code} 使用 {@link #getCode()}。
 * 成功响应的 code 固定为 200。
 */
public enum ErrorCode {

    CONNECTION_NOT_FOUND(40401, HttpStatus.NOT_FOUND, "连接不存在"),
    VALIDATION_FAILED(40001, HttpStatus.BAD_REQUEST, "参数校验失败"),
    SQL_BLOCKED(40301, HttpStatus.FORBIDDEN, "SQL 被安全策略拦截"),
    CONFLICT(40901, HttpStatus.CONFLICT, "数据已被其他操作修改，请刷新后重试"),
    SQL_EXECUTION_FAILED(50001, HttpStatus.INTERNAL_SERVER_ERROR, "SQL 执行失败"),
    CONNECTION_FAILED(50002, HttpStatus.INTERNAL_SERVER_ERROR, "数据库连接失败"),
    INTERNAL(50000, HttpStatus.INTERNAL_SERVER_ERROR, "内部错误");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
