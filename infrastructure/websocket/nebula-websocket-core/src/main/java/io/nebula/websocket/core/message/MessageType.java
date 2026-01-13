package io.nebula.websocket.core.message;

/**
 * 消息类型常量
 */
public final class MessageType {

    private MessageType() {
    }

    /**
     * 数据消息
     */
    public static final String DATA = "data";

    /**
     * 通知消息
     */
    public static final String NOTIFICATION = "notification";

    /**
     * 心跳消息
     */
    public static final String HEARTBEAT = "heartbeat";

    /**
     * 错误消息
     */
    public static final String ERROR = "error";

    /**
     * 系统消息
     */
    public static final String SYSTEM = "system";

    /**
     * 连接成功
     */
    public static final String CONNECTED = "connected";

    /**
     * 订阅确认
     */
    public static final String SUBSCRIBED = "subscribed";

    /**
     * 取消订阅确认
     */
    public static final String UNSUBSCRIBED = "unsubscribed";

    /**
     * 广播消息
     */
    public static final String BROADCAST = "broadcast";
}

