package io.nebula.websocket.core.exception;

/**
 * 消息发送异常
 */
public class MessageSendException extends WebSocketException {

    private static final long serialVersionUID = 1L;

    private final String sessionId;

    public MessageSendException(String sessionId, String message) {
        super("MESSAGE_SEND_FAILED", message);
        this.sessionId = sessionId;
    }

    public MessageSendException(String sessionId, String message, Throwable cause) {
        super("MESSAGE_SEND_FAILED", message, cause);
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}

