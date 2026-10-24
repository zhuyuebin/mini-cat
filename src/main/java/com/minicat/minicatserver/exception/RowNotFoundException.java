package com.minicat.minicatserver.exception;

/**
 * 目标数据行不存在（HTTP 404）。
 */
public class RowNotFoundException extends DatabaseException {

    public RowNotFoundException(String message) {
        super(ErrorCode.ROW_NOT_FOUND, message);
    }
}
