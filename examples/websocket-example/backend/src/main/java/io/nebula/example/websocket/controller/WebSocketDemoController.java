package io.nebula.example.websocket.controller;

import io.nebula.core.common.result.Result;
import io.nebula.example.websocket.entity.dto.SendMessageDto;
import io.nebula.websocket.core.WebSocketMessageService;
import io.nebula.websocket.core.message.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * WebSocket 演示 REST 控制器
 * 通过 HTTP 接口向 WebSocket 推送消息
 */
@Slf4j
@RestController
@RequestMapping("/ws-api")
@RequiredArgsConstructor
public class WebSocketDemoController {

    private final WebSocketMessageService messageService;

    /**
     * 获取在线状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        return Result.success(Map.of(
                "onlineSessions", messageService.getOnlineSessionCount(),
                "onlineUsers", messageService.getOnlineUserCount()
        ));
    }

    /**
     * 广播消息
     */
    @PostMapping("/broadcast")
    public Result<Map<String, Object>> broadcast(@RequestBody SendMessageDto dto) {
        WebSocketMessage<Map<String, String>> message = WebSocketMessage.of(
                dto.getType() != null ? dto.getType() : "notification",
                Map.of("content", dto.getContent())
        );

        int sent = messageService.broadcast(message);
        return Result.success(Map.of("sentTo", sent));
    }

    /**
     * 向指定用户发送消息
     */
    @PostMapping("/send-to-user")
    public Result<Map<String, Object>> sendToUser(@RequestBody SendMessageDto dto) {
        if (dto.getTargetUserId() == null) {
            return Result.error("400", "targetUserId 不能为空");
        }

        WebSocketMessage<Map<String, String>> message = WebSocketMessage.of(
                dto.getType() != null ? dto.getType() : "notification",
                Map.of("content", dto.getContent())
        );

        int sent = messageService.sendToUser(dto.getTargetUserId(), message);
        return Result.success(Map.of(
                "targetUserId", dto.getTargetUserId(),
                "sentTo", sent
        ));
    }

    /**
     * 向指定会话发送消息
     */
    @PostMapping("/send-to-session")
    public Result<Map<String, Object>> sendToSession(@RequestBody SendMessageDto dto) {
        if (dto.getTargetSessionId() == null) {
            return Result.error("400", "targetSessionId 不能为空");
        }

        WebSocketMessage<Map<String, String>> message = WebSocketMessage.of(
                dto.getType() != null ? dto.getType() : "notification",
                Map.of("content", dto.getContent())
        );

        boolean sent = messageService.sendToSession(dto.getTargetSessionId(), message);
        return Result.success(Map.of(
                "targetSessionId", dto.getTargetSessionId(),
                "sent", sent
        ));
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/online/{userId}")
    public Result<Map<String, Object>> isUserOnline(@PathVariable String userId) {
        return Result.success(Map.of(
                "userId", userId,
                "online", messageService.isUserOnline(userId)
        ));
    }
}
