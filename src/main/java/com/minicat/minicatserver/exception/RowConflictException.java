package com.minicat.minicatserver.exception;

/**
 * 目标数据行自页面加载后已被修改（乐观冲突，HTTP 409）。
 */
public class RowConflictException extends DatabaseException {

    public RowConflictException(String message) {
        super(ErrorCode.ROW_CONFLICT, message);
    }
}
