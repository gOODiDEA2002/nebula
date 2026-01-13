package io.nebula.websocket.core.session;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 默认会话注册表实现
 * <p>
 * 基于 ConcurrentHashMap 实现的线程安全会话注册表。
 * 适用于单机部署场景。
 * </p>
 */
@Slf4j
public class DefaultSessionRegistry implements SessionRegistry {

    /**
     * 会话存储: sessionId -> session
     */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 用户会话映射: userId -> Set<sessionId>
     */
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void register(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);

        String userId = session.getUserId();
        if (userId != null) {
            userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                    .add(sessionId);
        }

        log.debug("会话注册: sessionId={}, userId={}, 当前会话数: {}", 
                sessionId, userId, sessions.size());
    }

    @Override
    public Optional<WebSocketSession> unregister(String sessionId) {
        WebSocketSession session = sessions.remove(sessionId);
        if (session != null) {
            String userId = session.getUserId();
            if (userId != null) {
                Set<String> sessionIds = userSessions.get(userId);
                if (sessionIds != null) {
                    sessionIds.remove(sessionId);
                    if (sessionIds.isEmpty()) {
                        userSessions.remove(userId);
                    }
                }
            }
            log.debug("会话注销: sessionId={}, userId={}, 当前会话数: {}", 
                    sessionId, userId, sessions.size());
        }
        return Optional.ofNullable(session);
    }

    @Override
    public Optional<WebSocketSession> getSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public Collection<WebSocketSession> getSessionsByUserId(String userId) {
        Set<String> sessionIds = userSessions.get(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sessionIds.stream()
                .map(sessions::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<WebSocketSession> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    @Override
    public Collection<WebSocketSession> findSessions(Predicate<WebSocketSession> predicate) {
        return sessions.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @Override
    public int getSessionCount() {
        return sessions.size();
    }

    @Override
    public int getUserCount() {
        return userSessions.size();
    }

    @Override
    public boolean isUserOnline(String userId) {
        Set<String> sessionIds = userSessions.get(userId);
        return sessionIds != null && !sessionIds.isEmpty();
    }

    @Override
    public int cleanupInactiveSessions() {
        int count = 0;
        Iterator<Map.Entry<String, WebSocketSession>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, WebSocketSession> entry = iterator.next();
            WebSocketSession session = entry.getValue();
            if (!session.isOpen()) {
                iterator.remove();
                String userId = session.getUserId();
                if (userId != null) {
                    Set<String> sessionIds = userSessions.get(userId);
                    if (sessionIds != null) {
                        sessionIds.remove(entry.getKey());
                        if (sessionIds.isEmpty()) {
                            userSessions.remove(userId);
                        }
                    }
                }
                count++;
            }
        }
        if (count > 0) {
            log.info("清理无效会话: {} 个", count);
        }
        return count;
    }

    /**
     * 更新用户会话映射
     * <p>
     * 当用户 ID 发生变化时（如认证后），需要更新映射关系。
     * </p>
     *
     * @param session   会话对象
     * @param oldUserId 旧用户 ID
     * @param newUserId 新用户 ID
     */
    public void updateUserMapping(WebSocketSession session, String oldUserId, String newUserId) {
        String sessionId = session.getId();

        // 移除旧映射
        if (oldUserId != null) {
            Set<String> oldSessionIds = userSessions.get(oldUserId);
            if (oldSessionIds != null) {
                oldSessionIds.remove(sessionId);
                if (oldSessionIds.isEmpty()) {
                    userSessions.remove(oldUserId);
                }
            }
        }

        // 添加新映射
        if (newUserId != null) {
            userSessions.computeIfAbsent(newUserId, k -> ConcurrentHashMap.newKeySet())
                    .add(sessionId);
        }

        log.debug("更新用户映射: sessionId={}, {} -> {}", sessionId, oldUserId, newUserId);
    }
}

