package io.nebula.messaging.core.exception;

/**
 * 消息连接异常
 * 当消息中间件连接失败时抛出
 */
public class MessageConnectionException extends MessagingException {
    
    private static final long serialVersionUID = 1L;
    
    private final String host;
    private final Integer port;
    private final String virtualHost;
    
    public MessageConnectionException(String message) {
        super(MESSAGE_CONNECTION_ERROR, message);
        this.host = null;
        this.port = null;
        this.virtualHost = null;
    }
    
    public MessageConnectionException(String message, Throwable cause) {
        super(MESSAGE_CONNECTION_ERROR, message, cause);
        this.host = null;
        this.port = null;
        this.virtualHost = null;
    }
    
    public MessageConnectionException(String host, Integer port, String message) {
        super(MESSAGE_CONNECTION_ERROR, String.format("连接到消息中间件失败: %s:%s, error=%s", host, port, message));
        this.host = host;
        this.port = port;
        this.virtualHost = null;
    }
    
    public MessageConnectionException(String host, Integer port, String virtualHost, String message, Throwable cause) {
        super(MESSAGE_CONNECTION_ERROR, String.format("连接到消息中间件失败: %s:%s/%s, error=%s", 
                host, port, virtualHost, message), cause);
        this.host = host;
        this.port = port;
        this.virtualHost = virtualHost;
    }
    
    /**
     * 获取主机地址
     * 
     * @return 主机地址
     */
    public String getHost() {
        return host;
    }
    
    /**
     * 获取端口
     * 
     * @return 端口
     */
    public Integer getPort() {
        return port;
    }
    
    /**
     * 获取虚拟主机
     * 
     * @return 虚拟主机
     */
    public String getVirtualHost() {
        return virtualHost;
    }
}
