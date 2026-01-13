package io.nebula.websocket.core;

import io.nebula.websocket.core.message.WebSocketMessage;
import io.nebula.websocket.core.session.WebSocketSession;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * WebSocket 消息服务接口
 * <p>
 * 提供消息发送的统一 API，支持单发、群发、广播等多种模式。
 * </p>
 */
public interface WebSocketMessageService {

    /**
     * 向指定会话发送消息
     *
     * @param sessionId 会话 ID
     * @param message   消息对象
     * @param <T>       载荷类型
     * @return 是否发送成功
     */
    <T> boolean sendToSession(String sessionId, WebSocketMessage<T> message);

    /**
     * 向指定用户发送消息
     * <p>
     * 如果用户有多个连接，会发送到所有连接。
     * </p>
     *
     * @param userId  用户 ID
     * @param message 消息对象
     * @param <T>     载荷类型
     * @return 发送成功的会话数
     */
    <T> int sendToUser(String userId, WebSocketMessage<T> message);

    /**
     * 向多个用户发送消息
     *
     * @param userIds 用户 ID 集合
     * @param message 消息对象
     * @param <T>     载荷类型
     * @return 发送成功的会话数
     */
    <T> int sendToUsers(Collection<String> userIds, WebSocketMessage<T> message);

    /**
     * 向所有在线用户广播消息
     *
     * @param message 消息对象
     * @param <T>     载荷类型
     * @return 发送成功的会话数
     */
    <T> int broadcast(WebSocketMessage<T> message);

    /**
     * 向符合条件的会话发送消息
     *
     * @param predicate 会话过滤条件
     * @param message   消息对象
     * @param <T>       载荷类型
     * @return 发送成功的会话数
     */
    <T> int sendToSessions(Predicate<WebSocketSession> predicate, WebSocketMessage<T> message);

    /**
     * 向指定主题订阅者发送消息
     * <p>
     * 用于发布/订阅模式。
     * </p>
     *
     * @param topic   主题
     * @param message 消息对象
     * @param <T>     载荷类型
     * @return 发送成功的会话数
     */
    <T> int sendToTopic(String topic, WebSocketMessage<T> message);

    /**
     * 关闭指定会话
     *
     * @param sessionId 会话 ID
     * @param code      关闭代码
     * @param reason    关闭原因
     * @return 是否成功
     */
    boolean closeSession(String sessionId, int code, String reason);

    /**
     * 关闭指定用户的所有会话
     *
     * @param userId 用户 ID
     * @param code   关闭代码
     * @param reason 关闭原因
     * @return 关闭的会话数
     */
    int closeUserSessions(String userId, int code, String reason);

    /**
     * 获取在线会话数
     *
     * @return 会话数
     */
    int getOnlineSessionCount();

    /**
     * 获取在线用户数
     *
     * @return 用户数
     */
    int getOnlineUserCount();

    /**
     * 检查用户是否在线
     *
     * @param userId 用户 ID
     * @return 是否在线
     */
    boolean isUserOnline(String userId);
}

