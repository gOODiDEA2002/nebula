package io.nebula.messaging.core.handler;

import io.nebula.messaging.core.message.Message;

/**
 * 消息处理器接口
 * 
 * @param <T> 消息载荷类型
 */
@FunctionalInterface
public interface MessageHandler<T> {
    
    /**
     * 处理消息
     * 
     * @param message 消息对象
     * @return 处理结果
     */
    MessageHandleResult handle(Message<T> message);
    
    /**
     * 消息处理结果
     */
    enum MessageHandleResult {
        /**
         * 处理成功
         */
        SUCCESS,
        
        /**
         * 处理失败，需要重试
         */
        RETRY,
        
        /**
         * 处理失败，不需要重试
         */
        FAIL,
        
        /**
         * 暂停处理，稍后重试
         */
        SUSPEND
    }
}
