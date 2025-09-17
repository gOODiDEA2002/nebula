package io.nebula.core.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一响应结果
 * 
 * @param <T> 数据类型
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {
    
    /**
     * 成功标识
     */
    private boolean success;
    
    /**
     * 响应代码
     */
    private String code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 请求ID（用于链路追踪）
     */
    private String requestId;
    
    /**
     * 创建成功响应
     * 
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success() {
        return Result.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message("操作成功")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建成功响应（带数据）
     * 
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message("操作成功")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建成功响应（带数据和消息）
     * 
     * @param data    响应数据
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(T data, String message) {
        return Result.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败响应
     * 
     * @param code    错误代码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 失败响应
     */
    public static <T> Result<T> error(String code, String message) {
        return Result.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败响应（带数据）
     * 
     * @param code    错误代码
     * @param message 错误消息
     * @param data    错误数据
     * @param <T>     数据类型
     * @return 失败响应
     */
    public static <T> Result<T> error(String code, String message, T data) {
        return Result.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 系统错误响应
     * 
     * @param <T> 数据类型
     * @return 系统错误响应
     */
    public static <T> Result<T> systemError() {
        return error("SYSTEM_ERROR", "系统内部错误");
    }
    
    /**
     * 系统错误响应（带消息）
     * 
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 系统错误响应
     */
    public static <T> Result<T> systemError(String message) {
        return error("SYSTEM_ERROR", message);
    }
    
    /**
     * 业务错误响应
     * 
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 业务错误响应
     */
    public static <T> Result<T> businessError(String message) {
        return error("BUSINESS_ERROR", message);
    }
    
    /**
     * 参数验证错误响应
     * 
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 参数验证错误响应
     */
    public static <T> Result<T> validationError(String message) {
        return error("VALIDATION_ERROR", message);
    }
    
    /**
     * 参数验证错误响应（带错误详情）
     * 
     * @param message 错误消息
     * @param errors  错误详情
     * @param <T>     数据类型
     * @return 参数验证错误响应
     */
    public static <T> Result<T> validationError(String message, T errors) {
        return error("VALIDATION_ERROR", message, errors);
    }
    
    /**
     * 认证失败响应
     * 
     * @param <T> 数据类型
     * @return 认证失败响应
     */
    public static <T> Result<T> unauthorized() {
        return error("UNAUTHORIZED", "未授权访问");
    }
    
    /**
     * 认证失败响应（带消息）
     * 
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 认证失败响应
     */
    public static <T> Result<T> unauthorized(String message) {
        return error("UNAUTHORIZED", message);
    }
    
    /**
     * 权限不足响应
     * 
     * @param <T> 数据类型
     * @return 权限不足响应
     */
    public static <T> Result<T> forbidden() {
        return error("FORBIDDEN", "权限不足");
    }
    
    /**
     * 权限不足响应（带消息）
     * 
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 权限不足响应
     */
    public static <T> Result<T> forbidden(String message) {
        return error("FORBIDDEN", message);
    }
    
    /**
     * 资源未找到响应
     * 
     * @param <T> 数据类型
     * @return 资源未找到响应
     */
    public static <T> Result<T> notFound() {
        return error("NOT_FOUND", "资源未找到");
    }
    
    /**
     * 资源未找到响应（带消息）
     * 
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 资源未找到响应
     */
    public static <T> Result<T> notFound(String message) {
        return error("NOT_FOUND", message);
    }
    
    /**
     * 设置请求ID
     * 
     * @param requestId 请求ID
     * @return 当前实例
     */
    public Result<T> withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
}
