package io.nebula.core.common.exception;

/**
 * 业务异常
 * 用于表示业务逻辑错误，通常不需要重试
 */
public class BusinessException extends NebulaException {
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param args      错误参数
     */
    public BusinessException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param cause     原因
     * @param args      错误参数
     */
    public BusinessException(String errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }
    
    /**
     * 创建业务异常的便捷方法
     * 
     * @param message 错误消息
     * @return 业务异常实例
     */
    public static BusinessException of(String message) {
        return new BusinessException("BUSINESS_ERROR", message);
    }
    
    /**
     * 创建业务异常的便捷方法（带参数）
     * 
     * @param message 错误消息模板
     * @param args    参数
     * @return 业务异常实例
     */
    public static BusinessException of(String message, Object... args) {
        return new BusinessException("BUSINESS_ERROR", message, args);
    }
    
    /**
     * 创建带错误代码的业务异常
     * 
     * @param errorCode 错误代码
     * @param message   错误消息
     * @return 业务异常实例
     */
    public static BusinessException withCode(String errorCode, String message) {
        return new BusinessException(errorCode, message);
    }
    
    /**
     * 创建带错误代码的业务异常（带参数）
     * 
     * @param errorCode 错误代码
     * @param message   错误消息模板
     * @param args      参数
     * @return 业务异常实例
     */
    public static BusinessException withCode(String errorCode, String message, Object... args) {
        return new BusinessException(errorCode, message, args);
    }
}
