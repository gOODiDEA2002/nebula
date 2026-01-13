package io.nebula.websocket.core.session;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 会话注册表接口
 * <p>
 * 管理所有 WebSocket 会话的注册、查找和移除。
 * </p>
 */
public interface SessionRegistry {

    /**
     * 注册会话
     *
     * @param session 会话对象
     */
    void register(WebSocketSession session);

    /**
     * 注销会话
     *
     * @param sessionId 会话 ID
     * @return 被移除的会话，不存在则返回 empty
     */
    Optional<WebSocketSession> unregister(String sessionId);

    /**
     * 根据会话 ID 获取会话
     *
     * @param sessionId 会话 ID
     * @return 会话对象
     */
    Optional<WebSocketSession> getSession(String sessionId);

    /**
     * 根据用户 ID 获取所有会话
     * <p>
     * 一个用户可能有多个连接（多设备）
     * </p>
     *
     * @param userId 用户 ID
     * @return 会话集合
     */
    Collection<WebSocketSession> getSessionsByUserId(String userId);

    /**
     * 获取所有会话
     *
     * @return 所有会话
     */
    Collection<WebSocketSession> getAllSessions();

    /**
     * 根据条件查找会话
     *
     * @param predicate 过滤条件
     * @return 匹配的会话
     */
    Collection<WebSocketSession> findSessions(Predicate<WebSocketSession> predicate);

    /**
     * 获取会话数量
     *
     * @return 会话数量
     */
    int getSessionCount();

    /**
     * 获取用户数量（去重）
     *
     * @return 用户数量
     */
    int getUserCount();

    /**
     * 检查用户是否在线
     *
     * @param userId 用户 ID
     * @return 是否在线
     */
    boolean isUserOnline(String userId);

    /**
     * 清理无效会话
     *
     * @return 清理的会话数量
     */
    int cleanupInactiveSessions();
}

