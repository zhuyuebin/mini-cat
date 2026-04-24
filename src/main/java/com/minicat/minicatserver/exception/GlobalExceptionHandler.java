package com.minicat.minicatserver.exception;

import com.minicat.minicatserver.dto.ApiResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理所有Controller层抛出的异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理自定义数据库异常
     */
    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleDatabaseException(DatabaseException ex) {
        logger.error("Database exception: {}", ex.getMessage(), ex);
        ApiResponseDTO<Void> response = ApiResponseDTO.error(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode()).body(response);
    }
    
    /**
     * 处理连接未找到异常
     */
    @ExceptionHandler(ConnectionNotFoundException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleConnectionNotFoundException(ConnectionNotFoundException ex) {
        logger.warn("Connection not found: {}", ex.getMessage());
        ApiResponseDTO<Void> response = ApiResponseDTO.error(404, ex.getMessage());
        return ResponseEntity.status(404).body(response);
    }
    
    /**
     * 处理SQL注入异常
     */
    @ExceptionHandler(SqlInjectionException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleSqlInjectionException(SqlInjectionException ex) {
        logger.warn("SQL injection detected: {}", ex.getMessage());
        ApiResponseDTO<Void> response = ApiResponseDTO.error(403, ex.getMessage());
        return ResponseEntity.status(403).body(response);
    }
    
    /**
     * 处理查询执行异常
     */
    @ExceptionHandler(QueryExecutionException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleQueryExecutionException(QueryExecutionException ex) {
        logger.error("Query execution failed: {}", ex.getMessage(), ex);
        ApiResponseDTO<Void> response = ApiResponseDTO.error(500, ex.getMessage());
        return ResponseEntity.status(500).body(response);
    }
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleValidationException(ValidationException ex) {
        logger.warn("Validation failed: {}", ex.getMessage());
        ApiResponseDTO<Void> response = ApiResponseDTO.error(400, ex.getMessage());
        return ResponseEntity.status(400).body(response);
    }
    
    /**
     * 处理Bean验证异常（@Valid注解触发）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        logger.warn("Validation errors: {}", errors);
        ApiResponseDTO<Map<String, String>> response = ApiResponseDTO.error(400, "Validation failed");
        response.setData(errors);
        return ResponseEntity.status(400).body(response);
    }
    
    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleBindException(BindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        logger.warn("Binding errors: {}", errors);
        ApiResponseDTO<Map<String, String>> response = ApiResponseDTO.error(400, "Parameter binding failed");
        response.setData(errors);
        return ResponseEntity.status(400).body(response);
    }
    
    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid parameter '%s': expected type %s, but got %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
                ex.getValue() != null ? ex.getValue().getClass().getSimpleName() : "null");
        
        logger.warn("Type mismatch: {}", message);
        ApiResponseDTO<Void> response = ApiResponseDTO.error(400, message);
        return ResponseEntity.status(400).body(response);
    }
    
    /**
     * 处理乐观锁异常
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleOptimisticLockingException(ObjectOptimisticLockingFailureException ex) {
        logger.warn("Optimistic locking failure: {}", ex.getMessage());
        ApiResponseDTO<Void> response = ApiResponseDTO.error(409, "Data has been modified by another operation. Please refresh and try again.");
        return ResponseEntity.status(409).body(response);
    }
    
    /**
     * 处理SQL异常
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleSQLException(SQLException ex) {
        logger.error("SQL error: {}", ex.getMessage(), ex);
        String message = "Database operation failed";
        
        // 根据SQL状态码提供更具体的错误信息
        if (ex.getSQLState() != null) {
            if (ex.getSQLState().startsWith("23")) {
                message = "Data integrity violation: " + ex.getMessage();
            } else if (ex.getSQLState().startsWith("42")) {
                message = "SQL syntax error: " + ex.getMessage();
            }
        }
        
        ApiResponseDTO<Void> response = ApiResponseDTO.error(500, message);
        return ResponseEntity.status(500).body(response);
    }
    
    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        ApiResponseDTO<Void> response = ApiResponseDTO.error(400, ex.getMessage());
        return ResponseEntity.status(400).body(response);
    }
    
    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleNullPointerException(NullPointerException ex) {
        logger.error("Null pointer exception: {}", ex.getMessage(), ex);
        ApiResponseDTO<Void> response = ApiResponseDTO.error(500, "Internal server error");
        return ResponseEntity.status(500).body(response);
    }
    
    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        ApiResponseDTO<Void> response = ApiResponseDTO.error(500, "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
