package io.nebula.websocket.netty;

import io.nebula.websocket.core.WebSocketMessageService;
import io.nebula.websocket.core.cluster.ClusterMessageBroker;
import io.nebula.websocket.core.message.WebSocketMessage;
import io.nebula.websocket.core.session.SessionRegistry;
import io.nebula.websocket.core.session.WebSocketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * Netty WebSocket 消息服务实现
 */
@Slf4j
@RequiredArgsConstructor
public class NettyWebSocketMessageService implements WebSocketMessageService {

    private final SessionRegistry sessionRegistry;
    private final ClusterMessageBroker clusterMessageBroker;

    @Override
    public <T> boolean sendToSession(String sessionId, WebSocketMessage<T> message) {
        return sessionRegistry.getSession(sessionId)
                .map(session -> {
                    try {
                        session.send(message);
                        return true;
                    } catch (Exception e) {
                        log.error("发送消息失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
                        return false;
                    }
                })
                .orElseGet(() -> {
                    log.warn("会话不存在: sessionId={}", sessionId);
                    return false;
                });
    }

    @Override
    public <T> int sendToUser(String userId, WebSocketMessage<T> message) {
        Collection<WebSocketSession> sessions = sessionRegistry.getSessionsByUserId(userId);
        int successCount = 0;

        for (WebSocketSession session : sessions) {
            try {
                session.send(message);
                successCount++;
            } catch (Exception e) {
                log.error("发送消息失败: userId={}, sessionId={}, error={}",
                        userId, session.getId(), e.getMessage(), e);
            }
        }

        // 如果启用了集群模式，同时通过集群广播
        if (clusterMessageBroker != null && clusterMessageBroker.isAvailable()) {
            clusterMessageBroker.publishToUser(userId, message);
        }

        return successCount;
    }

    @Override
    public <T> int sendToUsers(Collection<String> userIds, WebSocketMessage<T> message) {
        int totalSuccess = 0;
        for (String userId : userIds) {
            totalSuccess += sendToUser(userId, message);
        }
        return totalSuccess;
    }

    @Override
    public <T> int broadcast(WebSocketMessage<T> message) {
        Collection<WebSocketSession> sessions = sessionRegistry.getAllSessions();
        int successCount = 0;

        for (WebSocketSession session : sessions) {
            try {
                session.send(message);
                successCount++;
            } catch (Exception e) {
                log.error("广播消息失败: sessionId={}, error={}", session.getId(), e.getMessage(), e);
            }
        }

        // 如果启用了集群模式，同时通过集群广播
        if (clusterMessageBroker != null && clusterMessageBroker.isAvailable()) {
            clusterMessageBroker.publishBroadcast(message);
        }

        log.debug("广播消息: 成功 {}/{} 个会话", successCount, sessions.size());
        return successCount;
    }

    @Override
    public <T> int sendToSessions(Predicate<WebSocketSession> predicate, WebSocketMessage<T> message) {
        Collection<WebSocketSession> sessions = sessionRegistry.findSessions(predicate);
        int successCount = 0;

        for (WebSocketSession session : sessions) {
            try {
                session.send(message);
                successCount++;
            } catch (Exception e) {
                log.error("发送消息失败: sessionId={}, error={}", session.getId(), e.getMessage(), e);
            }
        }

        return successCount;
    }

    @Override
    public <T> int sendToTopic(String topic, WebSocketMessage<T> message) {
        Collection<WebSocketSession> sessions = sessionRegistry.findSessions(session -> {
            Object topics = session.getAttribute("subscribed_topics");
            if (topics instanceof Collection) {
                return ((Collection<?>) topics).contains(topic);
            }
            return false;
        });

        int successCount = 0;
        for (WebSocketSession session : sessions) {
            try {
                session.send(message);
                successCount++;
            } catch (Exception e) {
                log.error("发送主题消息失败: topic={}, sessionId={}, error={}",
                        topic, session.getId(), e.getMessage(), e);
            }
        }

        // 如果启用了集群模式，同时通过集群广播
        if (clusterMessageBroker != null && clusterMessageBroker.isAvailable()) {
            clusterMessageBroker.publishToTopic(topic, message);
        }

        return successCount;
    }

    @Override
    public boolean closeSession(String sessionId, int code, String reason) {
        return sessionRegistry.getSession(sessionId)
                .map(session -> {
                    try {
                        session.close(code, reason);
                        return true;
                    } catch (Exception e) {
                        log.error("关闭会话失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
                        return false;
                    }
                })
                .orElse(false);
    }

    @Override
    public int closeUserSessions(String userId, int code, String reason) {
        Collection<WebSocketSession> sessions = sessionRegistry.getSessionsByUserId(userId);
        int count = 0;
        for (WebSocketSession session : sessions) {
            try {
                session.close(code, reason);
                count++;
            } catch (Exception e) {
                log.error("关闭用户会话失败: userId={}, sessionId={}, error={}",
                        userId, session.getId(), e.getMessage(), e);
            }
        }
        return count;
    }

    @Override
    public int getOnlineSessionCount() {
        return sessionRegistry.getSessionCount();
    }

    @Override
    public int getOnlineUserCount() {
        return sessionRegistry.getUserCount();
    }

    @Override
    public boolean isUserOnline(String userId) {
        return sessionRegistry.isUserOnline(userId);
    }
}

