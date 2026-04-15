package io.nebula.example.websocket.entity.dto;

import lombok.Data;

/**
 * 通过 REST 发送 WebSocket 消息的 DTO
 */
@Data
public class SendMessageDto {
    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 目标用户ID（可选，为空则广播）
     */
    private String targetUserId;

    /**
     * 目标会话ID（可选）
     */
    private String targetSessionId;
}
