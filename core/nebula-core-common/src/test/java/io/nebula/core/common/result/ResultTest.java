package io.nebula.core.common.result;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

/**
 * Result 类单元测试
 */
class ResultTest {
    
    @Test
    void testSuccessWithoutData() {
        // Given & When
        Result<Void> result = Result.success();
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo("SUCCESS");
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getData()).isNull();
        assertThat(result.getTimestamp()).isNotNull();
    }
    
    @Test
    void testSuccessWithData() {
        // Given
        String testData = "Hello World";
        
        // When
        Result<String> result = Result.success(testData);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo("SUCCESS");
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getData()).isEqualTo(testData);
        assertThat(result.getTimestamp()).isNotNull();
    }
    
    @Test
    void testSuccessWithDataAndMessage() {
        // Given
        String testData = "Hello World";
        String customMessage = "自定义成功消息";
        
        // When
        Result<String> result = Result.success(testData, customMessage);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo("SUCCESS");
        assertThat(result.getMessage()).isEqualTo(customMessage);
        assertThat(result.getData()).isEqualTo(testData);
    }
    
    @Test
    void testErrorWithCodeAndMessage() {
        // Given
        String errorCode = "CUSTOM_ERROR";
        String errorMessage = "自定义错误消息";
        
        // When
        Result<Void> result = Result.error(errorCode, errorMessage);
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(errorCode);
        assertThat(result.getMessage()).isEqualTo(errorMessage);
        assertThat(result.getData()).isNull();
        assertThat(result.getTimestamp()).isNotNull();
    }
    
    @Test
    void testErrorWithCodeMessageAndData() {
        // Given
        String errorCode = "VALIDATION_ERROR";
        String errorMessage = "验证失败";
        Object errorData = "详细错误信息";
        
        // When
        Result<Object> result = Result.error(errorCode, errorMessage, errorData);
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(errorCode);
        assertThat(result.getMessage()).isEqualTo(errorMessage);
        assertThat(result.getData()).isEqualTo(errorData);
    }
    
    @Test
    void testSystemError() {
        // When
        Result<Void> result = Result.systemError();
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("SYSTEM_ERROR");
        assertThat(result.getMessage()).isEqualTo("系统内部错误");
    }
    
    @Test
    void testSystemErrorWithMessage() {
        // Given
        String customMessage = "数据库连接失败";
        
        // When
        Result<Void> result = Result.systemError(customMessage);
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("SYSTEM_ERROR");
        assertThat(result.getMessage()).isEqualTo(customMessage);
    }
    
    @Test
    void testBusinessError() {
        // Given
        String businessMessage = "业务规则验证失败";
        
        // When
        Result<Void> result = Result.businessError(businessMessage);
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("BUSINESS_ERROR");
        assertThat(result.getMessage()).isEqualTo(businessMessage);
    }
    
    @Test
    void testValidationError() {
        // Given
        String validationMessage = "参数格式不正确";
        
        // When
        Result<Void> result = Result.validationError(validationMessage);
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(result.getMessage()).isEqualTo(validationMessage);
    }
    
    @Test
    void testValidationErrorWithData() {
        // Given
        String validationMessage = "参数验证失败";
        Object validationData = java.util.List.of("field1错误", "field2错误");
        
        // When
        Result<Object> result = Result.validationError(validationMessage, validationData);
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(result.getMessage()).isEqualTo(validationMessage);
        assertThat(result.getData()).isEqualTo(validationData);
    }
    
    @Test
    void testUnauthorized() {
        // When
        Result<Void> result = Result.unauthorized();
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("UNAUTHORIZED");
        assertThat(result.getMessage()).isEqualTo("未授权访问");
    }
    
    @Test
    void testUnauthorizedWithMessage() {
        // Given
        String authMessage = "Token已过期";
        
        // When
        Result<Void> result = Result.unauthorized(authMessage);
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("UNAUTHORIZED");
        assertThat(result.getMessage()).isEqualTo(authMessage);
    }
    
    @Test
    void testForbidden() {
        // When
        Result<Void> result = Result.forbidden();
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("FORBIDDEN");
        assertThat(result.getMessage()).isEqualTo("权限不足");
    }
    
    @Test
    void testForbiddenWithMessage() {
        // Given
        String forbiddenMessage = "没有足够的权限";
        
        // When
        Result<Void> result = Result.forbidden(forbiddenMessage);
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("FORBIDDEN");
        assertThat(result.getMessage()).isEqualTo(forbiddenMessage);
    }
    
    @Test
    void testNotFound() {
        // When
        Result<Void> result = Result.notFound();
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("NOT_FOUND");
        assertThat(result.getMessage()).isEqualTo("资源未找到");
    }
    
    @Test
    void testNotFoundWithMessage() {
        // Given
        String notFoundMessage = "用户不存在";
        
        // When
        Result<Void> result = Result.notFound(notFoundMessage);
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("NOT_FOUND");
        assertThat(result.getMessage()).isEqualTo(notFoundMessage);
    }
    
    @Test
    void testBuilderPattern() {
        // Given
        String testData = "Test Data";
        String testCode = "TEST_CODE";
        String testMessage = "Test Message";
        String requestId = "req-123";
        LocalDateTime timestamp = LocalDateTime.now();
        
        // When
        Result<String> result = Result.<String>builder()
                .success(true)
                .code(testCode)
                .message(testMessage)
                .data(testData)
                .requestId(requestId)
                .timestamp(timestamp)
                .build();
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo(testCode);
        assertThat(result.getMessage()).isEqualTo(testMessage);
        assertThat(result.getData()).isEqualTo(testData);
        assertThat(result.getRequestId()).isEqualTo(requestId);
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
    }
}
