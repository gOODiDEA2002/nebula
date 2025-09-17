package io.nebula.core.common.exception;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;

/**
 * ValidationException 类单元测试
 */
class ValidationExceptionTest {
    
    @Test
    void testConstructorWithFieldErrors() {
        // Given
        List<ValidationException.FieldError> fieldErrors = List.of(
            new ValidationException.FieldError("username", "用户名不能为空", null),
            new ValidationException.FieldError("email", "邮箱格式不正确", "invalid-email")
        );
        
        // When
        ValidationException exception = new ValidationException(fieldErrors);
        
        // Then
        assertThat(exception.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(exception.getMessage()).isEqualTo("数据验证失败");
        assertThat(exception.getFieldErrors()).hasSize(2);
        assertThat(exception.hasErrors()).isTrue();
        assertThat(exception.getErrorCount()).isEqualTo(2);
    }
    
    @Test
    void testConstructorWithEmptyFieldErrors() {
        // Given
        List<ValidationException.FieldError> fieldErrors = new ArrayList<>();
        
        // When
        ValidationException exception = new ValidationException(fieldErrors);
        
        // Then
        assertThat(exception.getFieldErrors()).isEmpty();
        assertThat(exception.hasErrors()).isFalse();
        assertThat(exception.getErrorCount()).isZero();
    }
    
    @Test
    void testConstructorWithNullFieldErrors() {
        // When
        ValidationException exception = new ValidationException(null);
        
        // Then
        assertThat(exception.getFieldErrors()).isEmpty();
        assertThat(exception.hasErrors()).isFalse();
        assertThat(exception.getErrorCount()).isZero();
    }
    
    @Test
    void testConstructorWithSingleField() {
        // Given
        String field = "username";
        String message = "用户名不能为空";
        String value = null;
        
        // When
        ValidationException exception = new ValidationException(field, message, value);
        
        // Then
        assertThat(exception.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(exception.getMessage()).isEqualTo("数据验证失败");
        assertThat(exception.getFieldErrors()).hasSize(1);
        assertThat(exception.getFieldErrors().get(0).getField()).isEqualTo(field);
        assertThat(exception.getFieldErrors().get(0).getMessage()).isEqualTo(message);
        assertThat(exception.getFieldErrors().get(0).getValue()).isEqualTo(value);
    }
    
    @Test
    void testAddFieldError() {
        // Given
        ValidationException exception = new ValidationException(new ArrayList<>());
        
        // When
        ValidationException result = exception.addFieldError("name", "名称不能为空", null);
        
        // Then
        assertThat(result).isSameAs(exception); // 返回自身，支持链式调用
        assertThat(exception.getFieldErrors()).hasSize(1);
        assertThat(exception.getFieldErrors().get(0).getField()).isEqualTo("name");
        assertThat(exception.getFieldErrors().get(0).getMessage()).isEqualTo("名称不能为空");
        assertThat(exception.getFieldErrors().get(0).getValue()).isNull();
    }
    
    @Test
    void testChainedAddFieldError() {
        // Given
        ValidationException exception = new ValidationException(new ArrayList<>());
        
        // When
        exception.addFieldError("username", "用户名不能为空", null)
                .addFieldError("password", "密码长度不足", "123")
                .addFieldError("email", "邮箱格式错误", "invalid@");
        
        // Then
        assertThat(exception.getFieldErrors()).hasSize(3);
        assertThat(exception.getErrorCount()).isEqualTo(3);
        assertThat(exception.hasErrors()).isTrue();
    }
    
    @Test
    void testGetFormattedMessageWithNoErrors() {
        // Given
        ValidationException exception = new ValidationException(new ArrayList<>());
        
        // When
        String formattedMessage = exception.getFormattedMessage();
        
        // Then
        assertThat(formattedMessage).isEqualTo("数据验证失败");
    }
    
    @Test
    void testGetFormattedMessageWithSingleError() {
        // Given
        List<ValidationException.FieldError> fieldErrors = List.of(
            new ValidationException.FieldError("username", "用户名不能为空", null)
        );
        ValidationException exception = new ValidationException(fieldErrors);
        
        // When
        String formattedMessage = exception.getFormattedMessage();
        
        // Then
        assertThat(formattedMessage).isEqualTo("数据验证失败: username: 用户名不能为空");
    }
    
    @Test
    void testGetFormattedMessageWithMultipleErrors() {
        // Given
        List<ValidationException.FieldError> fieldErrors = List.of(
            new ValidationException.FieldError("username", "用户名不能为空", null),
            new ValidationException.FieldError("email", "邮箱格式不正确", "invalid-email"),
            new ValidationException.FieldError("age", "年龄必须大于0", -1)
        );
        ValidationException exception = new ValidationException(fieldErrors);
        
        // When
        String formattedMessage = exception.getFormattedMessage();
        
        // Then
        assertThat(formattedMessage).isEqualTo(
            "数据验证失败: username: 用户名不能为空, email: 邮箱格式不正确, age: 年龄必须大于0"
        );
    }
    
    @Test
    void testFieldErrorConstructorAndGetters() {
        // Given
        String field = "email";
        String message = "邮箱格式不正确";
        Object value = "invalid@email";
        
        // When
        ValidationException.FieldError fieldError = new ValidationException.FieldError(field, message, value);
        
        // Then
        assertThat(fieldError.getField()).isEqualTo(field);
        assertThat(fieldError.getMessage()).isEqualTo(message);
        assertThat(fieldError.getValue()).isEqualTo(value);
    }
    
    @Test
    void testFieldErrorToString() {
        // Given
        ValidationException.FieldError fieldError = new ValidationException.FieldError(
            "username", "用户名不能为空", null
        );
        
        // When
        String toString = fieldError.toString();
        
        // Then
        assertThat(toString).isEqualTo("FieldError{field='username', message='用户名不能为空', value='null'}");
    }
    
    @Test
    void testFieldErrorWithNonStringValue() {
        // Given
        ValidationException.FieldError fieldError = new ValidationException.FieldError(
            "age", "年龄必须大于0", -5
        );
        
        // When
        String toString = fieldError.toString();
        
        // Then
        assertThat(toString).isEqualTo("FieldError{field='age', message='年龄必须大于0', value='-5'}");
    }
    
    @Test
    void testInheritanceFromNebulaException() {
        // Given
        ValidationException exception = new ValidationException(new ArrayList<>());
        
        // Then
        assertThat(exception).isInstanceOf(NebulaException.class);
        assertThat(exception.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(exception.getArgs()).isNotNull();
    }
}
