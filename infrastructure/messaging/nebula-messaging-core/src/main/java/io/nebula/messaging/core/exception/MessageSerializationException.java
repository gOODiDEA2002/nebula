package io.nebula.messaging.core.exception;

/**
 * 消息序列化异常
 * 当消息序列化或反序列化失败时抛出
 */
public class MessageSerializationException extends MessagingException {
    
    private static final long serialVersionUID = 1L;
    
    private final String messageType;
    private final String serializerType;
    
    public MessageSerializationException(String message) {
        super(MESSAGE_SERIALIZATION_ERROR, message);
        this.messageType = null;
        this.serializerType = null;
    }
    
    public MessageSerializationException(String message, Throwable cause) {
        super(MESSAGE_SERIALIZATION_ERROR, message, cause);
        this.messageType = null;
        this.serializerType = null;
    }
    
    public MessageSerializationException(String messageType, String serializerType, String message) {
        super(MESSAGE_SERIALIZATION_ERROR, String.format("序列化消息失败: messageType=%s, serializerType=%s, error=%s", 
                messageType, serializerType, message));
        this.messageType = messageType;
        this.serializerType = serializerType;
    }
    
    public MessageSerializationException(String messageType, String serializerType, String message, Throwable cause) {
        super(MESSAGE_SERIALIZATION_ERROR, String.format("序列化消息失败: messageType=%s, serializerType=%s, error=%s", 
                messageType, serializerType, message), cause);
        this.messageType = messageType;
        this.serializerType = serializerType;
    }
    
    /**
     * 获取消息类型
     * 
     * @return 消息类型
     */
    public String getMessageType() {
        return messageType;
    }
    
    /**
     * 获取序列化器类型
     * 
     * @return 序列化器类型
     */
    public String getSerializerType() {
        return serializerType;
    }
}
