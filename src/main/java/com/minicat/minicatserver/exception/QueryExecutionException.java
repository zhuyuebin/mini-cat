package com.minicat.minicatserver.exception;

/**
 * 数据库查询执行异常
 */
public class QueryExecutionException extends DatabaseException {
    
    public QueryExecutionException(String message) {
        super(message, 500);
    }
    
    public QueryExecutionException(String message, Throwable cause) {
        super(message, 500, cause);
    }
    
    public static QueryExecutionException fromSql(String sql, Throwable cause) {
        return new QueryExecutionException("Failed to execute query: " + sql, cause);
    }
}
