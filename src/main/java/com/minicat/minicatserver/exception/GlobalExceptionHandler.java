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
 * 全局异常处理器。
 *
 * <p>业务异常（{@link DatabaseException} 家族）统一按 {@link ErrorCode} 映射
 * HTTP 状态码 + 业务 code；其他异常提供合理的兜底，避免把内部细节直接抛给前端。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理所有业务异常（连接不存在 / 参数校验 / SQL 拦截 / 执行失败）。
     */
    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleDatabaseException(DatabaseException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        if (errorCode.getHttpStatus().is5xxServerError()) {
            logger.error("Database exception: code={}, message={}", errorCode.getCode(), ex.getMessage(), ex);
        } else {
            logger.warn("Business exception: code={}, message={}", errorCode.getCode(), ex.getMessage());
        }
        ApiResponseDTO<Void> response = ApiResponseDTO.error(errorCode.getCode(), ex.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    /**
     * 处理 Bean 校验异常（@Valid 触发）。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            errors.put(fieldName, error.getDefaultMessage());
        });

        logger.warn("Validation errors: {}", errors);
        ApiResponseDTO<Map<String, String>> response =
                ApiResponseDTO.error(ErrorCode.VALIDATION_FAILED.getCode(), "参数校验失败");
        response.setData(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理参数绑定异常。
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleBindException(BindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            errors.put(fieldName, error.getDefaultMessage());
        });

        logger.warn("Binding errors: {}", errors);
        ApiResponseDTO<Map<String, String>> response =
                ApiResponseDTO.error(ErrorCode.VALIDATION_FAILED.getCode(), "参数绑定失败");
        response.setData(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理参数类型不匹配。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        String message = String.format("参数 '%s' 类型不合法: 期望 %s, 实际 %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
                ex.getValue() != null ? ex.getValue().getClass().getSimpleName() : "null");

        logger.warn("Type mismatch: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error(ErrorCode.VALIDATION_FAILED.getCode(), message));
    }

    /**
     * 处理乐观锁冲突。
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleOptimisticLockingException(
            ObjectOptimisticLockingFailureException ex) {
        logger.warn("Optimistic locking failure: {}", ex.getMessage());
        return ResponseEntity.status(ErrorCode.CONFLICT.getHttpStatus())
                .body(ApiResponseDTO.error(ErrorCode.CONFLICT));
    }

    /**
     * 处理未被包装的 JDBC 异常（兜底，正常情况下应被 Service 层包装为 QueryExecutionException）。
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleSQLException(SQLException ex) {
        logger.error("SQL error: {}, sqlState={}", ex.getMessage(), ex.getSQLState(), ex);
        String message = "数据库操作失败";
        if (ex.getSQLState() != null) {
            if (ex.getSQLState().startsWith("23")) {
                message = "数据完整性约束冲突: " + ex.getMessage();
            } else if (ex.getSQLState().startsWith("42")) {
                message = "SQL 语法错误: " + ex.getMessage();
            }
        }
        return ResponseEntity.status(ErrorCode.SQL_EXECUTION_FAILED.getHttpStatus())
                .body(ApiResponseDTO.error(ErrorCode.SQL_EXECUTION_FAILED.getCode(), message));
    }

    /**
     * 处理非法参数。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error(ErrorCode.VALIDATION_FAILED.getCode(), ex.getMessage()));
    }

    /**
     * 处理空指针（视为服务端缺陷，记录完整堆栈，不向前端暴露细节）。
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleNullPointerException(NullPointerException ex) {
        logger.error("Null pointer exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.error(ErrorCode.INTERNAL));
    }

    /**
     * 兜底：运行时异常。
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleRuntimeException(RuntimeException ex) {
        logger.warn("Request failed: {}", ex.getMessage());
        String message = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? ErrorCode.INTERNAL.getDefaultMessage()
                : ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.error(ErrorCode.INTERNAL.getCode(), message));
    }

    /**
     * 兜底：其他所有异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.error(ErrorCode.INTERNAL));
    }
}
