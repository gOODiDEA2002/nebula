package io.nebula.web.controller;

import io.nebula.core.common.result.Result;
import io.nebula.core.common.result.PageResult;
import io.nebula.core.common.exception.BusinessException;
import io.nebula.core.common.exception.ValidationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import java.util.stream.Collectors;
import java.util.List;

/**
 * Web 控制器基础类
 * 提供统一的响应处理和异常处理
 */
public abstract class BaseController {
    
    /**
     * 成功响应
     */
    protected <T> Result<T> success(T data) {
        return Result.success(data);
    }
    
    /**
     * 成功响应（无数据）
     */
    protected Result<Void> success() {
        return Result.success();
    }
    
    /**
     * 错误响应（只有消息）
     */
    protected <T> Result<T> error(String message) {
        return Result.businessError(message);
    }
    
    /**
     * 错误响应（带错误码）
     */
    protected <T> Result<T> error(String code, String message) {
        return Result.error(code, message);
    }
    
    /**
     * 分页成功响应
     */
    protected <T> PageResult<T> pageSuccess(List<T> data, long total, long current, long size) {
        return PageResult.success(data, (int)current, (int)size, total);
    }
    
    /**
     * 验证参数绑定结果
     */
    protected void validateBindingResult(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<ValidationException.FieldError> fieldErrors = bindingResult.getFieldErrors()
                    .stream()
                    .map(error -> new ValidationException.FieldError(
                            error.getField(),
                            error.getDefaultMessage(),
                            error.getRejectedValue()))
                    .collect(Collectors.toList());
            throw new ValidationException(fieldErrors);
        }
    }
}