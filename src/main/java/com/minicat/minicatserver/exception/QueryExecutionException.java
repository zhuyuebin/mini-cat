package com.minicat.minicatserver.exception;

/**
 * 数据库查询/执行失败异常（HTTP 500）。
 */
public class QueryExecutionException extends DatabaseException {

    public QueryExecutionException(String message) {
        super(ErrorCode.SQL_EXECUTION_FAILED, message);
    }

    public QueryExecutionException(String message, Throwable cause) {
        super(ErrorCode.SQL_EXECUTION_FAILED, message, cause);
    }

    public static QueryExecutionException fromSql(String sql, Throwable cause) {
        String summary = sql == null ? "" : sql.strip();
        if (summary.length() > 200) {
            summary = summary.substring(0, 200) + "...";
        }
        return new QueryExecutionException("SQL 执行失败: " + summary, cause);
    }
}
