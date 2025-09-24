package io.nebula.messaging.core.exception;

import io.nebula.core.common.exception.NebulaException;

/**
 * 消息传递基础异常
 * 所有消息传递相关异常的基类
 */
public class MessagingException extends NebulaException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息传递错误代码
     */
    public static final String MESSAGING_ERROR = "MESSAGING_ERROR";
    public static final String MESSAGE_SEND_ERROR = "MESSAGE_SEND_ERROR";
    public static final String MESSAGE_CONSUME_ERROR = "MESSAGE_CONSUME_ERROR";
    public static final String MESSAGE_ROUTING_ERROR = "MESSAGE_ROUTING_ERROR";
    public static final String MESSAGE_SERIALIZATION_ERROR = "MESSAGE_SERIALIZATION_ERROR";
    public static final String MESSAGE_CONNECTION_ERROR = "MESSAGE_CONNECTION_ERROR";
    public static final String MESSAGE_TIMEOUT_ERROR = "MESSAGE_TIMEOUT_ERROR";
    public static final String MESSAGE_CONFIGURATION_ERROR = "MESSAGE_CONFIGURATION_ERROR";
    
    public MessagingException(String message) {
        super(MESSAGING_ERROR, message);
    }
    
    public MessagingException(String message, Throwable cause) {
        super(MESSAGING_ERROR, message, cause);
    }
    
    public MessagingException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public MessagingException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    public MessagingException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }
    
    public MessagingException(String errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }
}
