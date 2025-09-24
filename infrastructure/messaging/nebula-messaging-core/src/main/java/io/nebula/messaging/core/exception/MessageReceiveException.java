package io.nebula.messaging.core.exception;

/**
 * 消息接收异常
 * 当消息接收或消费失败时抛出
 */
public class MessageReceiveException extends MessagingException {
    
    private static final long serialVersionUID = 1L;
    
    private final String topic;
    private final String queue;
    private final String messageId;
    
    public MessageReceiveException(String message) {
        super(MESSAGE_CONSUME_ERROR, message);
        this.topic = null;
        this.queue = null;
        this.messageId = null;
    }
    
    public MessageReceiveException(String message, Throwable cause) {
        super(MESSAGE_CONSUME_ERROR, message, cause);
        this.topic = null;
        this.queue = null;
        this.messageId = null;
    }
    
    public MessageReceiveException(String topic, String message) {
        super(MESSAGE_CONSUME_ERROR, String.format("从主题 %s 接收消息失败: %s", topic, message));
        this.topic = topic;
        this.queue = null;
        this.messageId = null;
    }
    
    public MessageReceiveException(String topic, String queue, String message) {
        super(MESSAGE_CONSUME_ERROR, String.format("从主题 %s 队列 %s 接收消息失败: %s", topic, queue, message));
        this.topic = topic;
        this.queue = queue;
        this.messageId = null;
    }
    
    public MessageReceiveException(String topic, String queue, String messageId, String message, Throwable cause) {
        super(MESSAGE_CONSUME_ERROR, String.format("接收消息失败: topic=%s, queue=%s, messageId=%s, error=%s", 
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
