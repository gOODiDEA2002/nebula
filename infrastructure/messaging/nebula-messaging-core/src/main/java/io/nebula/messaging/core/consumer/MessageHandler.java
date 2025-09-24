package io.nebula.messaging.core.consumer;

import io.nebula.messaging.core.message.Message;

/**
 * 消息处理器接口
 * 用于处理接收到的消息
 *
 * @param <T> 消息体类型
 * @author nebula
 */
public interface MessageHandler<T> {

    /**
     * 处理消息
     *
     * @param message 接收到的消息
     */
    void handle(Message<T> message);

    /**
     * 获取支持的消息类型
     *
     * @return 消息类型
     */
    Class<T> getMessageType();
}
