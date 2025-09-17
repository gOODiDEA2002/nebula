package io.nebula.core.config;

import io.nebula.core.common.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 配置验证器
 * 负责验证配置对象的有效性
 */
@RequiredArgsConstructor
public class ConfigurationValidator {
    
    private final Validator validator;
    
    /**
     * 验证配置对象
     * 
     * @param config 配置对象
     * @param <T>    配置类型
     * @return 验证结果
     */
    public <T extends ConfigurationProperties> ValidationResult validate(T config) {
        if (config == null) {
            return ValidationResult.builder()
                    .success(false)
                    .errors(List.of(createFieldError("config", "配置对象不能为null", null)))
                    .build();
        }
        
        List<FieldError> errors = new ArrayList<>();
        
        // JSR-303 验证
        Set<ConstraintViolation<T>> violations = validator.validate(config);
        for (ConstraintViolation<T> violation : violations) {
            errors.add(createFieldError(
                    violation.getPropertyPath().toString(),
                    violation.getMessage(),
                    violation.getInvalidValue()
            ));
        }
        
        // 自定义验证
        try {
            config.validate();
        } catch (ValidationException e) {
            errors.addAll(convertValidationException(e));
        } catch (Exception e) {
            errors.add(createFieldError("custom", e.getMessage(), null));
        }
        
        return ValidationResult.builder()
                .success(errors.isEmpty())
                .errors(errors)
                .configPrefix(config.prefix())
                .configDescription(config.description())
                .build();
    }
    
    /**
     * 验证配置对象，如果失败则抛出异常
     * 
     * @param config 配置对象
     * @param <T>    配置类型
     * @throws ValidationException 验证失败时抛出
     */
    public <T extends ConfigurationProperties> void validateAndThrow(T config) {
        ValidationResult result = validate(config);
        if (!result.isSuccess()) {
            List<ValidationException.FieldError> fieldErrors = result.getErrors().stream()
                    .map(error -> new ValidationException.FieldError(
                            error.getField(),
                            error.getMessage(),
                            error.getValue()
                    ))
                    .toList();
            throw new ValidationException(fieldErrors);
        }
    }
    
    /**
     * 创建字段错误
     * 
     * @param field   字段名
     * @param message 错误消息
     * @param value   字段值
     * @return 字段错误
     */
    private FieldError createFieldError(String field, String message, Object value) {
        return FieldError.builder()
                .field(field)
                .message(message)
                .value(value)
                .build();
    }
    
    /**
     * 转换验证异常为字段错误列表
     * 
     * @param e 验证异常
     * @return 字段错误列表
     */
    private List<FieldError> convertValidationException(ValidationException e) {
        return e.getFieldErrors().stream()
                .map(fieldError -> createFieldError(
                        fieldError.getField(),
                        fieldError.getMessage(),
                        fieldError.getValue()
                ))
                .toList();
    }
    
    /**
     * 验证结果
     */
    @Data
    @Builder
    public static class ValidationResult {
        /**
         * 是否验证成功
         */
        private boolean success;
        
        /**
         * 错误列表
         */
        private List<FieldError> errors;
        
        /**
         * 配置前缀
         */
        private String configPrefix;
        
        /**
         * 配置描述
         */
        private String configDescription;
        
        /**
         * 是否有错误
         * 
         * @return 是否有错误
         */
        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }
        
        /**
         * 获取错误数量
         * 
         * @return 错误数量
         */
        public int getErrorCount() {
            return errors != null ? errors.size() : 0;
        }
        
        /**
         * 获取第一个错误消息
         * 
         * @return 第一个错误消息
         */
        public String getFirstErrorMessage() {
            if (errors == null || errors.isEmpty()) {
                return null;
            }
            return errors.get(0).getMessage();
        }
        
        /**
         * 获取所有错误消息
         * 
         * @return 所有错误消息
         */
        public List<String> getAllErrorMessages() {
            if (errors == null) {
                return new ArrayList<>();
            }
            return errors.stream()
                    .map(FieldError::getMessage)
                    .toList();
        }
    }
    
    /**
     * 字段错误
     */
    @Data
    @Builder
    public static class FieldError {
        /**
         * 字段名
         */
        private String field;
        
        /**
         * 错误消息
         */
        private String message;
        
        /**
         * 字段值
         */
        private Object value;
        
        @Override
        public String toString() {
            return String.format("FieldError{field='%s', message='%s', value='%s'}", 
                    field, message, value);
        }
    }
}
