package io.nebula.websocket.core.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 消息对象
 * <p>
 * 统一的消息格式，包含类型、载荷、元数据等信息。
 * </p>
 *
 * @param <T> 消息载荷类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage<T> {

    /**
     * 消息 ID
     */
    private String id;

    /**
     * 消息类型
     * <p>
     * 用于客户端区分不同类型的消息，如：
     * - notification: 通知消息
     * - data: 数据消息
     * - heartbeat: 心跳消息
     * - error: 错误消息
     * </p>
     */
    private String type;

    /**
     * 消息主题（可选）
     * <p>
     * 用于订阅/发布模式
     * </p>
     */
    private String topic;

    /**
     * 消息载荷
     */
    private T payload;

    /**
     * 消息头
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    /**
     * 发送者 ID
     */
    private String senderId;

    /**
     * 接收者 ID（可选）
     * <p>
     * 用于点对点消息
     * </p>
     */
    private String receiverId;

    /**
     * 创建时间
     */
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * 创建文本消息
     *
     * @param type    消息类型
     * @param payload 消息载荷
     * @param <T>     载荷类型
     * @return 消息对象
     */
    public static <T> WebSocketMessage<T> of(String type, T payload) {
        return WebSocketMessage.<T>builder()
                .type(type)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建带主题的消息
     *
     * @param type    消息类型
     * @param topic   消息主题
     * @param payload 消息载荷
     * @param <T>     载荷类型
     * @return 消息对象
     */
    public static <T> WebSocketMessage<T> of(String type, String topic, T payload) {
        return WebSocketMessage.<T>builder()
                .type(type)
                .topic(topic)
                .payload(payload)
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建心跳消息
     *
     * @return 心跳消息
     */
    public static WebSocketMessage<String> heartbeat() {
        return WebSocketMessage.<String>builder()
                .type(MessageType.HEARTBEAT)
                .payload("pong")
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建错误消息
     *
     * @param errorCode    错误代码
     * @param errorMessage 错误信息
     * @return 错误消息
     */
    public static WebSocketMessage<Map<String, String>> error(String errorCode, String errorMessage) {
        Map<String, String> error = new HashMap<>();
        error.put("code", errorCode);
        error.put("message", errorMessage);
        return WebSocketMessage.<Map<String, String>>builder()
                .type(MessageType.ERROR)
                .payload(error)
                .createTime(LocalDateTime.now())
                .build();
    }
}

