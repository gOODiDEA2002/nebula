package io.nebula.ai.core.exception;

/**
 * 聊天服务异常
 */
public class ChatException extends AIException {

    public ChatException(String message) {
        super(message, "CHAT_ERROR");
    }

    public ChatException(String message, Throwable cause) {
        super(message, "CHAT_ERROR", cause);
    }

    public ChatException(String message, String errorCode) {
        super(message, errorCode);
    }

    public ChatException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
