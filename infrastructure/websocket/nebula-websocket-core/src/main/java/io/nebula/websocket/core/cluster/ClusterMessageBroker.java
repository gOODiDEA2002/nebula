package io.nebula.websocket.core.cluster;

import io.nebula.websocket.core.message.WebSocketMessage;

import java.util.function.Consumer;

/**
 * 集群消息代理接口
 * <p>
 * 用于在集群环境中同步 WebSocket 消息。
 * 当应用部署多个实例时，需要通过消息代理将消息同步到其他实例。
 * </p>
 */
public interface ClusterMessageBroker {

    /**
     * 发布消息到集群
     * <p>
     * 将消息广播到所有集群节点。
     * </p>
     *
     * @param channel 频道名称
     * @param message 消息对象
     * @param <T>     载荷类型
     */
    <T> void publish(String channel, WebSocketMessage<T> message);

    /**
     * 订阅集群消息
     * <p>
     * 接收其他节点发布的消息。
     * </p>
     *
     * @param channel 频道名称
     * @param handler 消息处理器
     * @param <T>     载荷类型
     */
    <T> void subscribe(String channel, Consumer<WebSocketMessage<T>> handler);

    /**
     * 取消订阅
     *
     * @param channel 频道名称
     */
    void unsubscribe(String channel);

    /**
     * 发布用户消息
     * <p>
     * 将消息发送给指定用户的所有连接（可能在其他节点）。
     * </p>
     *
     * @param userId  用户 ID
     * @param message 消息对象
     * @param <T>     载荷类型
     */
    <T> void publishToUser(String userId, WebSocketMessage<T> message);

    /**
     * 发布广播消息
     * <p>
     * 将消息广播给所有连接的用户。
     * </p>
     *
     * @param message 消息对象
     * @param <T>     载荷类型
     */
    <T> void publishBroadcast(WebSocketMessage<T> message);

    /**
     * 发布主题消息
     * <p>
     * 将消息发送给订阅了指定主题的用户。
     * </p>
     *
     * @param topic   主题
     * @param message 消息对象
     * @param <T>     载荷类型
     */
    <T> void publishToTopic(String topic, WebSocketMessage<T> message);

    /**
     * 启动消息代理
     */
    void start();

    /**
     * 停止消息代理
     */
    void stop();

    /**
     * 检查是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();
}

