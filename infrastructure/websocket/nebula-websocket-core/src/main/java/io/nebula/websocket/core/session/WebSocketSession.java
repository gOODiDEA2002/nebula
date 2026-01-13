package io.nebula.websocket.core.session;

import io.nebula.websocket.core.message.WebSocketMessage;

import java.io.Closeable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * WebSocket 会话接口
 * <p>
 * 抽象 WebSocket 连接，提供统一的消息发送和会话管理能力。
 * 具体实现由 spring/netty 模块提供。
 * </p>
 */
public interface WebSocketSession extends Closeable {

    /**
     * 获取会话 ID
     *
     * @return 会话唯一标识
     */
    String getId();

    /**
     * 获取用户 ID
     *
     * @return 用户标识，未认证时可能为 null
     */
    String getUserId();

    /**
     * 设置用户 ID
     *
     * @param userId 用户标识
     */
    void setUserId(String userId);

    /**
     * 获取会话属性
     *
     * @param key 属性键
     * @param <T> 属性值类型
     * @return 属性值
     */
    <T> T getAttribute(String key);

    /**
     * 设置会话属性
     *
     * @param key   属性键
     * @param value 属性值
     */
    void setAttribute(String key, Object value);

    /**
     * 移除会话属性
     *
     * @param key 属性键
     */
    void removeAttribute(String key);

    /**
     * 获取所有会话属性
     *
     * @return 属性映射
     */
    Map<String, Object> getAttributes();

    /**
     * 检查会话是否打开
     *
     * @return 是否打开
     */
    boolean isOpen();

    /**
     * 发送文本消息
     *
     * @param text 文本内容
     */
    void sendText(String text);

    /**
     * 发送二进制消息
     *
     * @param data 二进制数据
     */
    void sendBinary(byte[] data);

    /**
     * 发送消息对象（会序列化为 JSON）
     *
     * @param message 消息对象
     * @param <T>     消息载荷类型
     */
    <T> void send(WebSocketMessage<T> message);

    /**
     * 异步发送消息
     *
     * @param message 消息对象
     * @param <T>     消息载荷类型
     * @return 发送结果
     */
    <T> CompletableFuture<Void> sendAsync(WebSocketMessage<T> message);

    /**
     * 关闭会话
     */
    @Override
    void close();

    /**
     * 关闭会话（带关闭原因）
     *
     * @param code   关闭代码
     * @param reason 关闭原因
     */
    void close(int code, String reason);

    /**
     * 获取远程地址
     *
     * @return 客户端地址
     */
    String getRemoteAddress();

    /**
     * 获取连接时间
     *
     * @return 连接建立时间
     */
    LocalDateTime getConnectTime();

    /**
     * 获取最后活跃时间
     *
     * @return 最后活跃时间
     */
    LocalDateTime getLastActiveTime();

    /**
     * 更新最后活跃时间
     */
    void updateLastActiveTime();

    /**
     * 获取原生会话对象
     * <p>
     * 返回底层实现的原生会话对象，便于高级用法。
     * </p>
     *
     * @param <T> 原生会话类型
     * @return 原生会话对象
     */
    <T> T getNativeSession();
}

