package io.nebula.core.common.result;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Result完整单元测试
 */
class ResultFullTest {
    
    // ====================
    // 成功响应测试
    // ====================
    
    @Test
    void testSuccess() {
        Result<Void> result = Result.success();
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo("SUCCESS");
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getTimestamp()).isNotNull();
        assertThat(result.getData()).isNull();
    }
    
    @Test
    void testSuccessWithData() {
        String data = "test data";
        
        Result<String> result = Result.success(data);
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo("SUCCESS");
        assertThat(result.getData()).isEqualTo(data);
    }
    
    @Test
    void testSuccessWithMessage() {
        String data = "test data";
        String message = "自定义成功消息";
        
        Result<String> result = Result.success(data, message);
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo(message);
        assertThat(result.getData()).isEqualTo(data);
    }
    
    // ====================
    // 错误响应测试
    // ====================
    
    @Test
    void testError() {
        Result<Void> result = Result.error("ERROR_CODE", "错误消息");
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("ERROR_CODE");
        assertThat(result.getMessage()).isEqualTo("错误消息");
        assertThat(result.getTimestamp()).isNotNull();
    }
    
    @Test
    void testErrorWithData() {
        String errorData = "error details";
        
        Result<String> result = Result.error("ERROR_CODE", "错误消息", errorData);
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getData()).isEqualTo(errorData);
    }
    
    @Test
    void testSystemError() {
        Result<Void> result = Result.systemError();
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("SYSTEM_ERROR");
        assertThat(result.getMessage()).isEqualTo("系统内部错误");
    }
    
    @Test
    void testSystemErrorWithMessage() {
        Result<Void> result = Result.systemError("数据库连接失败");
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("SYSTEM_ERROR");
        assertThat(result.getMessage()).isEqualTo("数据库连接失败");
    }
    
    @Test
    void testBusinessError() {
        Result<Void> result = Result.businessError("库存不足");
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("BUSINESS_ERROR");
        assertThat(result.getMessage()).isEqualTo("库存不足");
    }
    
    @Test
    void testValidationError() {
        Result<Void> result = Result.validationError("参数验证失败");
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(result.getMessage()).isEqualTo("参数验证失败");
    }
    
    @Test
    void testValidationErrorWithErrors() {
        String errors = "field1: 不能为空, field2: 格式错误";
        
        Result<String> result = Result.validationError("参数验证失败", errors);
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getData()).isEqualTo(errors);
    }
    
    // ====================
    // 认证和权限测试
    // ====================
    
    @Test
    void testUnauthorized() {
        Result<Void> result = Result.unauthorized();
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("UNAUTHORIZED");
        assertThat(result.getMessage()).isEqualTo("未授权访问");
    }
    
    @Test
    void testUnauthorizedWithMessage() {
        Result<Void> result = Result.unauthorized("Token已过期");
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Token已过期");
    }
    
    @Test
    void testForbidden() {
        Result<Void> result = Result.forbidden();
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("FORBIDDEN");
        assertThat(result.getMessage()).isEqualTo("权限不足");
    }
    
    @Test
    void testForbiddenWithMessage() {
        Result<Void> result = Result.forbidden("需要管理员权限");
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("需要管理员权限");
    }
    
    // ====================
    // 资源未找到测试
    // ====================
    
    @Test
    void testNotFound() {
        Result<Void> result = Result.notFound();
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("NOT_FOUND");
        assertThat(result.getMessage()).isEqualTo("资源未找到");
    }
    
    @Test
    void testNotFoundWithMessage() {
        Result<Void> result = Result.notFound("用户不存在");
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("用户不存在");
    }
    
    // ====================
    // 请求ID测试
    // ====================
    
    @Test
    void testWithRequestId() {
        String requestId = "req-123456";
        
        Result<String> result = Result.success("data").withRequestId(requestId);
        
        assertThat(result.getRequestId()).isEqualTo(requestId);
    }
    
    @Test
    void testWithRequestIdChaining() {
        String requestId = "req-123456";
        
        Result<String> result = Result.<String>error("ERROR", "错误")
                .withRequestId(requestId);
        
        assertThat(result.getRequestId()).isEqualTo(requestId);
        assertThat(result.isSuccess()).isFalse();
    }
    
    // ====================
    // 时间戳测试
    // ====================
    
    @Test
    void testTimestampAutoSet() {
        LocalDateTime before = LocalDateTime.now();
        Result<Void> result = Result.success();
        LocalDateTime after = LocalDateTime.now();
        
        assertThat(result.getTimestamp()).isNotNull();
        assertThat(result.getTimestamp()).isBetween(before, after);
    }
    
    // ====================
    // Builder测试
    // ====================
    
    @Test
    void testBuilder() {
        Result<String> result = Result.<String>builder()
                .success(true)
                .code("CUSTOM_CODE")
                .message("自定义消息")
                .data("自定义数据")
                .timestamp(LocalDateTime.now())
                .requestId("req-123")
                .build();
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo("CUSTOM_CODE");
        assertThat(result.getMessage()).isEqualTo("自定义消息");
        assertThat(result.getData()).isEqualTo("自定义数据");
        assertThat(result.getRequestId()).isEqualTo("req-123");
    }
}

