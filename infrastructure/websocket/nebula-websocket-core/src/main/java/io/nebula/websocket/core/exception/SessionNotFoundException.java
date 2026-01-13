package io.nebula.websocket.core.exception;

/**
 * 会话未找到异常
 */
public class SessionNotFoundException extends WebSocketException {

    private static final long serialVersionUID = 1L;

    private final String sessionId;

    public SessionNotFoundException(String sessionId) {
        super("SESSION_NOT_FOUND", "会话不存在: " + sessionId);
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}

