package io.nebula.messaging.core.exception;

/**
 * 消息发送异常
 * 当消息发送失败时抛出
 */
public class MessageSendException extends MessagingException {
    
    private static final long serialVersionUID = 1L;
    
    private final String topic;
    private final String queue;
    private final String messageId;
    
    public MessageSendException(String message) {
        super(MESSAGE_SEND_ERROR, message);
        this.topic = null;
        this.queue = null;
        this.messageId = null;
    }
    
    public MessageSendException(String message, Throwable cause) {
        super(MESSAGE_SEND_ERROR, message, cause);
        this.topic = null;
        this.queue = null;
        this.messageId = null;
    }
    
    public MessageSendException(String topic, String message) {
        super(MESSAGE_SEND_ERROR, String.format("发送消息到主题 %s 失败: %s", topic, message));
        this.topic = topic;
        this.queue = null;
        this.messageId = null;
    }
    
    public MessageSendException(String topic, String queue, String message) {
        super(MESSAGE_SEND_ERROR, String.format("发送消息到主题 %s 队列 %s 失败: %s", topic, queue, message));
        this.topic = topic;
        this.queue = queue;
        this.messageId = null;
    }
    
    public MessageSendException(String topic, String queue, String messageId, String message, Throwable cause) {
        super(MESSAGE_SEND_ERROR, String.format("发送消息失败: topic=%s, queue=%s, messageId=%s, error=%s", 
                topic, queue, messageId, message), cause);
        this.topic = topic;
        this.queue = queue;
        this.messageId = messageId;
    }
    
    /**
     * 获取主题
     * 
     * @return 主题
     */
    public String getTopic() {
        return topic;
    }
    
    /**
     * 获取队列
     * 
     * @return 队列
     */
    public String getQueue() {
        return queue;
    }
    
    /**
     * 获取消息ID
     * 
     * @return 消息ID
     */
    public String getMessageId() {
        return messageId;
    }
}
