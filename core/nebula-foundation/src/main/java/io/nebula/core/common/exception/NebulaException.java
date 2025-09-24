package io.nebula.core.common.exception;

import lombok.Getter;

/**
 * Nebula框架基础异常类
 * 所有框架异常的基类，提供错误码和参数支持
 */
@Getter
public abstract class NebulaException extends RuntimeException {
    
    /**
     * 错误代码
     */
    private final String errorCode;
    
    /**
     * 错误参数
     */
    private final Object[] args;
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param args      错误参数
     */
    protected NebulaException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param cause     原因
     * @param args      错误参数
     */
    protected NebulaException(String errorCode, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = args;
    }
    
    /**
     * 获取格式化的错误消息
     * 
     * @return 格式化的错误消息
     */
    public String getFormattedMessage() {
        if (args == null || args.length == 0) {
            return getMessage();
        }
        try {
            return String.format(getMessage(), args);
        } catch (Exception e) {
            return getMessage();
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s[%s]: %s", 
                getClass().getSimpleName(), 
                errorCode, 
                getFormattedMessage());
    }
}
