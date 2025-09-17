package io.nebula.web.exception;

import io.nebula.core.common.exception.BusinessException;
import io.nebula.core.common.exception.SystemException;
import io.nebula.core.common.exception.ValidationException;
import io.nebula.core.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理应用程序中的异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        logger.warn("业务异常: {}, URL: {}", e.getMessage(), request.getRequestURL());
        return ResponseEntity.ok(Result.error(e.getErrorCode(), e.getMessage()));
    }
    
    /**
     * 处理系统异常
     */
    @ExceptionHandler(SystemException.class)
    public ResponseEntity<Result<Void>> handleSystemException(SystemException e, HttpServletRequest request) {
        logger.error("系统异常: {}, URL: {}", e.getMessage(), request.getRequestURL(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(e.getErrorCode(), e.getMessage()));
    }
    
    /**
     * 处理验证异常
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Result<Void>> handleValidationException(ValidationException e, HttpServletRequest request) {
        logger.warn("验证异常: {}, URL: {}", e.getMessage(), request.getRequestURL());
        return ResponseEntity.badRequest()
                .body(Result.error(e.getErrorCode(), e.getMessage()));
    }
    
    /**
     * 处理方法参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errors = e.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        logger.warn("参数验证异常: {}, URL: {}", errors, request.getRequestURL());
        return ResponseEntity.badRequest()
                .body(Result.error("VALIDATION_ERROR", "参数验证失败: " + errors));
    }
    
    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e, HttpServletRequest request) {
        String errors = e.getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        logger.warn("绑定异常: {}, URL: {}", errors, request.getRequestURL());
        return ResponseEntity.badRequest()
                .body(Result.error("BIND_ERROR", "数据绑定失败: " + errors));
    }
    
    /**
     * 处理未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e, HttpServletRequest request) {
        logger.error("未知异常: {}, URL: {}", e.getMessage(), request.getRequestURL(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error("INTERNAL_ERROR", "服务器内部错误"));
    }
}
