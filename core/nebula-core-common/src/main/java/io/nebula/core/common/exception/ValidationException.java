package io.nebula.core.common.exception;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证异常
 * 用于表示数据验证错误，通常包含多个字段错误
 */
@Getter
public class ValidationException extends NebulaException {
    
    /**
     * 字段错误列表
     */
    private final List<FieldError> fieldErrors;
    
    /**
     * 构造函数
     * 
     * @param fieldErrors 字段错误列表
     */
    public ValidationException(List<FieldError> fieldErrors) {
        super("VALIDATION_FAILED", "数据验证失败");
        this.fieldErrors = fieldErrors != null ? fieldErrors : new ArrayList<>();
    }
    
    /**
     * 构造函数（单个字段错误）
     * 
     * @param field   字段名
     * @param message 错误消息
     * @param value   字段值
     */
    public ValidationException(String field, String message, Object value) {
        this(List.of(new FieldError(field, message, value)));
    }
    
    /**
     * 创建验证异常的便捷方法
     * 
     * @param field   字段名
     * @param message 错误消息
     * @return 验证异常实例
     */
    public static ValidationException of(String field, String message) {
        return new ValidationException(field, message, null);
    }
    
    /**
     * 创建验证异常的便捷方法（带字段值）
     * 
     * @param field   字段名
     * @param message 错误消息
     * @param value   字段值
     * @return 验证异常实例
     */
    public static ValidationException of(String field, String message, Object value) {
        return new ValidationException(field, message, value);
    }
    
    /**
     * 创建多字段验证异常
     * 
     * @param fieldErrors 字段错误列表
     * @return 验证异常实例
     */
    public static ValidationException of(List<FieldError> fieldErrors) {
        return new ValidationException(fieldErrors);
    }
    
    /**
     * 添加字段错误
     * 
     * @param field   字段名
     * @param message 错误消息
     * @param value   字段值
     * @return 当前异常实例（链式调用）
     */
    public ValidationException addFieldError(String field, String message, Object value) {
        this.fieldErrors.add(new FieldError(field, message, value));
        return this;
    }
    
    /**
     * 检查是否有字段错误
     * 
     * @return 是否有错误
     */
    public boolean hasErrors() {
        return !fieldErrors.isEmpty();
    }
    
    /**
     * 获取错误数量
     * 
     * @return 错误数量
     */
    public int getErrorCount() {
        return fieldErrors.size();
    }
    
    @Override
    public String getFormattedMessage() {
        if (fieldErrors.isEmpty()) {
            return getMessage();
        }
        
        StringBuilder sb = new StringBuilder(getMessage());
        sb.append(": ");
        for (int i = 0; i < fieldErrors.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            FieldError error = fieldErrors.get(i);
            sb.append(error.getField()).append(": ").append(error.getMessage());
        }
        return sb.toString();
    }
    
    /**
     * 字段错误信息
     */
    @Getter
    public static class FieldError {
        /**
         * 字段名
         */
        private final String field;
        
        /**
         * 错误消息
         */
        private final String message;
        
        /**
         * 字段值
         */
        private final Object value;
        
        /**
         * 构造函数
         * 
         * @param field   字段名
         * @param message 错误消息
         * @param value   字段值
         */
        public FieldError(String field, String message, Object value) {
            this.field = field;
            this.message = message;
            this.value = value;
        }
        
        @Override
        public String toString() {
            return String.format("FieldError{field='%s', message='%s', value='%s'}", 
                    field, message, value);
        }
    }
}
