package io.nebula.websocket.core.exception;

/**
 * WebSocket 异常基类
 */
public class WebSocketException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误代码
     */
    private final String errorCode;

    public WebSocketException(String message) {
        super(message);
        this.errorCode = "WEBSOCKET_ERROR";
    }

    public WebSocketException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WebSocketException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "WEBSOCKET_ERROR";
    }

    public WebSocketException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

