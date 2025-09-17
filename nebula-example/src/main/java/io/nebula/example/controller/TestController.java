package io.nebula.example.controller;

import io.nebula.core.common.result.Result;
import io.nebula.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 测试控制器 - 验证Nebula Web统一响应格式
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController extends BaseController {
    
    /**
     * 测试成功响应
     */
    @GetMapping("/success")
    public Result<Map<String, Object>> testSuccess() {
        log.info("测试成功响应");
        Map<String, Object> data = Map.of(
            "message", "Nebula Web统一响应格式测试成功",
            "timestamp", LocalDateTime.now(),
            "framework", "Nebula Web 2.0.0"
        );
        return success(data);
    }
    
    /**
     * 测试错误响应
     */
    @GetMapping("/error")
    public Result<Void> testError() {
        log.info("测试错误响应");
        return error("这是一个测试错误消息");
    }
    
    /**
     * 测试带错误码的错误响应
     */
    @GetMapping("/error-with-code")
    public Result<Void> testErrorWithCode() {
        log.info("测试带错误码的错误响应");
        return error("TEST_ERROR", "测试错误，错误码：TEST_ERROR");
    }
    
    /**
     * 测试参数验证（模拟用户不存在场景）
     */
    @GetMapping("/user/{id}")
    public Result<Map<String, Object>> testUserNotFound(@PathVariable Long id) {
        log.info("测试用户查找，ID: {}", id);
        
        if (id == 404) {
            return error("USER_NOT_FOUND", "用户不存在，ID: " + id);
        }
        
        Map<String, Object> user = Map.of(
            "id", id,
            "username", "test_user_" + id,
            "message", "这是模拟的用户数据"
        );
        return success(user);
    }
    
    @Override
    protected Long getCurrentUserId() {
        return 1L; // 测试用用户ID
    }
    
    @Override
    protected String getCurrentUsername() {
        return "test_user"; // 测试用用户名
    }
}
